package com.julieasoreng.touchgrass.ui.navigation

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.julieasoreng.touchgrass.ui.theme.GoalsLavender
import com.julieasoreng.touchgrass.ui.theme.GoalsPurple
import com.julieasoreng.touchgrass.ui.theme.GoalsTextMuted
import com.julieasoreng.touchgrass.ui.theme.Quicksand

private data class BottomNavTab(val route: String, val label: String, val icon: String)

private val bottomNavTabs = listOf(
    BottomNavTab(NavRoutes.HOME, "My Goals", "🎯"),
    BottomNavTab(NavRoutes.WEEKLY_SUMMARY, "This week", "📅"),
    BottomNavTab(NavRoutes.LOCK_PERMISSION, "Screen Lock", "🔒")
)

/** Whether [route] is one of the top-level destinations shown in [BloomBottomBar] — the same
 *  list drives both what tab shows as selected and whether the bar renders at all, so a route
 *  can't end up in one without the other. */
fun isBottomBarRoute(route: String?): Boolean = bottomNavTabs.any { it.route == route }

@Composable
fun BloomBottomBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    NavigationBar(containerColor = Color.White) {
        bottomNavTabs.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(tab.route) },
                icon = { Text(tab.icon, fontSize = 20.sp) },
                label = {
                    Text(
                        text = tab.label,
                        fontFamily = Quicksand,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = GoalsPurple,
                    selectedTextColor = GoalsPurple,
                    unselectedIconColor = GoalsTextMuted,
                    unselectedTextColor = GoalsTextMuted,
                    indicatorColor = GoalsLavender.copy(alpha = 0.35f)
                )
            )
        }
    }
}
