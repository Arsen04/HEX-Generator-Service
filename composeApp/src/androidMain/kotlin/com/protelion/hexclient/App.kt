package com.protelion.hexclient

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.protelion.hexclient.presentation.screen.MainScreen
import com.protelion.hexclient.presentation.viewmodel.MainViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun App() {
    val viewModel: MainViewModel = koinViewModel()
    val userDarkTheme by viewModel.isDarkTheme.collectAsState()
    
    val darkTheme = userDarkTheme ?: isSystemInDarkTheme()
    
    val customDarkColorScheme = darkColorScheme(
        primary = Color(0xFFD0BCFF),
        secondary = Color(0xFFCCC2DC),
        tertiary = Color(0xFFEFB8C8),
        background = Color(0xFF121212), // Матовый черный
        surface = Color(0xFF121212),
        onBackground = Color.White,
        onSurface = Color.White
    )

    val colorScheme = if (darkTheme) {
        customDarkColorScheme
    } else {
        lightColorScheme()
    }

    MaterialTheme(colorScheme = colorScheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MainScreen(viewModel = viewModel)
        }
    }
}
