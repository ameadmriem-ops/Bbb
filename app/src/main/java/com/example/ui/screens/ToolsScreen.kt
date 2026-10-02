package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ToolItem
import com.example.ui.components.AppNavDestination
import com.example.ui.theme.AiGoldPremium
import com.example.ui.theme.AiPrimaryCyan
import com.example.ui.theme.AiSecondaryPurple
import com.example.ui.theme.AiTertiaryEmerald
import com.example.util.AppToolsList
import kotlinx.coroutines.launch

@Composable
fun ToolsScreen(
    onNavigateTo: (AppNavDestination) -> Unit,
    onExecuteTool: suspend (prompt: String) -> String,
    onSpeak: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTool by remember { mutableStateOf<ToolItem?>(null) }
    var toolInput by remember { mutableStateOf("") }
    var toolOutput by remember { mutableStateOf("") }
    var isExecuting by remember { mutableStateOf(false) }

    if (selectedTool != null) {
        val tool = selectedTool!!
        // Detailed Tool Execution View
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = {
                        selectedTool = null
                        toolInput = ""
                        toolOutput = ""
                    },
                    modifier = Modifier.testTag("back_from_tool_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "الرجوع",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = tool.titleAr,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = tool.descAr,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 16.dp)
            )

            OutlinedTextField(
                value = toolInput,
                onValueChange = { toolInput = it },
                label = { Text("أدخل النص أو الطلب") },
                placeholder = { Text(tool.inputPlaceholderAr) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .testTag("tool_input_field"),
                shape = RoundedCornerShape(16.dp),
                maxLines = 10
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (toolInput.isNotBlank()) {
                        coroutineScope.launch {
                            isExecuting = true
                            toolOutput = ""
                            val prompt = "${tool.defaultPrompt}\n\n$toolInput"
                            val result = onExecuteTool(prompt)
                            toolOutput = result
                            isExecuting = false
                        }
                    }
                },
                enabled = toolInput.isNotBlank() && !isExecuting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("execute_tool_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isExecuting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("جاري التنفيذ والمعالجة...")
                } else {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("بدء معالجة الذكاء الاصطناعي")
                }
            }

            // Output card
            if (toolOutput.isNotBlank() || isExecuting) {
                Spacer(modifier = Modifier.height(18.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "النتيجة الذكية:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("AI Smart Tool", toolOutput))
                                        Toast.makeText(context, "تم نسخ النتيجة", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "نسخ", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                IconButton(
                                    onClick = { onSpeak(toolOutput) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "قراءة", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = toolOutput,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    } else {
        // Main Tools List & Hub
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Text(
                text = "أدوات AI Smart المتخصصة",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "اختر الأداة المناسبة لمهامك اليومية بدقة واحترافية فائقة",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // First: AI Chat Shortcut Card
                item {
                    ToolGridCard(
                        title = "محادثة AI Chat",
                        desc = "حوار مباشر وذكي مع النموذج لجميع استفساراتك",
                        icon = Icons.Default.Chat,
                        accentColor = AiPrimaryCyan,
                        onClick = { onNavigateTo(AppNavDestination.CHAT) }
                    )
                }

                // Second: AI Image Shortcut Card
                item {
                    ToolGridCard(
                        title = "توليد الصور AI",
                        desc = "إنشاء صور فنية وتصميمات بمقاسات مختلفة",
                        icon = Icons.Default.Image,
                        accentColor = AiSecondaryPurple,
                        onClick = { onNavigateTo(AppNavDestination.IMAGES) }
                    )
                }

                items(AppToolsList.tools, key = { it.id }) { tool ->
                    val (icon, color) = when (tool.id) {
                        "ai_writer" -> Pair(Icons.Default.Edit, AiPrimaryCyan)
                        "code_assistant" -> Pair(Icons.Default.Code, AiTertiaryEmerald)
                        "translator" -> Pair(Icons.Default.Translate, AiSecondaryPurple)
                        "summarizer" -> Pair(Icons.Default.Summarize, AiGoldPremium)
                        "pdf_analyzer" -> Pair(Icons.Default.Description, AiPrimaryCyan)
                        else -> Pair(Icons.Default.RecordVoiceOver, AiTertiaryEmerald)
                    }

                    ToolGridCard(
                        title = tool.titleAr,
                        desc = tool.descAr,
                        icon = icon,
                        accentColor = color,
                        onClick = { selectedTool = tool }
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolGridCard(
    title: String,
    desc: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .testTag("tool_card_${title}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = desc,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }
        }
    }
}
