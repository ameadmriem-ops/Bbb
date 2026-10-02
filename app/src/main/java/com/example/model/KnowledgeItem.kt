package com.example.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KnowledgeItem(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val category: String = "عام", // أخبار، تقنية، سياسات، منتجات، تعليم، عام
    val keywords: List<String> = emptyList(),
    val sourceUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val author: String = "Admin",
    val isActive: Boolean = true,
    val validUntil: Long? = null,
    val needsWebRefresh: Boolean = false
) {
    fun isExpired(): Boolean {
        val now = System.currentTimeMillis()
        if (validUntil != null && now > validUntil) return true
        // If older than 30 days and marked as volatile
        if (needsWebRefresh && now - updatedAt > 7L * 24 * 60 * 60 * 1000) return true
        return false
    }
}
