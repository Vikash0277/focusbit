package com.focusflow.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.focusflow.data.db.FocusFlowDatabase
import com.focusflow.data.db.entity.Goal
import com.focusflow.data.db.entity.Todo
import com.focusflow.ui.screens.goals.SetGoalScreen
import com.focusflow.ui.screens.habits.HabitsScreen
import com.focusflow.ui.screens.notes.AddNoteScreen
import com.focusflow.ui.screens.notes.NoteDetailScreen
import com.focusflow.ui.screens.notifications.NotificationsScreen
import com.focusflow.ui.screens.scroll.ScrollCounterScreen
import com.focusflow.ui.screens.settings.AppearanceScreen
import com.focusflow.ui.screens.shield.AppUsageScreenContent
import com.focusflow.ui.screens.shield.ShieldScreen
import com.focusflow.ui.screens.tasks.AddTodoScreen
import com.focusflow.ui.screens.tasks.TodoDetailScreen
import com.focusflow.ui.screens.timer.TimerScreen
import com.focusflow.ui.screens.today.TodayScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    database: FocusFlowDatabase,
    innerPadding: PaddingValues,
    allGoals: List<Goal> = emptyList(),
    allTodos: List<Todo> = emptyList(),
    onOpenWallet: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Today.route,
        modifier = Modifier.padding(innerPadding),
        enterTransition = { fadeIn(animationSpec = tween(260)) },
        exitTransition = { fadeOut(animationSpec = tween(200)) }
    ) {
        composable(Screen.Today.route) {
            TodayScreen(
                database = database,
                onOpenWallet = onOpenWallet,
                onNavigateToAddTodo = { navController.navigate(Screen.AddTodo.route) },
                onNavigateToTodoDetail = { todoId -> navController.navigate(Screen.TodoDetail.createRoute(todoId)) },
                onNavigateToAddNote = { navController.navigate(Screen.AddNote.route) },
                onNavigateToNoteDetail = { noteId -> navController.navigate(Screen.NoteDetail.createRoute(noteId)) },
                onNavigateToSetGoal = { navController.navigate(Screen.SetGoal.route) }
            )
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

        // ==========================================
        // STACKED SCREEN: APP USAGE
        // ==========================================
        composable(
            route = Screen.AppUsage.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(280)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(280)
                )
            }
        ) {
            AppUsageScreenContent(
                database = database,
                onClose = { navController.popBackStack() }
            )
        }

        // ==========================================
        // STACKED SCREEN: APPEARANCE & THEMES
        // ==========================================
        composable(
            route = Screen.Appearance.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(280)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(280)
                )
            }
        ) {
            AppearanceScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ==========================================
        // STACKED SCREEN: SET / EDIT GOAL
        // ==========================================
        composable(
            route = Screen.SetGoal.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(280)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(280)
                )
            }
        ) {
            SetGoalScreen(
                database = database,
                onBack = { navController.popBackStack() }
            )
        }

        // ==========================================
        // STACKED SCREEN: NOTIFICATIONS & REMINDERS
        // ==========================================
        composable(
            route = Screen.Notifications.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(280)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(280)
                )
            }
        ) {
            NotificationsScreen(
                goals = allGoals,
                todos = allTodos,
                onBack = { navController.popBackStack() },
                onOpenTodo = { todoId ->
                    navController.navigate(Screen.TodoDetail.createRoute(todoId))
                }
            )
        }

        // ==========================================
        // STACKED SCREEN: ADD TASK / TODO
        // ==========================================
        composable(
            route = Screen.AddTodo.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(280)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(280)
                )
            }
        ) {
            AddTodoScreen(
                database = database,
                onBack = { navController.popBackStack() }
            )
        }

        // ==========================================
        // STACKED SCREEN: TODO DETAILS & EDIT
        // ==========================================
        composable(
            route = Screen.TodoDetail.route,
            arguments = listOf(navArgument("todoId") { type = NavType.LongType }),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(280)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(280)
                )
            }
        ) { backStackEntry ->
            val todoId = backStackEntry.arguments?.getLong("todoId") ?: 0L
            TodoDetailScreen(
                todoId = todoId,
                database = database,
                onBack = { navController.popBackStack() }
            )
        }

        // ==========================================
        // STACKED SCREEN: ADD NOTE
        // ==========================================
        composable(
            route = Screen.AddNote.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(280)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(280)
                )
            }
        ) {
            AddNoteScreen(
                database = database,
                onBack = { navController.popBackStack() }
            )
        }

        // ==========================================
        // STACKED SCREEN: NOTE DETAILS & EDIT
        // ==========================================
        composable(
            route = Screen.NoteDetail.route,
            arguments = listOf(navArgument("noteId") { type = NavType.LongType }),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(280)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(280)
                )
            }
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
            NoteDetailScreen(
                noteId = noteId,
                database = database,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
