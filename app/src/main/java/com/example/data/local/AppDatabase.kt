package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ConversationEntity::class,
        ChatMessageEntity::class,
        GeneratedImageEntity::class,
        UserProfileEntity::class,
        KnowledgeEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun generatedImageDao(): GeneratedImageDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun knowledgeDao(): KnowledgeDao
}
