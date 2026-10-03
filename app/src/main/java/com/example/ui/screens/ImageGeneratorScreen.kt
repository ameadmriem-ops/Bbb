package com.example.ui.screens

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.GeneratedImageItem
import com.example.ui.theme.AiGoldPremium
import com.example.ui.theme.AiPrimaryCyan
import com.example.ui.theme.AiSecondaryPurple
import kotlinx.coroutines.launch

@Composable
fun ImageGeneratorScreen(
    imagesList: List<GeneratedImageItem>,
    isGenerating: Boolean,
    onGenerate: (prompt: String, aspectRatio: String) -> Unit,
    onShare: (GeneratedImageItem) -> Unit,
    onDelete: (Long) -> Unit
) {
    val context = LocalContext.current
    var promptInput by remember { mutableStateOf("") }
    var selectedRatio by remember { mutableStateOf("1:1") }
    var previewImage by remember { mutableStateOf<GeneratedImageItem?>(null) }

    val ratios = listOf(
        Pair("1:1", "مربع 1:1"),
        Pair("16:9", "عريض 16:9"),
        Pair("9:16", "ستوري 9:16"),
        Pair("4:3", "كلاسيكي 4:3")
    )

    val stylePrompts = listOf(
        "مدينة مستقبلية سريالية بأضواء النيون والسايبربانك 8K",
        "لوحة زيتية كلاسيكية لواحة صحراوية عند الغروب",
        "صقر روبوتي ذكي بتفاصيل ذهبية دقيقة وواقعية سينمائية",
        "طبيعة ساحرة وشلالات متلألئة تحت ضوء القمر بألوان الخيال"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "توليد الصور بالذكاء الاصطناعي",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "اكتب وصفاً إبداعياً وسيقوم الذكاء الاصطناعي برسمه في ثوانٍ",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
        )

        // Prompt input
        OutlinedTextField(
            value = promptInput,
            onValueChange = { promptInput = it },
            placeholder = { Text("صف الصورة التي تتخيلها بكل تفاصيلها...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .testTag("image_prompt_input"),
            shape = RoundedCornerShape(16.dp),
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Preset style suggestions
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(stylePrompts) { style ->
                Surface(
                    onClick = { promptInput = style },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "✨ " + style.take(24) + "...",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Aspect ratio selector
        Text(
            text = "اختيار المقاس والأبعاد:",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ratios.forEach { (ratioKey, ratioLabel) ->
                val isSelected = selectedRatio == ratioKey
                Surface(
                    onClick = { selectedRatio = ratioKey },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, AiPrimaryCyan) else null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = ratioLabel,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Generate button
        Button(
            onClick = {
                if (promptInput.isNotBlank()) {
                    onGenerate(promptInput, selectedRatio)
                }
            },
            enabled = promptInput.isNotBlank() && !isGenerating,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("generate_image_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("جاري رسم وتوليد الصورة...")
            } else {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("إنشاء الصورة الفنية الآن", fontWeight = FontWeight.Bold)
            }
        }

        // Preview of newest image
        val currentImageToDisplay = previewImage ?: imagesList.firstOrNull()
        if (currentImageToDisplay != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(18.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column {
                    AsyncImage(
                        model = currentImageToDisplay.localUri ?: currentImageToDisplay.imageUrl,
                        contentDescription = currentImageToDisplay.prompt,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentImageToDisplay.prompt,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2
                            )
                            Text(
                                text = "الأبعاد: ${currentImageToDisplay.aspectRatio}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row {
                            IconButton(
                                onClick = { onShare(currentImageToDisplay) },
                                modifier = Modifier.size(36.dp).testTag("share_image_button")
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "مشاركة", tint = AiPrimaryCyan)
                            }

                            IconButton(
                                onClick = {
                                    Toast.makeText(context, "تم حفظ الصورة في ذاكرة التطبيق", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(36.dp).testTag("download_image_button")
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = "تنزيل", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // Past Images History
        if (imagesList.size > 1) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "معرض الصور السابقة (${imagesList.size}):",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(imagesList, key = { it.id }) { item ->
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { previewImage = item }
                            .border(
                                1.dp,
                                if (previewImage?.id == item.id) AiPrimaryCyan else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        AsyncImage(
                            model = item.localUri ?: item.imageUrl,
                            contentDescription = item.prompt,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        IconButton(
                            onClick = { onDelete(item.id) },
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.TopEnd)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}
