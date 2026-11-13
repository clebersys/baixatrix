package com.cleber.baixavideo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

private const val SCREEN_HOME = "home"
private const val SCREEN_LIBRARY = "library"
private const val SCREEN_SETTINGS = "settings"

/** Navegação simples entre as três telas, sem dependência extra. */
@Composable
fun BaixaVideoApp(viewModel: HomeViewModel) {
    var screen by rememberSaveable { mutableStateOf(SCREEN_HOME) }

    BackHandler(enabled = screen != SCREEN_HOME) { screen = SCREEN_HOME }

    Crossfade(targetState = screen, label = "screens") { target ->
        when (target) {
            SCREEN_LIBRARY -> LibraryScreen(onBack = { screen = SCREEN_HOME })
            SCREEN_SETTINGS -> SettingsScreen(onBack = { screen = SCREEN_HOME })
            else -> HomeScreen(
                viewModel = viewModel,
                onOpenLibrary = { screen = SCREEN_LIBRARY },
                onOpenSettings = { screen = SCREEN_SETTINGS }
            )
        }
    }
}
