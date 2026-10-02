package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.GeneratedImageItem

@Entity(tableName = "generated_images")
data class GeneratedImageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val prompt: String,
    val imageUrl: String,
    val localUri: String? = null,
    val aspectRatio: String = "1:1",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): GeneratedImageItem = GeneratedImageItem(
        id = id,
        prompt = prompt,
        imageUrl = imageUrl,
        localUri = localUri,
        aspectRatio = aspectRatio,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(item: GeneratedImageItem): GeneratedImageEntity = GeneratedImageEntity(
            id = item.id,
            prompt = item.prompt,
            imageUrl = item.imageUrl,
            localUri = item.localUri,
            aspectRatio = item.aspectRatio,
            createdAt = item.createdAt
        )
    }
}
