package com.mamatiquest.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.mamatiquest.app.models.GameMode
import com.mamatiquest.app.models.GameTheme
import com.mamatiquest.app.services.HapticsManager
import com.mamatiquest.app.services.SettingsStore
import com.mamatiquest.app.services.SoundManager
import com.mamatiquest.app.ui.navigation.GameThemeNavType
import com.mamatiquest.app.ui.navigation.HomeRoute
import com.mamatiquest.app.ui.navigation.LevelMapRoute
import com.mamatiquest.app.ui.navigation.MultiplayerRoute
import com.mamatiquest.app.ui.navigation.NullableGameThemeNavType
import com.mamatiquest.app.ui.navigation.SettingsRoute
import com.mamatiquest.app.ui.navigation.ThemeSelectRoute
import com.mamatiquest.app.ui.navigation.TriviaGameRoute
import com.mamatiquest.app.ui.navigation.WordSearchGameRoute
import kotlin.reflect.typeOf
import com.mamatiquest.app.ui.screens.home.HomeScreen
import com.mamatiquest.app.ui.screens.levelmap.LevelMapScreen
import com.mamatiquest.app.ui.screens.multiplayer.MultiplayerLobbyScreen
import com.mamatiquest.app.ui.screens.settings.SettingsScreen
import com.mamatiquest.app.ui.screens.theme.ThemeSelectScreen
import com.mamatiquest.app.ui.screens.trivia.TriviaScreen
import com.mamatiquest.app.ui.screens.wordsearch.WordSearchScreen
import com.mamatiquest.app.ui.theme.WordQuestTheme

/**
 * Root composable: the Navigation-Compose graph mirroring iOS's
 * `RootView`/`Router`/`AppRoute` (Home -> ThemeSelect -> LevelMap ->
 * WordSearch/Trivia game, Settings, Multiplayer).
 */
@Composable
fun WordQuestApp() {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore.getInstance(context) }
    val appearance by settingsStore.appearance.collectAsState()
    val soundEnabled by settingsStore.soundEnabled.collectAsState()
    val hapticsEnabled by settingsStore.hapticsEnabled.collectAsState()

    LaunchedEffect(soundEnabled) { SoundManager.isEnabled = soundEnabled }
    LaunchedEffect(hapticsEnabled) { HapticsManager.isEnabled = hapticsEnabled }

    WordQuestTheme(appearance = appearance) {
        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = HomeRoute) {
            composable<HomeRoute> {
                HomeScreen(
                    onNavigateThemeSelect = { mode -> navController.navigate(ThemeSelectRoute(mode)) },
                    onNavigateMultiplayer = { theme -> navController.navigate(MultiplayerRoute(theme)) },
                    onNavigateSettings = { navController.navigate(SettingsRoute) },
                    onNavigateLevelMap = { mode, theme -> navController.navigate(LevelMapRoute(mode, theme)) },
                )
            }

            composable<ThemeSelectRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<ThemeSelectRoute>()
                ThemeSelectScreen(
                    mode = route.mode,
                    onBack = { navController.popBackStack() },
                    onThemeSelected = { theme -> navController.navigate(LevelMapRoute(route.mode, theme)) },
                )
            }

            composable<LevelMapRoute>(
                typeMap = mapOf(typeOf<GameTheme>() to GameThemeNavType),
            ) { backStackEntry ->
                val route = backStackEntry.toRoute<LevelMapRoute>()
                LevelMapScreen(
                    mode = route.mode,
                    theme = route.theme,
                    onBack = { navController.popBackStack() },
                    onLevelSelected = { level ->
                        val destination = if (route.mode == GameMode.WORD_SEARCH) {
                            WordSearchGameRoute(route.theme, level)
                        } else {
                            TriviaGameRoute(route.theme, level)
                        }
                        navController.navigate(destination)
                    },
                )
            }

            composable<WordSearchGameRoute>(
                typeMap = mapOf(typeOf<GameTheme>() to GameThemeNavType),
            ) { backStackEntry ->
                val route = backStackEntry.toRoute<WordSearchGameRoute>()
                WordSearchScreen(
                    theme = route.theme,
                    level = route.level,
                    onBack = { navController.popToHome() },
                    onReplay = {
                        navController.navigate(WordSearchGameRoute(route.theme, route.level)) {
                            popUpTo<WordSearchGameRoute> { inclusive = true }
                        }
                    },
                    onNext = {
                        navController.navigate(WordSearchGameRoute(route.theme, route.level + 1)) {
                            popUpTo<WordSearchGameRoute> { inclusive = true }
                        }
                    },
                )
            }

            composable<TriviaGameRoute>(
                typeMap = mapOf(typeOf<GameTheme>() to GameThemeNavType),
            ) { backStackEntry ->
                val route = backStackEntry.toRoute<TriviaGameRoute>()
                TriviaScreen(
                    theme = route.theme,
                    level = route.level,
                    onBack = { navController.popToHome() },
                    onReplay = {
                        navController.navigate(TriviaGameRoute(route.theme, route.level)) {
                            popUpTo<TriviaGameRoute> { inclusive = true }
                        }
                    },
                    onNext = {
                        navController.navigate(TriviaGameRoute(route.theme, route.level + 1)) {
                            popUpTo<TriviaGameRoute> { inclusive = true }
                        }
                    },
                )
            }

            composable<SettingsRoute> {
                SettingsScreen(onBack = { navController.popBackStack() })
            }

            composable<MultiplayerRoute>(
                typeMap = mapOf(typeOf<GameTheme?>() to NullableGameThemeNavType),
            ) { backStackEntry ->
                val route = backStackEntry.toRoute<MultiplayerRoute>()
                MultiplayerLobbyScreen(initialTheme = route.theme, onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun androidx.navigation.NavController.popToHome() {
    popBackStack(HomeRoute, inclusive = false)
}
