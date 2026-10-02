package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AdminStats
import com.example.model.ApiLogEntry
import com.example.model.KnowledgeItem
import com.example.ui.theme.AiError
import com.example.ui.theme.AiGoldPremium
import com.example.ui.theme.AiPrimaryCyan
import com.example.ui.theme.AiSecondaryPurple
import com.example.ui.theme.AiSuccess
import com.example.ui.theme.AiTertiaryEmerald
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminScreen(
    stats: AdminStats,
    logs: List<ApiLogEntry>,
    knowledgeList: List<KnowledgeItem>,
    onBack: () -> Unit,
    onUpdateLimits: (msgLimit: Int, imgLimit: Int) -> Unit,
    onToggleAds: (Boolean) -> Unit,
    onSendBroadcast: (String) -> Unit,
    onSaveKnowledge: (KnowledgeItem) -> Unit,
    onDeleteKnowledge: (String) -> Unit,
    onRefreshKnowledgeFromWeb: (KnowledgeItem) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedAdminTab by remember { mutableIntStateOf(0) } // 0: Stats & Controls, 1: Knowledge Base
    var msgLimitInput by remember { mutableIntStateOf(stats.freeDailyMessageLimit) }
    var imgLimitInput by remember { mutableIntStateOf(stats.freeDailyImageLimit) }
    var broadcastText by remember { mutableStateOf("") }

    // Knowledge Base State
    var knowledgeSearch by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("الكل") }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<KnowledgeItem?>(null) }
    var itemTitleInput by remember { mutableStateOf("") }
    var itemContentInput by remember { mutableStateOf("") }
    var itemCategoryInput by remember { mutableStateOf("عام") }
    var itemKeywordsInput by remember { mutableStateOf("") }
    var itemSourceUrlInput by remember { mutableStateOf("") }
    var itemIsActive by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("admin_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = AiTertiaryEmerald,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "لوحة تحكم المسؤول (Admin)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "مراقبة الإحصائيات، أداء API، وإدارة قاعدة المعرفة",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tabs
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            TabRow(
                selectedTabIndex = selectedAdminTab,
                containerColor = Color.Transparent,
                indicator = {}
            ) {
                Tab(
                    selected = selectedAdminTab == 0,
                    onClick = { selectedAdminTab = 0 },
                    text = { Text("الإحصائيات والتحكم", fontWeight = if (selectedAdminTab == 0) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedAdminTab == 1,
                    onClick = { selectedAdminTab = 1 },
                    text = { Text("قاعدة المعرفة (${knowledgeList.size})", fontWeight = if (selectedAdminTab == 1) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedAdminTab == 0) {
            // --- TAB 0: System Health, Metrics, Controls ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, AiSuccess.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = AiSuccess,
                        modifier = Modifier.size(28.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = stats.serverStatus,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "معدل نجاح API: ${stats.apiSuccessRate}% • متوسط الاستجابة: ${stats.avgLatencyMs}ms",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "إحصائيات المنصة الحية:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard("إجمالي المستخدمين", "${stats.totalUsers}", Icons.Default.Group, AiPrimaryCyan, Modifier.weight(1f))
                StatCard("المستخدمين اليوم", "${stats.activeToday}", Icons.Default.Speed, AiTertiaryEmerald, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard("إجمالي المحادثات", "${stats.totalConversations}", Icons.Default.Message, AiSecondaryPurple, Modifier.weight(1f))
                StatCard("الرسائل المعالجة", "${stats.totalMessages}", Icons.Default.Message, AiPrimaryCyan, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard("الصور المولدة", "${stats.totalImagesGenerated}", Icons.Default.Image, AiTertiaryEmerald, Modifier.weight(1f))
                StatCard("مشتركو Premium", "${stats.premiumUsersCount}", Icons.Default.Star, AiGoldPremium, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quotas and Ad controls
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "التحكم في حدود الاستخدام والإعلانات:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("حد الرسائل اليومية للباقة المجانية:", fontSize = 13.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = {
                                    if (msgLimitInput > 5) {
                                        msgLimitInput -= 5
                                        onUpdateLimits(msgLimitInput, imgLimitInput)
                                    }
                                },
                                modifier = Modifier.size(34.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text("-")
                            }
                            Text(
                                text = "$msgLimitInput",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )
                            Button(
                                onClick = {
                                    msgLimitInput += 5
                                    onUpdateLimits(msgLimitInput, imgLimitInput)
                                },
                                modifier = Modifier.size(34.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text("+")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("حد توليد الصور اليومية للباقة المجانية:", fontSize = 13.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = {
                                    if (imgLimitInput > 1) {
                                        imgLimitInput -= 1
                                        onUpdateLimits(msgLimitInput, imgLimitInput)
                                    }
                                },
                                modifier = Modifier.size(34.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text("-")
                            }
                            Text(
                                text = "$imgLimitInput",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )
                            Button(
                                onClick = {
                                    imgLimitInput += 1
                                    onUpdateLimits(msgLimitInput, imgLimitInput)
                                },
                                modifier = Modifier.size(34.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text("+")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تفعيل الإعلانات للباقة المجانية:", fontSize = 13.sp)
                        Switch(
                            checked = stats.isAdsEnabled,
                            onCheckedChange = { onToggleAds(it) },
                            modifier = Modifier.testTag("admin_ads_switch")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Broadcast sender
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Campaign, contentDescription = null, tint = AiPrimaryCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إرسال إشعار / تنبيه عام لكافة المستخدمين:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = broadcastText,
                        onValueChange = { broadcastText = it },
                        placeholder = { Text("اكتب نص الإشعار العام هنا...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("broadcast_input_field"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (broadcastText.isNotBlank()) {
                                onSendBroadcast(broadcastText)
                                broadcastText = ""
                                Toast.makeText(context, "تم نشر التنبيه لكافة المستخدمين بنجاح", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.align(Alignment.End).testTag("send_broadcast_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("نشر التنبيه")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // API Error & Request Logs
            Text(
                text = "سجل طلبات وأخطاء API الحية:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(10.dp)
            ) {
                if (logs.isEmpty()) {
                    Text(
                        text = "لا توجد أخطاء مسجلة، جميع الطلبات تعمل بكفاءة.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    logs.take(10).forEach { log ->
                        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (log.isError) Icons.Default.Error else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (log.isError) AiError else AiSuccess,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = log.endpoint,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }

                            Text(
                                text = "${log.status} • ${log.latencyMs}ms • $timeStr",
                                fontSize = 10.sp,
                                color = if (log.isError) AiError else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            // --- TAB 1: KNOWLEDGE BASE MANAGEMENT ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "إدارة المعرفة الخاصة بالتطبيق",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Button(
                    onClick = {
                        editingItem = null
                        itemTitleInput = ""
                        itemContentInput = ""
                        itemCategoryInput = "عام"
                        itemKeywordsInput = ""
                        itemSourceUrlInput = ""
                        itemIsActive = true
                        showEditDialog = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_knowledge_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة معلومة", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = knowledgeSearch,
                onValueChange = { knowledgeSearch = it },
                placeholder = { Text("بحث في العناوين والكلمات المفتاحية...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("knowledge_search_field")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category filter chips
            val categories = listOf("الكل", "عام", "تقنية", "أخبار", "سياسات", "منتجات")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategoryFilter == cat
                    Surface(
                        onClick = { selectedCategoryFilter = cat },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clip(RoundedCornerShape(16.dp))
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Filtered Knowledge List
            val filteredKnowledge = remember(knowledgeList, knowledgeSearch, selectedCategoryFilter) {
                knowledgeList.filter { item ->
                    val matchesCat = selectedCategoryFilter == "الكل" || item.category == selectedCategoryFilter
                    val matchesQuery = knowledgeSearch.isBlank() ||
                            item.title.contains(knowledgeSearch, ignoreCase = true) ||
                            item.content.contains(knowledgeSearch, ignoreCase = true) ||
                            item.keywords.any { it.contains(knowledgeSearch, ignoreCase = true) }
                    matchesCat && matchesQuery
                }
            }

            if (filteredKnowledge.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد معلومات مطابقة. اضغط \"إضافة معلومة\" لإنشاء عنصر جديد.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    filteredKnowledge.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth().testTag("knowledge_card_${item.id}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = AiPrimaryCyan.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = item.category,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AiPrimaryCyan,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text(
                                            text = item.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Active Status
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (item.isActive) AiSuccess.copy(alpha = 0.15f) else AiError.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (item.isActive) "نشط" else "معطل",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.isActive) AiSuccess else AiError,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = item.content,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (item.keywords.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "الكلمات المفتاحية: ${item.keywords.joinToString(" • ")}",
                                        fontSize = 10.sp,
                                        color = AiSecondaryPurple
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(item.updatedAt))
                                    Text(
                                        text = "آخر تحديث: $dateStr • المشرف: ${item.author}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )

                                    Row {
                                        // One-click Web Search Refresh
                                        IconButton(
                                            onClick = {
                                                onRefreshKnowledgeFromWeb(item)
                                                Toast.makeText(context, "جارٍ تحديث المعلومة بأحدث نتائج الويب...", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(30.dp).testTag("refresh_web_button_${item.id}")
                                        ) {
                                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "تحديث من الويب", tint = AiPrimaryCyan, modifier = Modifier.size(16.dp))
                                        }

                                        // Edit
                                        IconButton(
                                            onClick = {
                                                editingItem = item
                                                itemTitleInput = item.title
                                                itemContentInput = item.content
                                                itemCategoryInput = item.category
                                                itemKeywordsInput = item.keywords.joinToString(", ")
                                                itemSourceUrlInput = item.sourceUrl
                                                itemIsActive = item.isActive
                                                showEditDialog = true
                                            },
                                            modifier = Modifier.size(30.dp).testTag("edit_knowledge_${item.id}")
                                        ) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                        }

                                        // Delete
                                        IconButton(
                                            onClick = { onDeleteKnowledge(item.id) },
                                            modifier = Modifier.size(30.dp).testTag("delete_knowledge_${item.id}")
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = AiError, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Knowledge Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(if (editingItem == null) "إضافة معلومة جديدة" else "تعديل المعلومة") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = itemTitleInput,
                        onValueChange = { itemTitleInput = it },
                        label = { Text("عنوان المعلومة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_k_title")
                    )

                    OutlinedTextField(
                        value = itemContentInput,
                        onValueChange = { itemContentInput = it },
                        label = { Text("المحتوى بالتفصيل") },
                        modifier = Modifier.fillMaxWidth().height(120.dp).testTag("input_k_content"),
                        maxLines = 6
                    )

                    OutlinedTextField(
                        value = itemCategoryInput,
                        onValueChange = { itemCategoryInput = it },
                        label = { Text("التصنيف (عام، تقنية، أخبار، سياسات...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_k_category")
                    )

                    OutlinedTextField(
                        value = itemKeywordsInput,
                        onValueChange = { itemKeywordsInput = it },
                        label = { Text("الكلمات المفتاحية (مفصولة بفواصل)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_k_keywords")
                    )

                    OutlinedTextField(
                        value = itemSourceUrlInput,
                        onValueChange = { itemSourceUrlInput = it },
                        label = { Text("رابط المصدر (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_k_source_url")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("حالة المعلومة (نشطة):", fontSize = 13.sp)
                        Switch(
                            checked = itemIsActive,
                            onCheckedChange = { itemIsActive = it },
                            modifier = Modifier.testTag("switch_k_active")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (itemTitleInput.isNotBlank() && itemContentInput.isNotBlank()) {
                            val kw = itemKeywordsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            val newItem = (editingItem ?: KnowledgeItem()).copy(
                                id = editingItem?.id ?: "k_${System.currentTimeMillis()}",
                                title = itemTitleInput.trim(),
                                content = itemContentInput.trim(),
                                category = itemCategoryInput.trim().ifBlank { "عام" },
                                keywords = kw,
                                sourceUrl = itemSourceUrlInput.trim(),
                                isActive = itemIsActive,
                                updatedAt = System.currentTimeMillis()
                            )
                            onSaveKnowledge(newItem)
                            showEditDialog = false
                            Toast.makeText(context, "تم حفظ المعلومة بنجاح", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "يرجى ملء العنوان والمحتوى", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("save_knowledge_submit_button")
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
