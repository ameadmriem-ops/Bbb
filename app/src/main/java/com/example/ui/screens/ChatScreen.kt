package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AttachmentType
import com.example.model.ChatMessage
import com.example.model.Conversation
import com.example.ui.components.AdBannerCard
import com.example.ui.components.ChatInputArea
import com.example.ui.components.ChatMessageItem
import com.example.ui.theme.AiPrimaryCyan
import com.example.ui.theme.AiSecondaryPurple
import com.example.util.FileAnalyzerHelper
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ChatScreen(
    conversation: Conversation?,
    messages: List<ChatMessage>,
    isLoading: Boolean,
    isPremium: Boolean,
    isAdsEnabled: Boolean,
    broadcastMessage: String,
    searchStatus: String? = null,
    onSendMessage: (prompt: String, attachmentUri: Uri?, attachmentName: String?, attachmentType: AttachmentType, base64: String?, mimeType: String?, textContent: String?) -> Unit,
    onRetryLastMessage: () -> Unit,
    onOpenDrawer: () -> Unit,
    onUpgradeClick: () -> Unit,
    onSpeak: (String, Long) -> Unit,
    onStopSpeak: () -> Unit,
    isSpeaking: Boolean,
    currentlySpeakingId: Long?
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var attachmentUri by remember { mutableStateOf<Uri?>(null) }
    var attachmentName by remember { mutableStateOf<String?>(null) }
    var attachmentType by remember { mutableStateOf(AttachmentType.NONE) }
    var attachmentBase64 by remember { mutableStateOf<String?>(null) }
    var attachmentMimeType by remember { mutableStateOf<String?>(null) }
    var attachmentExtractedText by remember { mutableStateOf<String?>(null) }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                attachmentUri = uri
                attachmentName = FileAnalyzerHelper.getFileName(context, uri)
                attachmentType = AttachmentType.IMAGE
                val base64Result = FileAnalyzerHelper.convertImageToBase64(context, uri)
                attachmentBase64 = base64Result?.first
                attachmentMimeType = base64Result?.second ?: "image/jpeg"
                attachmentExtractedText = null
            }
        }
    }

    // File / Document Picker
    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val fileName = FileAnalyzerHelper.getFileName(context, uri)
                val mimeType = context.contentResolver.getType(uri)
                val type = FileAnalyzerHelper.detectAttachmentType(fileName, mimeType)

                attachmentUri = uri
                attachmentName = fileName
                attachmentType = type

                if (type == AttachmentType.IMAGE) {
                    val base64Result = FileAnalyzerHelper.convertImageToBase64(context, uri)
                    attachmentBase64 = base64Result?.first
                    attachmentMimeType = base64Result?.second
                    attachmentExtractedText = null
                } else {
                    val text = FileAnalyzerHelper.readTextContent(context, uri)
                    attachmentExtractedText = text
                    attachmentBase64 = null
                    attachmentMimeType = mimeType
                }
            }
        }
    }

    // Speech-To-Text Recognizer
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                inputText = if (inputText.isBlank()) spokenText else "$inputText $spokenText"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Broadcast banner if present
        if (broadcastMessage.isNotBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AiPrimaryCyan.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AiPrimaryCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = broadcastMessage,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }

        // Messages or Empty Welcome State
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(AiPrimaryCyan, AiSecondaryPurple))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "أهلاً بك في AI Smart",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "مساعدك فائق الذكاء للإجابة على الأسئلة، كتابة النصوص، صياغة الأكواد، وتحليل المستندات والصور.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp, start = 20.dp, end = 20.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "أفكار مقترحة للبدء السريع:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SuggestionCard("💡 اشرح لي مفهوم الحوسبة الكمومية ببساطة") {
                        inputText = "اشرح لي مفهوم الحوسبة الكمومية ببساطة وأهم تطبيقاتها المستقبلية."
                    }

                    SuggestionCard("💻 اكتب كود تطبيق Android بلغة Kotlin") {
                        inputText = "اكتب نموذج تطبيق مصغر في Jetpack Compose لعرض قائمة مهام وتحديث حالتها."
                    }

                    SuggestionCard("✍️ صياغة بريد إلكتروني رسمي لطلب ترقية") {
                        inputText = "اكتب رسالة بريد إلكتروني رسمية واحترافية للمدير لطلب ترقية وظيفية وزيادة في الراتب."
                    }

                    SuggestionCard("🌐 ترجمة وتحليل نصوص متقدمة") {
                        inputText = "ترجم النص التالي إلى الإنجليزية والفرنسية بأسلوب أكاديمي دقيق:"
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatMessageItem(
                        message = msg,
                        isSpeaking = isSpeaking && currentlySpeakingId == msg.id,
                        onSpeakClick = { text, id -> onSpeak(text, id) },
                        onStopSpeak = onStopSpeak,
                        onRetryClick = onRetryLastMessage
                    )
                }

                if (isLoading) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AiPrimaryCyan.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = AiPrimaryCyan
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = searchStatus ?: "AI Smart يفكر ويكتب الإجابة...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Non-intrusive ad banner for Free tier
        AdBannerCard(
            isPremium = isPremium,
            isAdsEnabled = isAdsEnabled,
            onUpgradeClick = onUpgradeClick
        )

        // Chat Input Area
        ChatInputArea(
            text = inputText,
            onTextChanged = { inputText = it },
            isLoading = isLoading,
            onSend = {
                val promptToSend = inputText
                val uriToSend = attachmentUri
                val nameToSend = attachmentName
                val typeToSend = attachmentType
                val base64ToSend = attachmentBase64
                val mimeToSend = attachmentMimeType
                val textToSend = attachmentExtractedText

                inputText = ""
                attachmentUri = null
                attachmentName = null
                attachmentType = AttachmentType.NONE
                attachmentBase64 = null
                attachmentMimeType = null
                attachmentExtractedText = null

                onSendMessage(promptToSend, uriToSend, nameToSend, typeToSend, base64ToSend, mimeToSend, textToSend)
            },
            onAttachClick = {
                // Show options: Pick Image or Pick Document
                docPickerLauncher.launch("*/*")
            },
            onVoiceClick = {
                try {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث الآن مع AI Smart...")
                    }
                    speechLauncher.launch(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "التعرف على الصوت غير متاح على هذا الجهاز", Toast.LENGTH_SHORT).show()
                }
            },
            attachmentUri = attachmentUri,
            attachmentName = attachmentName,
            attachmentType = attachmentType,
            onRemoveAttachment = {
                attachmentUri = null
                attachmentName = null
                attachmentType = AttachmentType.NONE
                attachmentBase64 = null
                attachmentMimeType = null
                attachmentExtractedText = null
            }
        )
    }
}

@Composable
private fun SuggestionCard(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("suggestion_chip")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
