/*
 * Cleanify
 * Copyright (c) 2025 LoopOtto
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.cleanify.ui.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import com.cleanify.ui.screens.duplicates.DuplicatesScreen
import com.cleanify.ui.screens.duplicates.GroupDetailsScreen
import com.cleanify.ui.screens.duplicates.DuplicatesViewModel
import com.cleanify.ui.screens.onboarding.OnboardingScreen
import com.cleanify.ui.screens.osslicenses.OpenSourceLicensesScreen
import com.cleanify.ui.screens.session.SessionSetupScreen
import com.cleanify.ui.screens.session.SessionSetupViewModel
import com.cleanify.ui.screens.settings.SettingsScreen
import com.cleanify.ui.screens.swiper.SwiperScreen
import com.cleanify.ui.screens.contacts.ContactCleanerScreen
import com.cleanify.ui.screens.recyclebin.RecycleBinScreen
import com.cleanify.ui.screens.tools.BlacklistEditorScreen
import com.cleanify.ui.screens.tools.CleanerSettingsScreen
import com.cleanify.ui.screens.tools.EmptyCleanerScreen
import com.cleanify.ui.screens.storage.StorageAnalysisScreen
import com.cleanify.ui.screens.tools.ToolsScreen
import com.cleanify.ui.screens.tools.WhitelistEditorScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// A constant for the new nested graph route.
const val DUPLICATES_GRAPH_ROUTE = "duplicates_graph"
private const val DEEP_LINK_URI_BASE = "app://com.cleanify"
const val RESET_SEARCH_RESULT_KEY = "reset_search_result"

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object SessionSetup : Screen("session_setup?forceRefresh={forceRefresh}") {
        fun createRoute(forceRefresh: Boolean = false): String {
            return "session_setup?forceRefresh=$forceRefresh"
        }
    }
    object Swiper : Screen("swiper/{bucketIds}") {
        fun createRoute(bucketIds: List<String>): String {
            val encodedPaths = bucketIds.joinToString("|") { path ->
                // Using standard Base64 encoding for paths
                android.util.Base64.encodeToString(
                    path.toByteArray(StandardCharsets.UTF_8),
                    android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
                )
            }
            return "swiper/${URLEncoder.encode(encodedPaths, StandardCharsets.UTF_8.toString())}"
        }
    }
    object Settings : Screen("settings?page={page}") {
        fun createRoute(page: String? = null): String {
            return if (page != null) "settings?page=$page" else "settings"
        }
    }
    object Libraries: Screen("libraries")
    object Tools : Screen("tools")
    object EmptyCleaner : Screen("empty_cleaner")
    object CleanerSettings : Screen("cleaner_settings")
    object CleanerBlacklist : Screen("cleaner_blacklist")
    object CleanerWhitelist : Screen("cleaner_whitelist")
    object RecycleBin : Screen("recycle_bin")
    object ContactCleaner : Screen("contact_cleaner")
    object StorageAnalysis : Screen("storage_analysis")

    // Routes for the duplicates feature, now part of a nested graph
    object Duplicates : Screen("duplicates_overview")
    object GroupDetails : Screen("duplicates_group_details/{groupId}") {
        fun createRoute(groupId: String): String {
            // Encode the group ID to be safe for navigation
            val encodedGroupId = URLEncoder.encode(groupId, StandardCharsets.UTF_8.toString())
            return "duplicates_group_details/$encodedGroupId"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    windowSizeClass: WindowSizeClass,
    startDestination: String = Screen.Onboarding.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.SessionSetup.createRoute(forceRefresh = true)) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.SessionSetup.route,
            arguments = listOf(navArgument("forceRefresh") {
                type = NavType.BoolType
                defaultValue = false
            })
        ) { backStackEntry ->
            val viewModel = hiltViewModel<SessionSetupViewModel>()

            val result by backStackEntry
                .savedStateHandle
                .getStateFlow(RESET_SEARCH_RESULT_KEY, false)
                .collectAsStateWithLifecycle()

            LaunchedEffect(result) {
                if (result) {
                    viewModel.handleResetResult()
                }
            }

            SessionSetupScreen(
                windowSizeClass = windowSizeClass,
                onStartSession = { bucketIds ->
                    viewModel.saveSelectedBucketsPreference()
                    navController.navigate(Screen.Swiper.createRoute(bucketIds))
                },
                onNavigateToSettings = { navController.navigate(Screen.Settings.createRoute()) },
                onNavigateToTools = { navController.navigate(Screen.Tools.route) },
                viewModel = viewModel
            )
        }

        composable(
            route = Screen.Swiper.route,
            arguments = listOf(navArgument("bucketIds") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedBucketIds = backStackEntry.arguments?.getString("bucketIds") ?: ""
            val bucketIds = try {
                if (encodedBucketIds.isNotEmpty()) {
                    val decodedString = URLDecoder.decode(encodedBucketIds, StandardCharsets.UTF_8.toString())
                    decodedString.split("|").map { encodedPath ->
                        String(android.util.Base64.decode(encodedPath, android.util.Base64.URL_SAFE), StandardCharsets.UTF_8)
                    }.filter { it.isNotEmpty() }
                } else { emptyList() }
            } catch (e: Exception) { emptyList() }

            SwiperScreen(
                windowSizeClass = windowSizeClass,
                bucketIds = bucketIds,
                onNavigateUp = { navController.navigateUp() },
                onNavigateUpAndReset = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(RESET_SEARCH_RESULT_KEY, true)
                    navController.popBackStack()
                },
onNavigateToSettings = { navController.navigate(Screen.Settings.createRoute("sorting")) },
                onNavigateToTools = { navController.navigate(Screen.Tools.route) }
            )
        }

        composable(
            route = Screen.RecycleBin.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = "$DEEP_LINK_URI_BASE/recycle_bin" }
            )
        ) {
            RecycleBinScreen(
                onNavigateUp = { navController.navigateUp() }
            )
        }

        composable(
            route = Screen.ContactCleaner.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = "$DEEP_LINK_URI_BASE/contact_cleaner" }
            )
        ) {
            ContactCleanerScreen(
                onNavigateUp = { navController.navigateUp() },
                onNavigateToSettings = { navController.navigate(Screen.Settings.createRoute("contact_cleaner")) }
            )
        }

        composable(Screen.Tools.route) {
            ToolsScreen(
                onNavigateUp = { navController.navigateUp() },
                onNavigateToDuplicates = { navController.navigate(DUPLICATES_GRAPH_ROUTE) },
                onNavigateToEmptyCleaner = { navController.navigate(Screen.EmptyCleaner.route) },
                onNavigateToRecycleBin = { navController.navigate(Screen.RecycleBin.route) },
                onNavigateToContactCleaner = { navController.navigate(Screen.ContactCleaner.route) },
                onNavigateToStorageAnalysis = { navController.navigate(Screen.StorageAnalysis.route) }
            )
        }

        composable(Screen.StorageAnalysis.route) {
            StorageAnalysisScreen(
                onNavigateUp = { navController.navigateUp() }
            )
        }

        composable(
            route = Screen.EmptyCleaner.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = "$DEEP_LINK_URI_BASE/empty_cleaner" }
            )
        ) {
            EmptyCleanerScreen(
                onNavigateUp = { navController.navigateUp() },
                onNavigateToSettings = { navController.navigate(Screen.CleanerSettings.route) }
            )
        }

        composable(Screen.CleanerSettings.route) {
            CleanerSettingsScreen(
                onNavigateUp = { navController.navigateUp() },
                onNavigateToBlacklist = { navController.navigate(Screen.CleanerBlacklist.route) },
                onNavigateToWhitelist = { navController.navigate(Screen.CleanerWhitelist.route) }
            )
        }

        composable(Screen.CleanerBlacklist.route) {
            BlacklistEditorScreen(
                onNavigateUp = { navController.navigateUp() }
            )
        }

        composable(Screen.CleanerWhitelist.route) {
            WhitelistEditorScreen(
                onNavigateUp = { navController.navigateUp() }
            )
        }

        composable(
            route = Screen.Settings.route,
            arguments = listOf(navArgument("page") {
                type = NavType.StringType
                defaultValue = null
                nullable = true
            })
        ) { backStackEntry ->
            val initialPage = backStackEntry.arguments?.getString("page")
            SettingsScreen(
                onNavigateUp = { navController.navigateUp() },
                onNavigateToLibraries = { navController.navigate(Screen.Libraries.route) },
                onNavigateToCleanerSettings = { navController.navigate(Screen.CleanerSettings.route) },
                initialPage = initialPage
            )
        }

        composable(Screen.Libraries.route) {
            OpenSourceLicensesScreen(
                onNavigateUp = { navController.navigateUp() }
            )
        }

        // --- Duplicates Nested Navigation Graph ---
        navigation(
            startDestination = Screen.Duplicates.route,
            route = DUPLICATES_GRAPH_ROUTE
        ) {
            // Duplicates Overview Screen
            composable(
                route = Screen.Duplicates.route,
                deepLinks = listOf(
                    navDeepLink { uriPattern = "$DEEP_LINK_URI_BASE/$DUPLICATES_GRAPH_ROUTE" }
                )
            ) { navBackStackEntry ->
                val parentEntry = remember(navBackStackEntry) {
                    navController.getBackStackEntry(DUPLICATES_GRAPH_ROUTE)
                }
                val viewModel = hiltViewModel<DuplicatesViewModel>(parentEntry)

                DuplicatesScreen(
                    viewModel = viewModel,
                    onNavigateUp = { navController.navigateUp() },
                    onNavigateToGroup = { groupId ->
                        navController.navigate(Screen.GroupDetails.createRoute(groupId))
                    },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.createRoute("duplicates")) }
                )
            }

            // Group Details Screen
            composable(
                route = Screen.GroupDetails.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { navBackStackEntry ->
                val parentEntry = remember(navBackStackEntry) {
                    navController.getBackStackEntry(DUPLICATES_GRAPH_ROUTE)
                }
                val viewModel = hiltViewModel<DuplicatesViewModel>(parentEntry)
                val encodedGroupId = navBackStackEntry.arguments?.getString("groupId") ?: ""
                val groupId = URLDecoder.decode(encodedGroupId, StandardCharsets.UTF_8.toString())

                GroupDetailsScreen(
                    viewModel = viewModel,
                    groupId = groupId,
                    onNavigateUp = { navController.navigateUp() }
                )
            }
        }
    }
}
