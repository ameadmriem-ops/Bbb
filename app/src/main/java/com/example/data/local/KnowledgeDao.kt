package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeDao {

    @Query("SELECT * FROM knowledge_entries ORDER BY updatedAt DESC")
    fun getAllKnowledge(): Flow<List<KnowledgeEntity>>

    @Query("SELECT * FROM knowledge_entries WHERE isActive = 1 ORDER BY updatedAt DESC")
    suspend fun getActiveKnowledgeSync(): List<KnowledgeEntity>

    @Query("SELECT * FROM knowledge_entries WHERE id = :id LIMIT 1")
    suspend fun getKnowledgeById(id: String): KnowledgeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKnowledge(item: KnowledgeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<KnowledgeEntity>)

    @Update
    suspend fun updateKnowledge(item: KnowledgeEntity)

    @Query("DELETE FROM knowledge_entries WHERE id = :id")
    suspend fun deleteKnowledgeById(id: String)

    @Query("SELECT COUNT(*) FROM knowledge_entries")
    suspend fun getKnowledgeCount(): Int
}
