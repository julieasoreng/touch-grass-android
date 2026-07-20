package com.julieasoreng.touchgrass.ui.lock

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.julieasoreng.touchgrass.admin.TouchGrassDeviceAdminReceiver
import com.julieasoreng.touchgrass.usage.UsageStatsHelper
import com.julieasoreng.touchgrass.ui.theme.GoalsBackground
import com.julieasoreng.touchgrass.ui.theme.GoalsMintDark
import com.julieasoreng.touchgrass.ui.theme.GoalsPurple
import com.julieasoreng.touchgrass.ui.theme.GoalsTextMuted
import com.julieasoreng.touchgrass.ui.theme.GoalsTextPrimary
import com.julieasoreng.touchgrass.ui.theme.Inter
import com.julieasoreng.touchgrass.ui.theme.Quicksand

private val dailyLimitOptions = listOf(30, 60, 90, 120)
private const val CUSTOM_LIMIT_STEP_MINUTES = 5

private fun formatLimitLabel(minutes: Int): String =
    if (minutes < 60) "${minutes}m" else "${minutes / 60}h${(minutes % 60).takeIf { it > 0 }?.let { "${it}m" } ?: ""}"

@Composable
fun DeviceAdminPermissionScreen(
    viewModel: LockFeatureViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    var hasUsageAccess by remember { mutableStateOf(UsageStatsHelper.hasUsageAccess(context)) }
    var hasNotificationPermission by remember { mutableStateOf(isNotificationPermissionGranted(context)) }
    var hasFullScreenIntentPermission by remember { mutableStateOf(isFullScreenIntentPermissionGranted(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAdminState(context)
                hasUsageAccess = UsageStatsHelper.hasUsageAccess(context)
                hasNotificationPermission = isNotificationPermissionGranted(context)
                hasFullScreenIntentPermission = isFullScreenIntentPermissionGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val deviceAdminLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshAdminState(context)
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GoalsBackground)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "‹",
                fontSize = 22.sp,
                color = GoalsTextMuted,
                modifier = Modifier.clickable(onClick = onBack)
            )
            Text(
                text = "Screen lock",
                fontFamily = Quicksand,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = GoalsTextPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("🔒", fontSize = 28.sp)
            Text(
                text = "Lock your phone when you hit your limit",
                fontFamily = Quicksand,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = GoalsTextPrimary
            )
            Text(
                text = "Touch Grass can lock your screen when you hit your limit, so willpower " +
                    "isn't the only thing standing between you and your goals. You can revoke this " +
                    "anytime in Settings.",
                fontFamily = Inter,
                fontSize = 13.5.sp,
                color = GoalsTextMuted
            )
        }

        PermissionRow(
            label = "Screen lock",
            granted = state.isDeviceAdminActive,
            actionLabel = if (state.isDeviceAdminActive) "Manage" else "Activate",
            onClick = {
                if (state.isDeviceAdminActive) {
                    context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                } else {
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(
                            DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                            TouchGrassDeviceAdminReceiver.componentName(context)
                        )
                        putExtra(
                            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            "Touch Grass locks your screen when you hit your daily screen time limit."
                        )
                    }
                    deviceAdminLauncher.launch(intent)
                }
            }
        )

        PermissionRow(
            label = "Usage access",
            granted = hasUsageAccess,
            actionLabel = "Grant access",
            onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:${context.packageName}"))
                )
            }
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionRow(
                label = "Notifications",
                granted = hasNotificationPermission,
                actionLabel = "Allow",
                onClick = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
            )
        }

        // Android 14+ won't auto-launch the post-unlock screen without this — it silently
        // degrades to a normal notification the user has to notice and tap themselves. Missing
        // this permission is the single biggest reason the lock screen can fail to show up.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            PermissionRow(
                label = "Full screen alerts",
                granted = hasFullScreenIntentPermission,
                actionLabel = "Allow",
                onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Daily limit",
                fontFamily = Quicksand,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = GoalsTextPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                dailyLimitOptions.forEach { option ->
                    val selected = state.dailyLimitMinutes == option
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (selected) GoalsPurple else Color.White)
                            .clickable { viewModel.setDailyLimitMinutes(option) }
                            .padding(vertical = 9.dp, horizontal = 16.dp)
                    ) {
                        Text(
                            text = formatLimitLabel(option),
                            fontFamily = Quicksand,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            color = if (selected) Color.White else GoalsTextPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            val isCustomActive = state.dailyLimitMinutes !in dailyLimitOptions
            var customMinutes by remember(state.dailyLimitMinutes) {
                val clamped = state.dailyLimitMinutes.coerceIn(MIN_DAILY_LIMIT_MINUTES, MAX_DAILY_LIMIT_MINUTES)
                mutableIntStateOf(Math.round(clamped / CUSTOM_LIMIT_STEP_MINUTES.toDouble()).toInt() * CUSTOM_LIMIT_STEP_MINUTES)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isCustomActive) GoalsPurple.copy(alpha = 0.08f) else GoalsBackground)
                    .border(
                        width = if (isCustomActive) 1.5.dp else 1.dp,
                        color = if (isCustomActive) GoalsPurple else GoalsPurple.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Or set your own",
                    fontFamily = Quicksand,
                    fontWeight = if (isCustomActive) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = if (isCustomActive) GoalsPurple else GoalsTextMuted
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LimitStepperButton(symbol = "–") {
                        customMinutes = (customMinutes - CUSTOM_LIMIT_STEP_MINUTES).coerceAtLeast(MIN_DAILY_LIMIT_MINUTES)
                        viewModel.setDailyLimitMinutes(customMinutes)
                    }
                    Text(
                        text = formatLimitLabel(customMinutes),
                        fontFamily = Quicksand,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = GoalsTextPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(min = 64.dp)
                    )
                    LimitStepperButton(symbol = "+") {
                        customMinutes = (customMinutes + CUSTOM_LIMIT_STEP_MINUTES).coerceAtMost(MAX_DAILY_LIMIT_MINUTES)
                        viewModel.setDailyLimitMinutes(customMinutes)
                    }
                }
            }
        }
    }
}

@Composable
private fun LimitStepperButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = symbol, fontSize = 18.sp, color = GoalsTextMuted, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PermissionRow(
    label: String,
    granted: Boolean,
    actionLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(vertical = 14.dp, horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(label, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GoalsTextPrimary)
            Text(
                text = if (granted) "Granted" else "Not granted",
                fontFamily = Inter,
                fontSize = 12.sp,
                color = if (granted) GoalsMintDark else GoalsTextMuted
            )
        }
        if (!granted || label == "Screen lock") {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(GoalsPurple)
                    .clickable(onClick = onClick)
                    .padding(vertical = 8.dp, horizontal = 14.dp)
            ) {
                Text(actionLabel, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color.White)
            }
        }
    }
}

private fun isNotificationPermissionGranted(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED
}

private fun isFullScreenIntentPermissionGranted(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
    return NotificationManagerCompat.from(context).canUseFullScreenIntent()
}
