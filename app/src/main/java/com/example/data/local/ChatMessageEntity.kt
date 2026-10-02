package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.AttachmentType
import com.example.model.ChatMessage
import com.example.model.MessageRole
import com.example.model.WebSource
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["conversationId"])]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long,
    val role: String, // USER, ASSISTANT, SYSTEM
    val content: String,
    val attachmentPath: String? = null,
    val attachmentName: String? = null,
    val attachmentType: String = AttachmentType.NONE.name,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
    val sourcesJson: String? = null,
    val knowledgeSourceTitle: String? = null
) {
    fun toDomain(): ChatMessage {
        val parsedSources: List<WebSource> = try {
            if (!sourcesJson.isNullOrBlank()) {
                val type = Types.newParameterizedType(List::class.java, WebSource::class.java)
                val adapter = moshi.adapter<List<WebSource>>(type)
                adapter.fromJson(sourcesJson) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }

        return ChatMessage(
            id = id,
            conversationId = conversationId,
            role = try { MessageRole.valueOf(role) } catch (_: Exception) { MessageRole.ASSISTANT },
            content = content,
            attachmentPath = attachmentPath,
            attachmentName = attachmentName,
            attachmentType = try { AttachmentType.valueOf(attachmentType) } catch (_: Exception) { AttachmentType.NONE },
            timestamp = timestamp,
            isError = isError,
            sources = parsedSources,
            knowledgeSourceTitle = knowledgeSourceTitle
        )
    }

    companion object {
        private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

        fun fromDomain(msg: ChatMessage): ChatMessageEntity {
            val sourcesStr: String? = if (msg.sources.isNotEmpty()) {
                try {
                    val type = Types.newParameterizedType(List::class.java, WebSource::class.java)
                    val adapter = moshi.adapter<List<WebSource>>(type)
                    adapter.toJson(msg.sources)
                } catch (_: Exception) {
                    null
                }
            } else null

            return ChatMessageEntity(
                id = msg.id,
                conversationId = msg.conversationId,
                role = msg.role.name,
                content = msg.content,
                attachmentPath = msg.attachmentPath,
                attachmentName = msg.attachmentName,
                attachmentType = msg.attachmentType.name,
                timestamp = msg.timestamp,
                isError = msg.isError,
                sourcesJson = sourcesStr,
                knowledgeSourceTitle = msg.knowledgeSourceTitle
            )
        }
    }
}
