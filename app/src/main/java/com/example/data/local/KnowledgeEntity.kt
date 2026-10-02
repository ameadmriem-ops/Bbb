package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.KnowledgeItem

@Entity(tableName = "knowledge_entries")
data class KnowledgeEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val content: String,
    val category: String,
    val keywordsCsv: String,
    val sourceUrl: String,
    val createdAt: Long,
    val updatedAt: Long,
    val author: String,
    val isActive: Boolean,
    val validUntil: Long?,
    val needsWebRefresh: Boolean
) {
    fun toDomain(): KnowledgeItem = KnowledgeItem(
        id = id,
        title = title,
        content = content,
        category = category,
        keywords = if (keywordsCsv.isBlank()) emptyList() else keywordsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
        sourceUrl = sourceUrl,
        createdAt = createdAt,
        updatedAt = updatedAt,
        author = author,
        isActive = isActive,
        validUntil = validUntil,
        needsWebRefresh = needsWebRefresh
    )

    companion object {
        fun fromDomain(item: KnowledgeItem): KnowledgeEntity = KnowledgeEntity(
            id = item.id.ifBlank { "k_${System.currentTimeMillis()}" },
            title = item.title,
            content = item.content,
            category = item.category,
            keywordsCsv = item.keywords.joinToString(","),
            sourceUrl = item.sourceUrl,
            createdAt = item.createdAt,
            updatedAt = item.updatedAt,
            author = item.author,
            isActive = item.isActive,
            validUntil = item.validUntil,
            needsWebRefresh = item.needsWebRefresh
        )
    }
}
