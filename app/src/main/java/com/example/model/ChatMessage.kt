package com.example.model

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

enum class AttachmentType {
    NONE,
    IMAGE,
    PDF,
    TEXT_FILE,
    CODE_FILE
}

data class ChatMessage(
    val id: Long = 0,
    val conversationId: Long,
    val role: MessageRole,
    val content: String,
    val attachmentPath: String? = null,
    val attachmentName: String? = null,
    val attachmentType: AttachmentType = AttachmentType.NONE,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
    val isStreaming: Boolean = false,
    val sources: List<WebSource> = emptyList(),
    val knowledgeSourceTitle: String? = null,
    val searchQueriesUsed: List<String> = emptyList()
)
