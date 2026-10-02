package com.example.data.remote

import com.example.model.KnowledgeItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirestoreKnowledgeService {

    private var firestore: FirebaseFirestore? = null

    init {
        try {
            firestore = FirebaseFirestore.getInstance()
        } catch (_: Exception) {
            firestore = null
        }
    }

    suspend fun fetchAllKnowledge(): List<KnowledgeItem> = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext emptyList()
        try {
            val snapshot = fs.collection("knowledge_base").get().await()
            snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val keywords = (data["keywords"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                KnowledgeItem(
                    id = doc.id,
                    title = data["title"] as? String ?: "",
                    content = data["content"] as? String ?: "",
                    category = data["category"] as? String ?: "عام",
                    keywords = keywords,
                    sourceUrl = data["sourceUrl"] as? String ?: "",
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    author = data["author"] as? String ?: "Admin",
                    isActive = data["isActive"] as? Boolean ?: true,
                    validUntil = (data["validUntil"] as? Number)?.toLong(),
                    needsWebRefresh = data["needsWebRefresh"] as? Boolean ?: false
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun saveKnowledge(item: KnowledgeItem): Boolean = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext false
        try {
            val docRef = if (item.id.isNotBlank()) fs.collection("knowledge_base").document(item.id)
            else fs.collection("knowledge_base").document()

            val map = hashMapOf(
                "title" to item.title,
                "content" to item.content,
                "category" to item.category,
                "keywords" to item.keywords,
                "sourceUrl" to item.sourceUrl,
                "createdAt" to item.createdAt,
                "updatedAt" to item.updatedAt,
                "author" to item.author,
                "isActive" to item.isActive,
                "validUntil" to item.validUntil,
                "needsWebRefresh" to item.needsWebRefresh
            )
            docRef.set(map).await()
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun deleteKnowledge(id: String): Boolean = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext false
        try {
            fs.collection("knowledge_base").document(id).delete().await()
            true
        } catch (_: Exception) {
            false
        }
    }
}
