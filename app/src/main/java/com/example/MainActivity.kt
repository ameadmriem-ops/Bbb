package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.components.AISmartBottomNav
import com.example.ui.components.AISmartTopBar
import com.example.ui.components.AppNavDestination
import com.example.ui.components.ConversationDrawerSheet
import com.example.ui.components.PaywallDialog
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ImageGeneratorScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                // Arabic RTL Layout direction by default
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AISmartMainApp(
                        viewModel = viewModel,
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme }
                    )
                }
            }
        }
    }
}

@Composable
fun AISmartMainApp(
    viewModel: MainViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val isAuthLoading by viewModel.isAuthLoading.collectAsStateWithLifecycle()
    val authErrorMessage by viewModel.authErrorMessage.collectAsStateWithLifecycle()

    // Navigation and dialog states
    var currentDestination by remember { mutableStateOf(AppNavDestination.CHAT) }
    var isDrawerOpen by remember { mutableStateOf(false) }
    var isPaywallOpen by remember { mutableStateOf(false) }

    // If user is not authenticated yet, show Auth / Welcome screen
    if (currentUser == null) {
        AuthScreen(
            isLoading = isAuthLoading,
            errorMessage = authErrorMessage,
            onLogin = { email, pass -> viewModel.login(email, pass) },
            onRegister = { email, pass, name -> viewModel.register(email, pass, name) },
            onGuestLogin = { viewModel.guestLogin() },
            onResetPassword = { email -> viewModel.resetPassword(email) }
        )
        return
    }

    // Handle back button for sub-destinations
    BackHandler(enabled = currentDestination != AppNavDestination.CHAT) {
        currentDestination = AppNavDestination.CHAT
    }

    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val currentConvId by viewModel.currentConversationId.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isChatLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()
    val generatedImages by viewModel.generatedImages.collectAsStateWithLifecycle()
    val isGeneratingImage by viewModel.isGeneratingImage.collectAsStateWithLifecycle()
    val adminStats by viewModel.adminStats.collectAsStateWithLifecycle()
    val apiLogs by viewModel.apiLogs.collectAsStateWithLifecycle()
    val dailyMessagesUsed by viewModel.dailyMessagesUsed.collectAsStateWithLifecycle()
    val dailyImagesUsed by viewModel.dailyImagesUsed.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val currentlySpeakingId by viewModel.currentlySpeakingId.collectAsStateWithLifecycle()
    val searchStatus by viewModel.searchStatus.collectAsStateWithLifecycle()
    val knowledgeList by viewModel.allKnowledge.collectAsStateWithLifecycle()

    val currentConversation = conversations.find { it.id == currentConvId }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (currentDestination != AppNavDestination.ADMIN) {
                val screenTitle = when (currentDestination) {
                    AppNavDestination.CHAT -> currentConversation?.title?.ifBlank { "AI Smart" } ?: "AI Smart"
                    AppNavDestination.TOOLS -> "أدوات AI Smart"
                    AppNavDestination.IMAGES -> "توليد الصور AI"
                    AppNavDestination.PROFILE -> "الملف الشخصي"
                    AppNavDestination.ADMIN -> "لوحة الإدارة"
                }

                AISmartTopBar(
                    title = screenTitle,
                    isPremium = isPremium,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    onOpenDrawer = if (currentDestination == AppNavDestination.CHAT) {
                        { isDrawerOpen = true }
                    } else null,
                    onNewChat = if (currentDestination == AppNavDestination.CHAT) {
                        { viewModel.createNewConversation() }
                    } else null,
                    onUpgradeClick = { isPaywallOpen = true }
                )
            }
        },
        bottomBar = {
            AISmartBottomNav(
                currentDestination = currentDestination,
                onNavigate = { currentDestination = it },
                isAdmin = currentUser?.isAdmin == true
            )
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentDestination) {
                AppNavDestination.CHAT -> {
                    ChatScreen(
                        conversation = currentConversation,
                        messages = messages,
                        isLoading = isChatLoading,
                        isPremium = isPremium,
                        isAdsEnabled = adminStats.isAdsEnabled,
                        broadcastMessage = adminStats.lastBroadcastMessage,
                        searchStatus = searchStatus,
                        onSendMessage = { prompt, uri, name, type, b64, mime, text ->
                            viewModel.sendMessage(prompt, uri, name, type, b64, mime, text)
                        },
                        onRetryLastMessage = { viewModel.retryLastMessage() },
                        onOpenDrawer = { isDrawerOpen = true },
                        onUpgradeClick = { isPaywallOpen = true },
                        onSpeak = { text, id -> viewModel.speak(text, id) },
                        onStopSpeak = { viewModel.stopSpeak() },
                        isSpeaking = isSpeaking,
                        currentlySpeakingId = currentlySpeakingId
                    )
                }

                AppNavDestination.TOOLS -> {
                    ToolsScreen(
                        onNavigateTo = { dest -> currentDestination = dest },
                        onExecuteTool = { prompt -> viewModel.executeToolPrompt(prompt) },
                        onSpeak = { text -> viewModel.speak(text) }
                    )
                }

                AppNavDestination.IMAGES -> {
                    ImageGeneratorScreen(
                        imagesList = generatedImages,
                        isGenerating = isGeneratingImage,
                        onGenerate = { prompt, ratio -> viewModel.generateImage(prompt, ratio) },
                        onShare = { item -> viewModel.shareImage(item) },
                        onDelete = { id -> viewModel.deleteImage(id) }
                    )
                }

                AppNavDestination.PROFILE -> {
                    ProfileScreen(
                        user = currentUser,
                        dailyMessagesUsed = dailyMessagesUsed,
                        dailyImagesUsed = dailyImagesUsed,
                        maxDailyMessages = adminStats.freeDailyMessageLimit,
                        maxDailyImages = adminStats.freeDailyImageLimit,
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = onToggleTheme,
                        onUpgradeClick = { isPaywallOpen = true },
                        onAdminClick = { currentDestination = AppNavDestination.ADMIN },
                        onMakeAdmin = { viewModel.makeAdmin() },
                        onLogout = { viewModel.logout() }
                    )
                }

                AppNavDestination.ADMIN -> {
                    AdminScreen(
                        stats = adminStats,
                        logs = apiLogs,
                        knowledgeList = knowledgeList,
                        onBack = { currentDestination = AppNavDestination.PROFILE },
                        onUpdateLimits = { msg, img -> viewModel.updateAdminLimits(msg, img) },
                        onToggleAds = { viewModel.toggleAds(it) },
                        onSendBroadcast = { viewModel.sendBroadcast(it) },
                        onSaveKnowledge = { viewModel.saveKnowledge(it) },
                        onDeleteKnowledge = { viewModel.deleteKnowledge(it) },
                        onRefreshKnowledgeFromWeb = { viewModel.refreshKnowledgeFromWeb(it) }
                    )
                }
            }
        }

        // Conversation history bottom sheet
        ConversationDrawerSheet(
            isOpen = isDrawerOpen,
            onDismiss = { isDrawerOpen = false },
            conversations = conversations,
            currentConversationId = currentConvId ?: -1L,
            onSelectConversation = { id -> viewModel.selectConversation(id) },
            onNewChat = { viewModel.createNewConversation() },
            onRenameConversation = { id, title -> viewModel.renameConversation(id, title) },
            onDeleteConversation = { id -> viewModel.deleteConversation(id) }
        )

        // Upgrade / Paywall dialog
        PaywallDialog(
            isOpen = isPaywallOpen,
            onDismiss = { isPaywallOpen = false },
            onUpgradeSuccess = {
                viewModel.upgradeToPremium()
                isPaywallOpen = false
            }
        )
    }
}
