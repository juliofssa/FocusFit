package com.focusfit.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import com.focusfit.app.ui.settings.NotificationSettingsScreen
import com.focusfit.app.ui.settings.TargetsScreen
import androidx.navigation.navArgument
import com.focusfit.app.ui.onboarding.OnboardingScreen
import com.focusfit.app.ui.session.SessionScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.focusfit.app.ui.habits.HabitsScreen
import com.focusfit.app.ui.plan.PlanScreen
import com.focusfit.app.ui.profile.ProfileScreen
import com.focusfit.app.ui.progress.ProgressScreen
import com.focusfit.app.ui.today.TodayScreen

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    TODAY("today", "Today", Icons.Outlined.CheckCircle),
    PLAN("plan", "Plan", Icons.Outlined.FitnessCenter),
    HABITS("habits", "Habits", Icons.Outlined.AutoStories),
    PROGRESS("progress", "Progress", Icons.Outlined.Insights),
    PROFILE("profile", "Profile", Icons.Outlined.Person),
}

private const val EDIT_PROFILE_ROUTE = "edit_profile"
private const val SESSION_ROUTE = "session/{routineId}"
private const val NOTIFICATIONS_ROUTE = "notifications"
private const val TARGETS_ROUTE = "targets"

@Composable
fun FocusFitRoot(
    pendingRoutine: String? = null,
    onRoutineOpened: () -> Unit = {},
    vm: RootViewModel = hiltViewModel(),
) {
    when (vm.onboarded.collectAsStateWithLifecycle().value) {
        null -> Unit // loading (a few ms)
        false -> Box(Modifier.fillMaxSize().systemBarsPadding()) {
            // Saving the profile flips `onboarded`, which swaps in the main app.
            OnboardingScreen(onFinished = {})
        }
        true -> MainScaffold(pendingRoutine, onRoutineOpened)
    }
}

@Composable
private fun MainScaffold(pendingRoutine: String?, onRoutineOpened: () -> Unit) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination
    val fullScreen = current?.route in setOf(EDIT_PROFILE_ROUTE, SESSION_ROUTE, NOTIFICATIONS_ROUTE, TARGETS_ROUTE)
    val startRoutine: (String) -> Unit = { id -> nav.navigate("session/$id") }

    // Opened from a core/mobility reminder → go straight to the guided session.
    LaunchedEffect(pendingRoutine) {
        if (pendingRoutine != null) {
            startRoutine(pendingRoutine)
            onRoutineOpened()
        }
    }

    // Android 13+: ask once for permission to show reminders (the system stops asking after 2 refusals).
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        bottomBar = {
            if (!fullScreen) NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = current?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            nav,
            startDestination = Tab.TODAY.route,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable(Tab.TODAY.route) { TodayScreen(onStartRoutine = startRoutine) }
            composable(Tab.PLAN.route) { PlanScreen(onStartRoutine = startRoutine) }
            composable(SESSION_ROUTE, arguments = listOf(navArgument("routineId") { type = NavType.StringType })) {
                SessionScreen(onClose = { nav.popBackStack() })
            }
            composable(Tab.HABITS.route) { HabitsScreen() }
            composable(Tab.PROGRESS.route) { ProgressScreen() }
            composable(Tab.PROFILE.route) {
                ProfileScreen(
                    onEdit = { nav.navigate(EDIT_PROFILE_ROUTE) },
                    onNotifications = { nav.navigate(NOTIFICATIONS_ROUTE) },
                    onTargets = { nav.navigate(TARGETS_ROUTE) },
                )
            }
            composable(NOTIFICATIONS_ROUTE) { NotificationSettingsScreen(onClose = { nav.popBackStack() }) }
            composable(TARGETS_ROUTE) { TargetsScreen(onClose = { nav.popBackStack() }) }
            composable(EDIT_PROFILE_ROUTE) { OnboardingScreen(onFinished = { nav.popBackStack() }) }
        }
    }
}
