package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserProfile
import com.example.ui.theme.AiError
import com.example.ui.theme.AiGoldPremium
import com.example.ui.theme.AiPrimaryCyan
import com.example.ui.theme.AiSecondaryPurple
import com.example.ui.theme.AiTertiaryEmerald

@Composable
fun ProfileScreen(
    user: UserProfile?,
    dailyMessagesUsed: Int,
    dailyImagesUsed: Int,
    maxDailyMessages: Int,
    maxDailyImages: Int,
    credits: Int = 0,
    subscriptionPlan: String? = null,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onUpgradeClick: () -> Unit,
    onRestorePurchases: () -> Unit = {},
    onManageSubscriptions: () -> Unit = {},
    onAdminClick: () -> Unit,
    onMakeAdmin: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var showAdminPasscodeDialog by remember { mutableStateOf(false) }
    var passcodeText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "الملف الشخصي والإعدادات",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // User Identity Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(AiPrimaryCyan, AiSecondaryPurple))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user?.displayName ?: "مستخدم AI Smart",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (user?.isPremium == true) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AiGoldPremium.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AiGoldPremium)
                            ) {
                                Text(
                                    text = "PRO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AiGoldPremium,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = user?.email ?: "guest@aismart.local",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Daily Usage Quotas
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "استهلاك الباقة اليومية:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Messages Quota
                val msgProgress = if (user?.isPremium == true) 0.05f else (dailyMessagesUsed.toFloat() / maxDailyMessages.toFloat()).coerceIn(0f, 1f)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("الرسائل والمحادثات", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = if (user?.isPremium == true) "غير محدود (Premium)" else "$dailyMessagesUsed / $maxDailyMessages رسالة",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (user?.isPremium == true) AiGoldPremium else MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { msgProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (user?.isPremium == true) AiGoldPremium else AiPrimaryCyan,
                    trackColor = MaterialTheme.colorScheme.surface
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Images Quota
                val imgProgress = if (user?.isPremium == true) 0.05f else (dailyImagesUsed.toFloat() / maxDailyImages.toFloat()).coerceIn(0f, 1f)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("توليد الصور الفنية", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = if (user?.isPremium == true) "غير محدود (Flux 8K)" else "$dailyImagesUsed / $maxDailyImages صور",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (user?.isPremium == true) AiGoldPremium else AiSecondaryPurple
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { imgProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (user?.isPremium == true) AiGoldPremium else AiSecondaryPurple,
                    trackColor = MaterialTheme.colorScheme.surface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google Play Billing Status & Credits Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = if (user?.isPremium == true) AiGoldPremium.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, if (user?.isPremium == true) AiGoldPremium else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (user?.isPremium == true) AiGoldPremium.copy(alpha = 0.25f) else AiPrimaryCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (user?.isPremium == true) Icons.Default.Star else Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (user?.isPremium == true) AiGoldPremium else AiPrimaryCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = if (user?.isPremium == true) {
                                    if (subscriptionPlan?.contains("yearly") == true) "اشتراك Pro سنوي نشط" else "اشتراك Pro شهري نشط"
                                } else "الخطة المجانية",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (user?.isPremium == true) "مفعل عبر Google Play • غير محدود" else "ميزات أساسية مع قيود يومية",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Credits badge
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AiPrimaryCyan.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AiPrimaryCyan.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💎 $credits نقطة",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AiPrimaryCyan
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (user?.isPremium == true) {
                        Button(
                            onClick = onManageSubscriptions,
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Text(text = "إدارة الاشتراك", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onUpgradeClick,
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AiGoldPremium, contentColor = Color.Black)
                        ) {
                            Text(text = "ترقية إلى Pro", fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Button(
                        onClick = onUpgradeClick,
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AiPrimaryCyan, contentColor = Color.Black)
                    ) {
                        Text(text = "شراء Credits 💎", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onRestorePurchases,
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Text(
                        text = "استعادة مشتريات Google Play (Restore)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Settings items
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column {
                // Theme toggle row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "الوضع الداكن (Dark Mode)", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { onToggleTheme() },
                        modifier = Modifier.testTag("theme_switch")
                    )
                }

                // Language row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "اللغة المدعومة", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(
                        text = "العربية (RTL) / English / FR",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Admin Access Section
        Surface(
            onClick = {
                if (user?.isAdmin == true) {
                    onAdminClick()
                } else {
                    showAdminPasscodeDialog = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_panel_button"),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = AiTertiaryEmerald,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "لوحة تحكم المسؤول (Admin Panel)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (user?.isAdmin == true) "مفعلة بحسابك الإداري" else "محمية بصلاحيات ورمز أمان",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Logout Button
        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("logout_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AiError.copy(alpha = 0.15f), contentColor = AiError)
        ) {
            Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("تسجيل الخروج", fontWeight = FontWeight.Bold)
        }
    }

    // Admin Passcode Dialog
    if (showAdminPasscodeDialog) {
        AlertDialog(
            onDismissRequest = { showAdminPasscodeDialog = false },
            title = { Text("التحقق من صلاحية المسؤول") },
            text = {
                Column {
                    Text("أدخل رمز أمان المسؤول (الرمز الافتراضي: 7788) أو سجل الدخول بحساب المسؤول:")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = passcodeText,
                        onValueChange = { passcodeText = it },
                        label = { Text("رمز المسؤول PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_pin_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (passcodeText == "7788") {
                            onMakeAdmin()
                            showAdminPasscodeDialog = false
                            onAdminClick()
                            Toast.makeText(context, "تم التحقق ومنح صلاحيات المسؤول بنجاح", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "رمز الأمان غير صحيح", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("confirm_admin_pin_button")
                ) {
                    Text("دخول")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdminPasscodeDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
