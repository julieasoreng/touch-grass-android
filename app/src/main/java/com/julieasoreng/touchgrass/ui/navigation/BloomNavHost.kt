package com.julieasoreng.touchgrass.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.julieasoreng.touchgrass.data.preferences.DeviceIdentityRepository
import com.julieasoreng.touchgrass.data.preferences.OnboardingPreferencesRepository
import com.julieasoreng.touchgrass.ui.goals.ActiveTimerScreen
import com.julieasoreng.touchgrass.ui.goals.GoalsViewModel
import com.julieasoreng.touchgrass.ui.goals.GoalsViewModelFactory
import com.julieasoreng.touchgrass.ui.goals.MyGoalsScreen
import com.julieasoreng.touchgrass.ui.goals.SetDurationScreen
import com.julieasoreng.touchgrass.ui.goals.WeeklySummaryScreen
import com.julieasoreng.touchgrass.ui.lock.DeviceAdminPermissionScreen
import com.julieasoreng.touchgrass.ui.lock.LockFeatureViewModel
import com.julieasoreng.touchgrass.ui.lock.LockFeatureViewModelFactory
import com.julieasoreng.touchgrass.ui.lock.PostUnlockScreen
import com.julieasoreng.touchgrass.ui.onboarding.OnboardingScreen
import com.julieasoreng.touchgrass.ui.theme.CreamBackground
import kotlinx.coroutines.flow.first

@Composable
fun BloomNavHost(
    navController: NavHostController = rememberNavController(),
    showPostUnlock: Boolean = false,
    onPostUnlockConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val goalsViewModel: GoalsViewModel = viewModel(
        factory = remember { GoalsViewModelFactory(context.applicationContext) }
    )
    val lockFeatureViewModel: LockFeatureViewModel = viewModel(
        factory = LockFeatureViewModelFactory(context.applicationContext)
    )
    val onboardingPreferencesRepository = remember { OnboardingPreferencesRepository(context.applicationContext) }
    val deviceIdentityRepository = remember { DeviceIdentityRepository(context.applicationContext) }

    var startDestination by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        deviceIdentityRepository.getOrCreateDeviceId()
        val completed = onboardingPreferencesRepository.isOnboardingComplete.first()
        startDestination = if (completed) NavRoutes.HOME else NavRoutes.ONBOARDING
    }

    val resolvedStartDestination = startDestination
    if (resolvedStartDestination == null) {
        // Briefly shown while local storage is checked so we don't flash the onboarding screen
        // before knowing whether it should actually be skipped.
        Box(modifier = Modifier.fillMaxSize().background(CreamBackground))
        return
    }

    // Placed after the loading gate above so the NavHost (and its graph) already exists by the
    // time this actually navigates.
    LaunchedEffect(showPostUnlock) {
        if (showPostUnlock) {
            navController.navigate(NavRoutes.POST_UNLOCK) { launchSingleTop = true }
            onPostUnlockConsumed()
        }
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (isBottomBarRoute(currentRoute)) {
                BloomBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = resolvedStartDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavRoutes.ONBOARDING) {
                OnboardingScreen(
                    onOnboardingComplete = {
                        navController.navigate(NavRoutes.HOME) {
                            popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }
            composable(NavRoutes.HOME) {
                MyGoalsScreen(
                    viewModel = goalsViewModel,
                    onGoalSelected = { goalId -> navController.navigate(NavRoutes.setDuration(goalId)) },
                    onViewSummary = { navController.navigate(NavRoutes.WEEKLY_SUMMARY) },
                    onOpenLockSettings = { navController.navigate(NavRoutes.LOCK_PERMISSION) }
                )
            }
            composable(
                route = NavRoutes.SET_DURATION,
                arguments = listOf(navArgument("goalId") { type = NavType.StringType })
            ) { backStackEntry ->
                val goalId = backStackEntry.arguments?.getString("goalId").orEmpty()
                SetDurationScreen(
                    goalId = goalId,
                    viewModel = goalsViewModel,
                    onBack = { navController.popBackStack() },
                    onStartFocusing = { minutes -> navController.navigate(NavRoutes.activeTimer(goalId, minutes)) }
                )
            }
            composable(
                route = NavRoutes.ACTIVE_TIMER,
                arguments = listOf(
                    navArgument("goalId") { type = NavType.StringType },
                    navArgument("minutes") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val goalId = backStackEntry.arguments?.getString("goalId").orEmpty()
                val minutes = backStackEntry.arguments?.getInt("minutes") ?: 0
                ActiveTimerScreen(
                    goalId = goalId,
                    minutes = minutes,
                    viewModel = goalsViewModel,
                    onSessionEnded = { navController.popBackStack(NavRoutes.HOME, inclusive = false) }
                )
            }
            composable(NavRoutes.WEEKLY_SUMMARY) {
                WeeklySummaryScreen(
                    viewModel = goalsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(NavRoutes.LOCK_PERMISSION) {
                DeviceAdminPermissionScreen(
                    viewModel = lockFeatureViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(NavRoutes.POST_UNLOCK) {
                PostUnlockScreen(
                    viewModel = lockFeatureViewModel,
                    onStartFocusSession = {
                        navController.navigate(NavRoutes.HOME) {
                            popUpTo(NavRoutes.HOME) { inclusive = true }
                        }
                    },
                    onDismiss = {
                        navController.navigate(NavRoutes.HOME) {
                            popUpTo(NavRoutes.HOME) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
