package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Conversation

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastMessagePreview: String = "",
    val messageCount: Int = 0,
    val userId: String = "guest"
) {
    fun toDomain(): Conversation = Conversation(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastMessagePreview = lastMessagePreview,
        messageCount = messageCount,
        userId = userId
    )

    companion object {
        fun fromDomain(conversation: Conversation): ConversationEntity = ConversationEntity(
            id = conversation.id,
            title = conversation.title,
            createdAt = conversation.createdAt,
            updatedAt = conversation.updatedAt,
            lastMessagePreview = conversation.lastMessagePreview,
            messageCount = conversation.messageCount,
            userId = conversation.userId
        )
    }
}
