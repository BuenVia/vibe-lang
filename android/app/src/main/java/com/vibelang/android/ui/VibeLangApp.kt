package com.vibelang.android.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.vibelang.android.ui.admin.AdminScreen
import com.vibelang.android.ui.admin.AdminTab
import com.vibelang.android.ui.categories.CategoriesScreen
import com.vibelang.android.ui.study.StudyScreen
import com.vibelang.android.ui.path.PathScreen
import com.vibelang.android.ui.practice.PracticeScreen
import com.vibelang.android.ui.settings.SettingsScreen
import com.vibelang.android.ui.subject.SubjectScreen
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

// Destinations

@Serializable data object PathRoute

@Serializable data object CategoriesRoute

@Serializable data class AdminRoute(val tab: AdminTab = AdminTab.SUBJECTS)

@Serializable data object SettingsRoute

@Serializable data class SubjectRoute(val id: Long)

@Serializable data class StudyRoute(val subjectId: Long)

@Serializable data class PracticeRoute(val subjectId: Long)

private data class TopLevel(val label: String, val icon: ImageVector, val route: Any, val routeClass: KClass<*>)

@Composable
fun VibeLangApp() {
    val appVm: AppViewModel = appViewModel { AppViewModel(it.api, it.settings) }
    val settings by appVm.settings.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    // Wait for stored settings so the app doesn't flash learner mode before switching to admin.
    val s = settings ?: return Box(Modifier.fillMaxSize())

    val tabs = buildList {
        add(TopLevel("Path", Icons.Filled.Home, PathRoute, PathRoute::class))
        add(TopLevel("Categories", Icons.AutoMirrored.Filled.List, CategoriesRoute, CategoriesRoute::class))
        if (s.isAdmin) add(TopLevel("Admin", Icons.Filled.Build, AdminRoute(), AdminRoute::class))
        add(TopLevel("Settings", Icons.Filled.Settings, SettingsRoute, SettingsRoute::class))
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = tabs.any { destination?.hasRoute(it.routeClass) == true }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = destination?.hasRoute(tab.routeClass) == true,
                            onClick = { navController.navigateToTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = PathRoute,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable<PathRoute> {
                PathScreen(
                    appVm = appVm,
                    onOpenSubject = { navController.navigate(SubjectRoute(it)) },
                    onEditOrder = { navController.navigateToTab(AdminRoute(AdminTab.SUBJECTS), restore = false) },
                )
            }
            composable<CategoriesRoute> {
                CategoriesScreen(
                    appVm = appVm,
                    onOpenSubject = { navController.navigate(SubjectRoute(it)) },
                    onManage = { navController.navigateToTab(AdminRoute(AdminTab.CATEGORIES), restore = false) },
                )
            }
            composable<AdminRoute> { entry ->
                AdminScreen(
                    appVm = appVm,
                    initialTab = entry.toRoute<AdminRoute>().tab,
                    onOpenSubject = { navController.navigate(SubjectRoute(it)) },
                )
            }
            composable<SettingsRoute> {
                SettingsScreen(appVm = appVm)
            }
            composable<SubjectRoute> { entry ->
                val id = entry.toRoute<SubjectRoute>().id
                SubjectScreen(
                    subjectId = id,
                    appVm = appVm,
                    onBack = { navController.popBackStack() },
                    onStudy = { navController.navigate(StudyRoute(id)) },
                    onPractice = { navController.navigate(PracticeRoute(id)) },
                )
            }
            composable<StudyRoute> { entry ->
                StudyScreen(
                    subjectId = entry.toRoute<StudyRoute>().subjectId,
                    appVm = appVm,
                    onBack = { navController.popBackStack() },
                )
            }
            composable<PracticeRoute> { entry ->
                PracticeScreen(
                    subjectId = entry.toRoute<PracticeRoute>().subjectId,
                    appVm = appVm,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

/**
 * Switches bottom-bar tab, keeping one back stack entry per tab. [restore] = false opens the route with its
 * own arguments instead of restoring the tab's saved state (e.g. "Manage categories" → Admin › Categories).
 */
private fun NavHostController.navigateToTab(route: Any, restore: Boolean = true) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = restore
    }
}
