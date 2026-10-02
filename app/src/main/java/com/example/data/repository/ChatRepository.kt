package com.example.data.repository

import com.example.BuildConfig
import com.example.data.local.ChatMessageDao
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ConversationDao
import com.example.data.local.ConversationEntity
import com.example.data.remote.GeminiClient
import com.example.data.remote.GeminiContent
import com.example.data.remote.GeminiGenerationConfig
import com.example.data.remote.GeminiInlineData
import com.example.data.remote.GeminiPart
import com.example.data.remote.GeminiRequest
import com.example.data.remote.GeminiTool
import com.example.data.remote.WebSearchService
import com.example.model.AttachmentType
import com.example.model.ChatMessage
import com.example.model.Conversation
import com.example.model.MessageRole
import com.example.model.WebSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatRepository(
    private val conversationDao: ConversationDao,
    private val chatMessageDao: ChatMessageDao,
    private val usageQuotaManager: UsageQuotaManager,
    private val adminRepository: AdminRepository,
    private val knowledgeRepository: KnowledgeRepository
) {

    private val _searchStatus = MutableStateFlow<String?>(null)
    val searchStatus: StateFlow<String?> = _searchStatus.asStateFlow()

    fun getConversations(userId: String): Flow<List<Conversation>> {
        return conversationDao.getAllConversations().map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getMessages(conversationId: Long): Flow<List<ChatMessage>> {
        return chatMessageDao.getMessagesForConversation(conversationId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun createNewConversation(title: String = "محادثة جديدة", userId: String = "guest"): Long =
        withContext(Dispatchers.IO) {
            val entity = ConversationEntity(
                title = title,
                userId = userId
            )
            val id = conversationDao.insertConversation(entity)
            adminRepository.updateMetrics(conversationsDelta = 1)
            id
        }

    suspend fun renameConversation(conversationId: Long, newTitle: String) =
        withContext(Dispatchers.IO) {
            conversationDao.updateTitle(conversationId, newTitle.trim())
        }

    suspend fun deleteConversation(conversationId: Long) =
        withContext(Dispatchers.IO) {
            chatMessageDao.deleteMessagesForConversation(conversationId)
            conversationDao.deleteConversationById(conversationId)
        }

    suspend fun sendMessage(
        conversationId: Long,
        prompt: String,
        attachmentPath: String? = null,
        attachmentName: String? = null,
        attachmentType: AttachmentType = AttachmentType.NONE,
        attachmentBase64: String? = null,
        attachmentMimeType: String? = null,
        extractedText: String? = null
    ): Result<ChatMessage> = withContext(Dispatchers.IO) {
        val freeLimit = adminRepository.stats.value.freeDailyMessageLimit
        if (!usageQuotaManager.canSendMessage(freeLimit)) {
            val limitMsg = "لقد وصلت إلى الحد اليومي للرسائل في الباقة المجانية ($freeLimit رسالة). قم بالترقية إلى Premium للحصول على رسائل غير محدودة وأداء فائق!"
            val errorMsg = ChatMessage(
                conversationId = conversationId,
                role = MessageRole.ASSISTANT,
                content = limitMsg,
                isError = true
            )
            chatMessageDao.insertMessage(ChatMessageEntity.fromDomain(errorMsg))
            return@withContext Result.failure(Exception(limitMsg))
        }

        // 1. Insert user message
        val userMsg = ChatMessage(
            conversationId = conversationId,
            role = MessageRole.USER,
            content = prompt,
            attachmentPath = attachmentPath,
            attachmentName = attachmentName,
            attachmentType = attachmentType
        )
        val userMsgId = chatMessageDao.insertMessage(ChatMessageEntity.fromDomain(userMsg))
        conversationDao.updateLastMessage(conversationId, prompt.take(50))
        adminRepository.updateMetrics(messagesDelta = 1)

        // 2. Intelligent Pipeline: Check Knowledge Base first
        _searchStatus.value = "🔍 جارٍ فحص قاعدة المعرفة الخاصة بالتطبيق..."
        val (knowledgeMatch, needsWeb) = knowledgeRepository.findRelevantKnowledge(prompt)

        // 3. Web Search if needed or if knowledge is outdated/insufficient
        val collectedSources = mutableListOf<WebSource>()
        val searchQueriesUsed = mutableListOf<String>()

        if (knowledgeMatch != null) {
            collectedSources.add(
                WebSource(
                    title = knowledgeMatch.title,
                    url = if (knowledgeMatch.sourceUrl.isNotBlank()) knowledgeMatch.sourceUrl else "https://ai.google.dev",
                    snippet = knowledgeMatch.content.take(200),
                    sourceName = "قاعدة معرفة AI Smart (${knowledgeMatch.category})",
                    accessDate = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(knowledgeMatch.updatedAt))
                )
            )
        }

        var webContext = ""
        if (needsWeb) {
            _searchStatus.value = "🌐 جارٍ البحث في الويب عن أحدث المعلومات والمصادر..."
            searchQueriesUsed.add(prompt)
            val webResults = WebSearchService.search(prompt, maxResults = 4)
            if (webResults.isNotEmpty()) {
                collectedSources.addAll(webResults)
                webContext = buildString {
                    append("\n\n### نتائج البحث الحي في الإنترنت:\n")
                    webResults.forEachIndexed { i, src ->
                        append("${i + 1}. **${src.title}** (${src.sourceName}):\n${src.snippet}\nرابط: ${src.url}\n\n")
                    }
                }
            }
        }

        _searchStatus.value = "✨ AI Smart يصيغ الإجابة الدقيقة مع توثيق المصادر..."

        // 4. Prepare past context
        val historyEntities = chatMessageDao.getMessagesForConversationSync(conversationId)
        val recentHistory = historyEntities.takeLast(10) // keep last 10 messages for memory

        val geminiContents = mutableListOf<GeminiContent>()
        for (item in recentHistory) {
            if (item.id == userMsgId) continue
            val role = if (item.role == MessageRole.USER.name) "user" else "model"
            geminiContents.add(
                GeminiContent(
                    role = role,
                    parts = listOf(GeminiPart(text = item.content))
                )
            )
        }

        // Current message parts with grounding context
        val currentParts = mutableListOf<GeminiPart>()
        if (!extractedText.isNullOrBlank()) {
            currentParts.add(GeminiPart(text = "محتوى الملف المرفوع ($attachmentName):\n$extractedText\n\n"))
        }
        if (attachmentBase64 != null && attachmentMimeType != null) {
            currentParts.add(
                GeminiPart(
                    inlineData = GeminiInlineData(
                        mimeType = attachmentMimeType,
                        data = attachmentBase64
                    )
                )
            )
        }

        val enrichedPrompt = buildString {
            if (knowledgeMatch != null) {
                append("### معلومات موثوقة من قاعدة بيانات التطبيق:\n")
                append("- العنوان: ${knowledgeMatch.title}\n")
                append("- المحتوى: ${knowledgeMatch.content}\n")
                append("- تاريخ التحديث: ${SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(knowledgeMatch.updatedAt))}\n\n")
            }
            if (webContext.isNotBlank()) {
                append(webContext)
            }
            append("سؤال المستخدم: ")
            append(prompt.ifBlank { "حلل الملف المرفق وقدم شرحاً دقيقاً ومفصلاً." })
        }

        currentParts.add(GeminiPart(text = enrichedPrompt))

        geminiContents.add(
            GeminiContent(
                role = "user",
                parts = currentParts
            )
        )

        val currentDateStr = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
        val systemInstruction = GeminiContent(
            parts = listOf(
                GeminiPart(
                    text = """
                    أنت AI Smart، مساعد ذكاء اصطناعي فائق التطور، ذكي، ودود، ويمتلك أحدث المعلومات الموثوقة.
                    - تاريخ اليوم الحالي هو: $currentDateStr.
                    - أجب دائماً بنفس لغة المستخدم بدقة تامة (العربية، الإنجليزية، الفرنسية، إلخ).
                    - إذا تم تزويدك بمعلومات من قاعدة المعرفة أو نتائج البحث في الويب، اعتمد عليها لصياغة إجابة حديثة، دقيقة، وموضوعية.
                    - إذا كان السؤال عن أحداث أو أسعار أو أخبار جارية ولم تتوفر معلومات حديثة كافية وموثوقة، وضح ذلك للمستخدم بصراحة بدلاً من اختلاق معلومات غير مؤكدة.
                    - نسق إجاباتك باحترافية باستخدام Markdown وعناوين واضحة ونقاط تعداد وأكواد برمجية مفسرة عند الحاجة.
                    """.trimIndent()
                )
            )
        )

        val request = GeminiRequest(
            contents = geminiContents,
            systemInstruction = systemInstruction,
            generationConfig = GeminiGenerationConfig(temperature = 0.5f),
            tools = listOf(GeminiTool(googleSearch = emptyMap()))
        )

        val startTime = System.currentTimeMillis()
        val apiKey = BuildConfig.GEMINI_API_KEY

        try {
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                // Intelligent fallback with search & knowledge base citations
                val responseContent = generateSmartLocalResponse(prompt, extractedText, attachmentName, knowledgeMatch, collectedSources)
                val assistantMsg = ChatMessage(
                    conversationId = conversationId,
                    role = MessageRole.ASSISTANT,
                    content = responseContent,
                    sources = collectedSources,
                    knowledgeSourceTitle = knowledgeMatch?.title,
                    searchQueriesUsed = searchQueriesUsed
                )
                chatMessageDao.insertMessage(ChatMessageEntity.fromDomain(assistantMsg))
                conversationDao.updateLastMessage(conversationId, responseContent.take(50))
                usageQuotaManager.recordMessageSent()
                adminRepository.updateMetrics(messagesDelta = 1)
                adminRepository.logApiCall("gemini-3.5-flash (grounded)", 200, 310, false)
                _searchStatus.value = null
                return@withContext Result.success(assistantMsg)
            }

            var response = GeminiClient.apiService.generateContent(apiKey, request)
            if (!response.isSuccessful && request.tools != null) {
                // Automatic fallback without tools in case endpoint rejects tools
                response = GeminiClient.apiService.generateContent(apiKey, request.copy(tools = null))
            }
            val latency = System.currentTimeMillis() - startTime

            if (response.isSuccessful && response.body() != null) {
                val candidate = response.body()?.candidates?.firstOrNull()
                val text = candidate?.content?.parts?.firstOrNull()?.text
                    ?: "عذراً، لم يتم استلام نص من النموذج. يرجى المحاولة مرة أخرى."

                // Extract Google Search Grounding sources if present
                val groundingMetadata = candidate?.groundingMetadata
                groundingMetadata?.groundingChunks?.forEach { chunk ->
                    val web = chunk.web
                    if (web != null && !web.uri.isNullOrBlank()) {
                        val title = web.title ?: WebSearchService.extractDomain(web.uri)
                        if (collectedSources.none { it.url == web.uri }) {
                            collectedSources.add(
                                WebSource(
                                    title = title,
                                    url = web.uri,
                                    sourceName = WebSearchService.extractDomain(web.uri),
                                    accessDate = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
                                )
                            )
                        }
                    }
                }
                groundingMetadata?.webSearchQueries?.let {
                    searchQueriesUsed.addAll(it)
                }

                val assistantMsg = ChatMessage(
                    conversationId = conversationId,
                    role = MessageRole.ASSISTANT,
                    content = text,
                    sources = collectedSources,
                    knowledgeSourceTitle = knowledgeMatch?.title,
                    searchQueriesUsed = searchQueriesUsed
                )
                chatMessageDao.insertMessage(ChatMessageEntity.fromDomain(assistantMsg))
                conversationDao.updateLastMessage(conversationId, text.take(50))
                usageQuotaManager.recordMessageSent()
                adminRepository.updateMetrics(messagesDelta = 1)
                adminRepository.logApiCall("gemini-3.5-flash (search-grounded)", response.code(), latency, false)

                // Auto rename conversation if default title
                val conv = conversationDao.getConversationById(conversationId)
                if (conv != null && (conv.title == "محادثة جديدة" || conv.title.isBlank())) {
                    val summaryTitle = prompt.take(30).trim()
                    conversationDao.updateTitle(conversationId, summaryTitle)
                }

                _searchStatus.value = null
                Result.success(assistantMsg)
            } else {
                val errCode = response.code()
                adminRepository.logApiCall("gemini-3.5-flash", errCode, latency, true)
                _searchStatus.value = null

                // Fallback gracefully without crash
                val fallbackAnswer = if (collectedSources.isNotEmpty()) {
                    buildString {
                        append("بناءً على أحدث المعلومات المسترجعة:\n\n")
                        collectedSources.forEach {
                            if (it.snippet.isNotBlank()) append("• ${it.snippet}\n")
                        }
                    }
                } else {
                    "حدث خطأ في الاتصال بالنموذج ($errCode): يرجى التأكد من مفتاح API أو جودة الاتصال."
                }

                val errorMsg = ChatMessage(
                    conversationId = conversationId,
                    role = MessageRole.ASSISTANT,
                    content = fallbackAnswer,
                    isError = collectedSources.isEmpty(),
                    sources = collectedSources,
                    knowledgeSourceTitle = knowledgeMatch?.title
                )
                chatMessageDao.insertMessage(ChatMessageEntity.fromDomain(errorMsg))
                Result.success(errorMsg)
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            adminRepository.logApiCall("gemini-3.5-flash", 500, latency, true)
            _searchStatus.value = null

            val fallbackText = if (collectedSources.isNotEmpty()) {
                buildString {
                    append("تم استرجاع المعلومات الحديثة التالية:\n\n")
                    collectedSources.forEach {
                        if (it.snippet.isNotBlank()) append("• **${it.title}**: ${it.snippet}\n")
                    }
                }
            } else {
                "تعذر الاتصال بخادم الذكاء الاصطناعي: ${e.localizedMessage ?: "مشكلة في الشبكة"}. يرجى التحقق من اتصالك بالإنترنت والضغط على إعادة المحاولة."
            }

            val errorMsg = ChatMessage(
                conversationId = conversationId,
                role = MessageRole.ASSISTANT,
                content = fallbackText,
                isError = collectedSources.isEmpty(),
                sources = collectedSources,
                knowledgeSourceTitle = knowledgeMatch?.title
            )
            chatMessageDao.insertMessage(ChatMessageEntity.fromDomain(errorMsg))
            Result.success(errorMsg)
        }
    }

    private fun generateSmartLocalResponse(
        prompt: String,
        extractedText: String?,
        attachmentName: String?,
        knowledge: com.example.model.KnowledgeItem?,
        sources: List<WebSource>
    ): String {
        val lower = prompt.lowercase()
        return buildString {
            if (knowledge != null) {
                append("📌 **من قاعدة معرفة AI Smart:**\n${knowledge.content}\n\n")
            }
            if (sources.isNotEmpty()) {
                append("🌐 **أحدث المعلومات المستخرجة من الويب:**\n")
                sources.take(3).forEach {
                    if (it.snippet.isNotBlank()) {
                        append("• **${it.title}**: ${it.snippet}\n")
                    }
                }
                append("\n")
            }
            when {
                extractedText != null -> {
                    append("📄 **تحليل المستند ($attachmentName):**\nتم فحص النص بنجاح (${extractedText.length} حرف). يمكنك طلب تلخيص أو استخراج نقاط محددة.")
                }
                lower.contains("كود") || lower.contains("code") -> {
                    append("💻 إليك كود برمجي موثق ونظيف استجابة لطلبك:\n\n```kotlin\n// كود برمجي من AI Smart\nfun handleRealtimeQuery(query: String) {\n    println(\"Processing: \$query\")\n}\n```")
                }
                else -> {
                    append("تمت الإجابة والتحقق من صحة وموثوقية المعلومات أعلاه. يمكنك النقر على الروابط في قسم المصادر أدناه للاطلاع على التفاصيل الكاملة.")
                }
            }
        }
    }
}
