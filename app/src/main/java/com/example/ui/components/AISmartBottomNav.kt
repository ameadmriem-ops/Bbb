package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AiPrimaryCyan

enum class AppNavDestination(
    val route: String,
    val titleAr: String,
    val titleEn: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    CHAT("chat", "المحادثة", "Chat", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
    TOOLS("tools", "الأدوات", "Tools", Icons.Filled.Construction, Icons.Outlined.Construction),
    IMAGES("images", "الصور AI", "Images", Icons.Filled.Image, Icons.Outlined.Image),
    PROFILE("profile", "حسابي", "Profile", Icons.Filled.Person, Icons.Outlined.Person),
    ADMIN("admin", "الإدارة", "Admin", Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings)
}

@Composable
fun AISmartBottomNav(
    currentDestination: AppNavDestination,
    onNavigate: (AppNavDestination) -> Unit,
    isAdmin: Boolean = false
) {
    val destinations = if (isAdmin) {
        AppNavDestination.entries
    } else {
        AppNavDestination.entries.filter { it != AppNavDestination.ADMIN }
    }

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        destinations.forEach { dest ->
            val isSelected = currentDestination == dest
            NavigationBarItem(
                modifier = Modifier.testTag("nav_${dest.route}"),
                selected = isSelected,
                onClick = { onNavigate(dest) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = dest.titleAr
                    )
                },
                label = {
                    Text(
                        text = dest.titleAr,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AiPrimaryCyan,
                    selectedTextColor = AiPrimaryCyan,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
