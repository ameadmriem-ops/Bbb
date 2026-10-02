package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.remote.FirestoreKnowledgeService
import com.example.data.repository.AdminRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.ImageRepository
import com.example.data.repository.KnowledgeRepository
import com.example.data.repository.UsageQuotaManager
import com.example.util.VoiceManager
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AISmartApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var knowledgeRepository: KnowledgeRepository
        private set

    lateinit var chatRepository: ChatRepository
        private set

    lateinit var imageRepository: ImageRepository
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var usageQuotaManager: UsageQuotaManager
        private set

    lateinit var adminRepository: AdminRepository
        private set

    lateinit var voiceManager: VoiceManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Safe Firebase init
        try {
            FirebaseApp.initializeApp(this)
        } catch (_: Exception) {
            // Firebase initialized or config missing, safe fallback
        }

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "ai_smart_database.db"
        )
            .fallbackToDestructiveMigration()
            .build()

        val firestoreService = FirestoreKnowledgeService()
        knowledgeRepository = KnowledgeRepository(database.knowledgeDao(), firestoreService)

        usageQuotaManager = UsageQuotaManager(this)
        adminRepository = AdminRepository(this)
        authRepository = AuthRepository(this, database.userProfileDao())
        chatRepository = ChatRepository(database.conversationDao(), database.chatMessageDao(), usageQuotaManager, adminRepository, knowledgeRepository)
        imageRepository = ImageRepository(this, database.generatedImageDao(), usageQuotaManager, adminRepository)
        voiceManager = VoiceManager(this)

        // Seed default knowledge base entries in background
        CoroutineScope(Dispatchers.IO).launch {
            knowledgeRepository.initializeAndSeed()
        }
    }

    companion object {
        lateinit var instance: AISmartApp
            private set
    }
}
