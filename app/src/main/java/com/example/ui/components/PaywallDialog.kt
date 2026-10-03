package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.billing.BillingManager
import com.example.data.billing.PlayBillingProduct
import com.example.ui.theme.AiGoldPremium
import com.example.ui.theme.AiPrimaryCyan
import com.example.ui.theme.AiSecondaryPurple
import com.example.ui.theme.AiTertiaryEmerald

@Composable
fun PaywallDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    billingProducts: Map<String, PlayBillingProduct> = emptyMap(),
    isPremium: Boolean = false,
    currentCredits: Int = 0,
    subscriptionPlan: String? = null,
    isPurchasing: Boolean = false,
    onPurchaseProduct: (String) -> Unit,
    onRestorePurchases: () -> Unit,
    onManageSubscriptions: () -> Unit
) {
    if (!isOpen) return

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Subscriptions, 1: Credits
    var selectedPlanId by remember { mutableStateOf(BillingManager.PRODUCT_PRO_YEARLY) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.5.dp, Brush.linearGradient(listOf(AiGoldPremium, AiPrimaryCyan)), RoundedCornerShape(24.dp))
                .testTag("paywall_dialog_surface"),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AiGoldPremium.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = AiGoldPremium,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "متجر Google Play",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).testTag("close_paywall_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "AI Smart Pro & Credits",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "اختر الباقة المناسبة لاحتياجاتك واستمتع بأقوى إمكانيات الذكاء الاصطناعي",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // Current Plan & Credits Summary Bar
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "الخطة الحالية: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (isPremium) {
                                    if (subscriptionPlan == BillingManager.PRODUCT_PRO_YEARLY) "Pro سنوي ⭐" else "Pro شهري ⭐"
                                } else "مجانية",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPremium) AiGoldPremium else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Diamond, contentDescription = null, tint = AiPrimaryCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "$currentCredits نقطة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AiPrimaryCyan)
                        }
                    }
                }

                // Tabs: Subscriptions vs Credits
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).padding(bottom = 14.dp)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("اشتراكات Pro", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("شراء Credits 💎", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                }

                if (selectedTab == 0) {
                    // --- TAB 1: SUBSCRIPTIONS ---
                    val yearlyProduct = billingProducts[BillingManager.PRODUCT_PRO_YEARLY]
                    val monthlyProduct = billingProducts[BillingManager.PRODUCT_PRO_MONTHLY]

                    val yearlyPrice = yearlyProduct?.formattedPrice ?: "39.99$ / سنة"
                    val monthlyPrice = monthlyProduct?.formattedPrice ?: "4.99$ / شهر"

                    // Features list
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(12.dp)
                    ) {
                        PremiumFeatureRow("رسائل غير محدودة بدون حد يومي")
                        PremiumFeatureRow("توليد صور بجودة فائقة وفورية (Flux 8K)")
                        PremiumFeatureRow("تحليل وقراءة المستندات والصور الكبيرة")
                        PremiumFeatureRow("سرعة استجابة فائقة وأولوية في معالجة النماذج")
                        PremiumFeatureRow("تجربة خالية تماماً من الإعلانات")
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Plans Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Yearly Plan Option
                        SubscriptionOptionCard(
                            title = "اشتراك سنوي",
                            price = yearlyPrice,
                            subtext = "وفر 40% سنوياً",
                            badge = "الأفضل قيمة",
                            badgeColor = AiGoldPremium,
                            isSelected = selectedPlanId == BillingManager.PRODUCT_PRO_YEARLY,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedPlanId = BillingManager.PRODUCT_PRO_YEARLY }
                        )

                        // Monthly Plan Option
                        SubscriptionOptionCard(
                            title = "اشتراك شهري",
                            price = monthlyPrice,
                            subtext = "تجديد شهري مرن",
                            badge = null,
                            badgeColor = Color.Transparent,
                            isSelected = selectedPlanId == BillingManager.PRODUCT_PRO_MONTHLY,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedPlanId = BillingManager.PRODUCT_PRO_MONTHLY }
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Subscribe Button
                    Button(
                        onClick = { onPurchaseProduct(selectedPlanId) },
                        enabled = !isPurchasing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("subscribe_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .background(Brush.linearGradient(listOf(AiGoldPremium, Color(0xFFFF9E00))))
                                .clip(RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPurchasing) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isPremium) "تحديث الاشتراك عبر Google Play" else "تفعيل Pro الآن عبر Google Play",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    if (isPremium) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onManageSubscriptions, modifier = Modifier.testTag("manage_sub_button")) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = AiPrimaryCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إدارة الاشتراك في متجر Google Play", fontSize = 12.sp, color = AiPrimaryCyan)
                        }
                    }

                } else {
                    // --- TAB 2: CREDITS PACKAGES ---
                    Text(
                        text = "نقاط إضافية لتوليد الصور والمهام المتقدمة دون التزام باشتراك شهري",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    CreditPackageRow(
                        productId = BillingManager.PRODUCT_CREDITS_100,
                        credits = 100,
                        title = "باقة البداية",
                        formattedPrice = billingProducts[BillingManager.PRODUCT_CREDITS_100]?.formattedPrice ?: "0.99$",
                        isPurchasing = isPurchasing,
                        onBuy = { onPurchaseProduct(BillingManager.PRODUCT_CREDITS_100) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CreditPackageRow(
                        productId = BillingManager.PRODUCT_CREDITS_500,
                        credits = 500,
                        title = "الباقة الشائعة ⭐",
                        formattedPrice = billingProducts[BillingManager.PRODUCT_CREDITS_500]?.formattedPrice ?: "3.99$",
                        isPurchasing = isPurchasing,
                        isPopular = true,
                        onBuy = { onPurchaseProduct(BillingManager.PRODUCT_CREDITS_500) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CreditPackageRow(
                        productId = BillingManager.PRODUCT_CREDITS_1000,
                        credits = 1000,
                        title = "باقة المحترفين 👑",
                        formattedPrice = billingProducts[BillingManager.PRODUCT_CREDITS_1000]?.formattedPrice ?: "6.99$",
                        isPurchasing = isPurchasing,
                        onBuy = { onPurchaseProduct(BillingManager.PRODUCT_CREDITS_1000) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Restore Purchases Button
                OutlinedButton(
                    onClick = onRestorePurchases,
                    modifier = Modifier.fillMaxWidth().testTag("restore_purchases_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("استعادة المشتريات السابقة (Restore Purchases)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "تتم جميع المدفوعات بأمان تام عبر Google Play Billing • إلغاء في أي وقت",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SubscriptionOptionCard(
    title: String,
    price: String,
    subtext: String,
    badge: String?,
    badgeColor: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) AiGoldPremium else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) AiGoldPremium.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = badge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(18.dp))
            }

            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = price, fontWeight = FontWeight.Black, fontSize = 14.sp, color = if (isSelected) AiGoldPremium else MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtext, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CreditPackageRow(
    productId: String,
    credits: Int,
    title: String,
    formattedPrice: String,
    isPurchasing: Boolean,
    isPopular: Boolean = false,
    onBuy: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isPopular) AiPrimaryCyan.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPopular) AiPrimaryCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AiPrimaryCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Diamond, contentDescription = null, tint = AiPrimaryCyan, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "$credits نقطة رصيد", fontSize = 11.sp, color = AiPrimaryCyan, fontWeight = FontWeight.SemiBold)
                }
            }

            Button(
                onClick = onBuy,
                enabled = !isPurchasing,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPopular) AiPrimaryCyan else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (isPopular) Color.Black else MaterialTheme.colorScheme.onPrimaryContainer
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.testTag("buy_credit_${productId}")
            ) {
                Text(text = formattedPrice, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PremiumFeatureRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(AiPrimaryCyan.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = AiPrimaryCyan, modifier = Modifier.size(11.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
