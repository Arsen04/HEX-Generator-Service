package com.protelion.hexclient

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.protelion.hexclient.presentation.screen.MainScreen
import com.protelion.hexclient.presentation.viewmodel.MainViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun App() {
    MaterialTheme {
        val viewModel: MainViewModel = koinViewModel()
        MainScreen(viewModel = viewModel)
    }
}
