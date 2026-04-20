package com.protelion.hexclient

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.protelion.hexclient.presentation.screen.MainScreen
import com.protelion.hexclient.presentation.viewmodel.MainViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun App() {
    val viewModel: MainViewModel = koinViewModel()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val systemDark = isSystemInDarkTheme()
    
    val colorScheme = if (isDarkTheme ?: systemDark) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }

    MaterialTheme(colorScheme = colorScheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MainScreen(viewModel = viewModel)
        }
    }
}
