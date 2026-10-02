package com.example.model

enum class ToolCategory {
    WRITING,
    CODING,
    TRANSLATION,
    ANALYSIS,
    MEDIA,
    PRODUCTIVITY
}

data class ToolItem(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val titleFr: String,
    val descAr: String,
    val descEn: String,
    val descFr: String,
    val iconName: String,
    val category: ToolCategory,
    val defaultPrompt: String,
    val inputPlaceholderAr: String,
    val inputPlaceholderEn: String,
    val inputPlaceholderFr: String,
    val isPremiumOnly: Boolean = false
)
