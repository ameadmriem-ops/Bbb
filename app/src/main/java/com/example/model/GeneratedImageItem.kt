package com.example.model

data class GeneratedImageItem(
    val id: Long = 0,
    val prompt: String,
    val imageUrl: String,
    val localUri: String? = null,
    val aspectRatio: String = "1:1",
    val createdAt: Long = System.currentTimeMillis()
)
