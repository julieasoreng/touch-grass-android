package com.julieasoreng.touchgrass.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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

@Composable
fun BloomNavHost(
    navController: NavHostController = rememberNavController(),
    showPostUnlock: Boolean = false,
    onPostUnlockConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val goalsViewModel: GoalsViewModel = viewModel(factory = GoalsViewModelFactory())
    val lockFeatureViewModel: LockFeatureViewModel = viewModel(
        factory = LockFeatureViewModelFactory(context.applicationContext)
    )

    LaunchedEffect(showPostUnlock) {
        if (showPostUnlock) {
            navController.navigate(NavRoutes.POST_UNLOCK) { launchSingleTop = true }
            onPostUnlockConsumed()
        }
    }

    NavHost(navController = navController, startDestination = NavRoutes.ONBOARDING) {
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
