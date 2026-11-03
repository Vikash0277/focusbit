package com.focusflow.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.focusflow.data.db.FocusFlowDatabase

import com.focusflow.ui.screens.habits.HabitsScreen
import com.focusflow.ui.screens.scroll.ScrollCounterScreen
import com.focusflow.ui.screens.shield.AppUsageScreenContent
import com.focusflow.ui.screens.shield.ShieldScreen
import com.focusflow.ui.screens.timer.TimerScreen
import com.focusflow.ui.screens.today.TodayScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    database: FocusFlowDatabase,
    innerPadding: PaddingValues,
    onOpenWallet: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Today.route,
        modifier = Modifier.padding(innerPadding),
        enterTransition = { fadeIn(animationSpec = tween(200)) },
        exitTransition = { fadeOut(animationSpec = tween(200)) }
    ) {
        composable(Screen.Today.route) {
            TodayScreen(database = database, onOpenWallet = onOpenWallet)
        }
        composable(Screen.Shield.route) {
            ShieldScreen(
                database = database,
                onNavigateToAppUsage = { navController.navigate(Screen.AppUsage.route) }
            )
        }
        composable(Screen.Scroll.route) {
            ScrollCounterScreen()
        }
        composable(Screen.Habits.route) {
            HabitsScreen(database = database)
        }
        composable(Screen.Timer.route) {
            TimerScreen(database = database)
        }
        composable(
            route = Screen.AppUsage.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(300)
                )
            }
        ) {
            AppUsageScreenContent(
                database = database,
                onClose = { navController.popBackStack() }
            )
        }
    }
}
