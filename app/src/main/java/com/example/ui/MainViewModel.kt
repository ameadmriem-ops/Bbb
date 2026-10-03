package com.example.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.AISmartApp
import com.example.data.repository.AdminRepository
import com.example.data.repository.AuthResult
import com.example.model.AdminStats
import com.example.model.ApiLogEntry
import com.example.model.AttachmentType
import com.example.model.ChatMessage
import com.example.model.Conversation
import com.example.model.GeneratedImageItem
import com.example.model.KnowledgeItem
import com.example.model.MessageRole
import com.example.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val app = AISmartApp.instance
    private val authRepo = app.authRepository
    private val chatRepo = app.chatRepository
    private val imageRepo = app.imageRepository
    private val quotaManager = app.usageQuotaManager
    private val adminRepo = app.adminRepository
    private val voiceMgr = app.voiceManager
    private val knowledgeRepo = app.knowledgeRepository
    private val billingMgr = app.billingManager

    val currentUser: StateFlow<UserProfile?> = authRepo.currentUser

    val isPremium: StateFlow<Boolean> = quotaManager.isPremium
    val credits: StateFlow<Int> = quotaManager.credits
    val subscriptionPlan: StateFlow<String?> = quotaManager.subscriptionPlan
    val dailyMessagesUsed: StateFlow<Int> = quotaManager.dailyMessagesUsed
    val dailyImagesUsed: StateFlow<Int> = quotaManager.dailyImagesUsed

    val billingProducts = billingMgr.products
    val isBillingConnected = billingMgr.isConnected
    val isPurchasing = billingMgr.isPurchasing
    val billingMessage = billingMgr.billingMessage

    val adminStats: StateFlow<AdminStats> = adminRepo.stats
    val apiLogs: StateFlow<List<ApiLogEntry>> = adminRepo.apiLogs
    val searchStatus: StateFlow<String?> = chatRepo.searchStatus

    val isSpeaking: StateFlow<Boolean> = voiceMgr.isSpeaking
    val currentlySpeakingId: StateFlow<Long?> = voiceMgr.currentlySpeakingId

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _currentConversationId = MutableStateFlow<Long?>(null)
    val currentConversationId: StateFlow<Long?> = _currentConversationId.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _generatedImages = MutableStateFlow<List<GeneratedImageItem>>(emptyList())
    val generatedImages: StateFlow<List<GeneratedImageItem>> = _generatedImages.asStateFlow()

    private val _allKnowledge = MutableStateFlow<List<KnowledgeItem>>(emptyList())
    val allKnowledge: StateFlow<List<KnowledgeItem>> = _allKnowledge.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    private val _isGeneratingImage = MutableStateFlow(false)
    val isGeneratingImage: StateFlow<Boolean> = _isGeneratingImage.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    private var lastUserPrompt: String = ""

    private var messagesJob: kotlinx.coroutines.Job? = null

    init {
        observeConversations()
        observeImages()
        observeKnowledge()
    }

    private fun observeConversations() {
        viewModelScope.launch {
            chatRepo.getConversations("all").collect { list ->
                _conversations.value = list
                if (_currentConversationId.value == null && list.isNotEmpty()) {
                    selectConversation(list.first().id)
                } else if (list.isEmpty()) {
                    val newId = chatRepo.createNewConversation("محادثة جديدة")
                    selectConversation(newId)
                }
            }
        }
    }

    private fun observeImages() {
        viewModelScope.launch {
            imageRepo.getAllImages().collect { list ->
                _generatedImages.value = list
            }
        }
    }

    private fun observeKnowledge() {
        viewModelScope.launch {
            knowledgeRepo.allKnowledge.collect { list ->
                _allKnowledge.value = list
            }
        }
    }

    fun selectConversation(id: Long) {
        _currentConversationId.value = id
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            chatRepo.getMessages(id).collect { msgs ->
                _messages.value = msgs
            }
        }
    }

    fun createNewConversation() {
        viewModelScope.launch {
            val newId = chatRepo.createNewConversation("محادثة جديدة")
            selectConversation(newId)
        }
    }

    fun renameConversation(id: Long, newTitle: String) {
        viewModelScope.launch {
            chatRepo.renameConversation(id, newTitle)
        }
    }

    fun deleteConversation(id: Long) {
        viewModelScope.launch {
            chatRepo.deleteConversation(id)
            if (_currentConversationId.value == id) {
                val remaining = _conversations.value.filter { it.id != id }
                if (remaining.isNotEmpty()) {
                    selectConversation(remaining.first().id)
                } else {
                    createNewConversation()
                }
            }
        }
    }

    fun sendMessage(
        prompt: String,
        attachmentUri: Uri?,
        attachmentName: String?,
        attachmentType: AttachmentType,
        attachmentBase64: String?,
        attachmentMimeType: String?,
        attachmentExtractedText: String?
    ) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isBlank() && attachmentUri == null) return
        lastUserPrompt = cleanPrompt

        viewModelScope.launch {
            var convId = _currentConversationId.value
            if (convId == null) {
                convId = chatRepo.createNewConversation("محادثة جديدة")
                selectConversation(convId)
            }

            // Optimistic immediate UI update so user message and loading appear instantly
            val optimisticMsg = ChatMessage(
                id = System.currentTimeMillis(),
                conversationId = convId,
                role = MessageRole.USER,
                content = cleanPrompt,
                attachmentPath = attachmentUri?.toString(),
                attachmentName = attachmentName,
                attachmentType = attachmentType,
                timestamp = System.currentTimeMillis()
            )
            _messages.value = _messages.value + optimisticMsg
            _isChatLoading.value = true

            chatRepo.sendMessage(
                conversationId = convId,
                prompt = cleanPrompt,
                attachmentPath = attachmentUri?.toString(),
                attachmentName = attachmentName,
                attachmentType = attachmentType,
                attachmentBase64 = attachmentBase64,
                attachmentMimeType = attachmentMimeType,
                extractedText = attachmentExtractedText
            )
            _isChatLoading.value = false
        }
    }

    fun retryLastMessage() {
        if (lastUserPrompt.isNotBlank()) {
            sendMessage(
                prompt = lastUserPrompt,
                attachmentUri = null,
                attachmentName = null,
                attachmentType = AttachmentType.NONE,
                attachmentBase64 = null,
                attachmentMimeType = null,
                attachmentExtractedText = null
            )
        }
    }

    fun stopGenerating() {
        _isChatLoading.value = false
    }

    suspend fun executeToolPrompt(prompt: String): String {
        val convId = _currentConversationId.value ?: 1L
        val result = chatRepo.sendMessage(
            conversationId = convId,
            prompt = prompt
        )
        return result.getOrNull()?.content ?: "تمت معالجة الطلب بنجاح."
    }

    fun generateImage(prompt: String, aspectRatio: String) {
        viewModelScope.launch {
            _isGeneratingImage.value = true
            imageRepo.generateImage(prompt, aspectRatio)
            _isGeneratingImage.value = false
        }
    }

    fun deleteImage(id: Long) {
        viewModelScope.launch {
            imageRepo.deleteImage(id)
        }
    }

    fun shareImage(item: GeneratedImageItem) {
        imageRepo.shareImage(item)
    }

    fun speak(text: String, messageId: Long? = null) {
        voiceMgr.speak(text, messageId)
    }

    fun stopSpeak() {
        voiceMgr.stop()
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            when (val result = authRepo.login(email, pass)) {
                is AuthResult.Success -> {
                    if (result.user.isPremium) {
                        quotaManager.setPremium(true)
                    }
                }
                is AuthResult.Error -> {
                    _authErrorMessage.value = result.message
                }
            }
            _isAuthLoading.value = false
        }
    }

    fun register(email: String, pass: String, name: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            when (val result = authRepo.register(email, pass, name)) {
                is AuthResult.Success -> {
                    if (result.user.isPremium) {
                        quotaManager.setPremium(true)
                    }
                }
                is AuthResult.Error -> {
                    _authErrorMessage.value = result.message
                }
            }
            _isAuthLoading.value = false
        }
    }

    fun guestLogin() {
        viewModelScope.launch {
            _isAuthLoading.value = true
            authRepo.loginAsGuest()
            _isAuthLoading.value = false
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            authRepo.sendPasswordReset(email)
        }
    }

    fun logout() {
        viewModelScope.launch {
            voiceMgr.stop()
            authRepo.logout()
            quotaManager.setPremium(false)
        }
    }

    fun upgradeToPremium() {
        quotaManager.setPremium(true)
        viewModelScope.launch {
            authRepo.updateProfile(currentUser.value?.displayName ?: "User", isPremium = true)
        }
    }

    fun makeAdmin() {
        authRepo.makeAdminForTesting()
        quotaManager.setPremium(true)
    }

    fun updateAdminLimits(msgLimit: Int, imgLimit: Int) {
        adminRepo.setFreeLimits(msgLimit, imgLimit)
    }

    fun toggleAds(enabled: Boolean) {
        adminRepo.setAdsEnabled(enabled)
    }

    fun sendBroadcast(message: String) {
        adminRepo.sendBroadcast(message)
    }

    // Knowledge Base management
    fun saveKnowledge(item: KnowledgeItem) {
        viewModelScope.launch {
            knowledgeRepo.saveKnowledge(item)
        }
    }

    fun deleteKnowledge(id: String) {
        viewModelScope.launch {
            knowledgeRepo.deleteKnowledge(id)
        }
    }

    fun refreshKnowledgeFromWeb(item: KnowledgeItem) {
        viewModelScope.launch {
            knowledgeRepo.refreshFromWeb(item)
        }
    }

    // Google Play Billing Actions
    fun purchaseProduct(activity: android.app.Activity, productId: String) {
        billingMgr.launchPurchaseFlow(activity, productId)
    }

    fun restorePurchases(onComplete: ((Boolean) -> Unit)? = null) {
        billingMgr.restorePurchases(onComplete)
    }

    fun openManageSubscriptions(activity: android.app.Activity) {
        billingMgr.openManageSubscriptions(activity)
    }

    fun clearBillingMessage() {
        billingMgr.clearMessage()
    }

    override fun onCleared() {
        super.onCleared()
        voiceMgr.shutdown()
    }
}
