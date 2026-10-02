package com.example.data.repository

import com.example.data.local.KnowledgeDao
import com.example.data.local.KnowledgeEntity
import com.example.data.remote.FirestoreKnowledgeService
import com.example.data.remote.WebSearchService
import com.example.model.KnowledgeItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class KnowledgeRepository(
    private val knowledgeDao: KnowledgeDao,
    private val firestoreService: FirestoreKnowledgeService
) {

    val allKnowledge: Flow<List<KnowledgeItem>> = knowledgeDao.getAllKnowledge().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun initializeAndSeed() = withContext(Dispatchers.IO) {
        val count = knowledgeDao.getKnowledgeCount()
        if (count == 0) {
            // Seed verified initial entries
            val initial = listOf(
                KnowledgeItem(
                    id = "k_aismart_about",
                    title = "عن تطبيق AI Smart ومميزاته الرسمية",
                    content = "AI Smart هو تطبيق ذكاء اصطناعي فائق التطور يعمل بنماذج Gemini الحديثة ومحرك توليد الصور Flux. يوفر 8 أدوات تخصصية (الكاتب الذكي، مساعد الأكواد، المترجم الفوري، الملخص السريع، محلل المستندات والصور، والمساعد الصوتي التفاعلي). يدعم البحث الحي في الويب، وحفظ المحادثات محلياً وسحابياً عبر Firebase.",
                    category = "عام",
                    keywords = listOf("ai smart", "تطبيق", "مميزات", "منصة", "ذكاء اصطناعي", "about"),
                    sourceUrl = "https://ai.google.dev",
                    author = "System Admin",
                    isActive = true
                ),
                KnowledgeItem(
                    id = "k_gemini_models",
                    title = "مواصفات ونماذج الذكاء الاصطناعي في AI Smart",
                    content = "يعتمد التطبيق على نموذج Gemini 3.5 Flash للسرعة الفائقة والاستجابة الفورية، ونموذج Gemini 3.1 Pro للمهام المعقدة والتفكير المنطقي العميق، مع دعم Google Search Grounding المباشر للربط بمصادر الإنترنت الحية.",
                    category = "تقنية",
                    keywords = listOf("gemini", "نموذج", "فلاش", "برو", "تفكير", "models"),
                    sourceUrl = "https://deepmind.google/technologies/gemini/",
                    author = "System Admin",
                    isActive = true
                ),
                KnowledgeItem(
                    id = "k_privacy_security",
                    title = "سياسة الأمان والخصوصية وحماية البيانات",
                    content = "يتم تشفير جميع اتصالات وبيانات المستخدمين وفق معايير الحماية الصارمة. لا يتم تخزين مفاتيح API في أجهزة المستخدمين، وتُحفظ المحادثات في مساحة آمنة ومعزولة لكل مستخدم في Firebase وRoom.",
                    category = "سياسات",
                    keywords = listOf("خصوصية", "أمان", "تشفير", "بيانات", "حماية", "security"),
                    sourceUrl = "https://policies.google.com/privacy",
                    author = "Compliance Officer",
                    isActive = true
                ),
                KnowledgeItem(
                    id = "k_latest_updates_2026",
                    title = "سجل التحديثات الحالية لعام 2026",
                    content = "تحديث 2026 يشمل إضافة البحث الحي في الويب المباشر مع عرض المصادر الموثوقة والروابط النشطة، ونظام قاعدة المعرفة الديناميكية المتصلة بـ Firestore، ودعم استخراج النصوص من ملفات PDF المرفقة.",
                    category = "أخبار",
                    keywords = listOf("تحديث", "2026", "جديد", "أخبار", "مستجدات", "updates"),
                    sourceUrl = "https://github.com",
                    author = "Product Manager",
                    isActive = true,
                    needsWebRefresh = true
                )
            )
            knowledgeDao.insertAll(initial.map { KnowledgeEntity.fromDomain(it) })
            // Sync to firestore if online
            initial.forEach { firestoreService.saveKnowledge(it) }
        }
    }

    /**
     * Search knowledge base for matches against user query.
     * Returns best matching KnowledgeItem and boolean if web search is still advised.
     */
    suspend fun findRelevantKnowledge(query: String): Pair<KnowledgeItem?, Boolean> = withContext(Dispatchers.IO) {
        val clean = query.trim().lowercase()
        val allActive = knowledgeDao.getActiveKnowledgeSync().map { it.toDomain() }

        var bestMatch: KnowledgeItem? = null
        var highestScore = 0

        for (item in allActive) {
            var score = 0
            if (clean.contains(item.title.lowercase())) score += 10
            for (keyword in item.keywords) {
                if (clean.contains(keyword.lowercase())) score += 5
            }
            if (item.content.lowercase().contains(clean) && clean.length > 4) score += 3

            if (score > highestScore && score >= 5) {
                highestScore = score
                bestMatch = item
            }
        }

        val needsWeb = isRealtimeQuery(clean) || (bestMatch != null && (bestMatch.isExpired() || bestMatch.needsWebRefresh))
        Pair(bestMatch, needsWeb)
    }

    suspend fun saveKnowledge(item: KnowledgeItem) = withContext(Dispatchers.IO) {
        val updated = item.copy(updatedAt = System.currentTimeMillis())
        knowledgeDao.insertKnowledge(KnowledgeEntity.fromDomain(updated))
        firestoreService.saveKnowledge(updated)
    }

    suspend fun deleteKnowledge(id: String) = withContext(Dispatchers.IO) {
        knowledgeDao.deleteKnowledgeById(id)
        firestoreService.deleteKnowledge(id)
    }

    /**
     * Refreshes a knowledge entry with latest web search info.
     */
    suspend fun refreshFromWeb(item: KnowledgeItem): KnowledgeItem = withContext(Dispatchers.IO) {
        val webResults = WebSearchService.search("${item.title} ${item.keywords.firstOrNull() ?: ""}", maxResults = 3)
        if (webResults.isNotEmpty()) {
            val freshContent = buildString {
                append(item.content)
                append("\n\n--- تحديث الويب المباشر (${webResults.first().accessDate}) ---\n")
                webResults.forEach { source ->
                    append("• ${source.title}: ${source.snippet}\n")
                }
            }
            val updated = item.copy(
                content = freshContent,
                sourceUrl = webResults.first().url,
                updatedAt = System.currentTimeMillis(),
                needsWebRefresh = false
            )
            saveKnowledge(updated)
            updated
        } else {
            val touched = item.copy(updatedAt = System.currentTimeMillis())
            saveKnowledge(touched)
            touched
        }
    }

    private fun isRealtimeQuery(query: String): Boolean {
        val keywords = listOf(
            "أخبار", "آخر", "أحدث", "اليوم", "أمس", "الآن", "سعر", "أسعار", "مباراة", "نتائج", "طقس", "ترتيب",
            "news", "latest", "price", "today", "yesterday", "current", "update", "2026", "who won", "score"
        )
        return keywords.any { query.contains(it) }
    }
}
