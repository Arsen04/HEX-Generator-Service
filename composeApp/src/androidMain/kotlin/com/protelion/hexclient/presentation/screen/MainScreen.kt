package com.protelion.hexclient.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.protelion.hexclient.presentation.viewmodel.MainViewModel
import com.protelion.hexclient.data.service.HexService
import com.protelion.hexclient.domain.model.HexCode

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val status by viewModel.serviceStatus.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val interval by viewModel.currentInterval.collectAsState()
    val codes by viewModel.history.collectAsState()

    MainScreenContent(
        status = status,
        isGenerating = isGenerating,
        interval = interval,
        codes = codes,
        onSendCommand = {
            action,
            key,
            value -> viewModel.sendCommand(action, key, value)
        },
        onDeleteCode = { viewModel.deleteCode(it) },
        onClearAll = { viewModel.clearAll() }
    )
}

@Composable
fun MainScreenContent(
    status: String,
    isGenerating: Boolean,
    interval: Long,
    codes: List<HexCode>,
    onSendCommand: (String, String?, Long?) -> Unit,
    onDeleteCode: (Long) -> Unit,
    onClearAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Status: $status", style = MaterialTheme.typography.headlineMedium)

        if (isGenerating) {
            Text("Generating...", color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Interval: ${interval}ms")
        Slider(
            value = interval.toFloat(),
            onValueChange = { onSendCommand(HexService.CMD_SET_INTERVAL, "interval", it.toLong()) },
            valueRange = 100f..5000f
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(codes, key = { it.timestamp }) { hex ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    onClick = { onDeleteCode(hex.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(hex.value)
                        Text(java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(hex.timestamp))
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { onSendCommand(HexService.CMD_START, null, null) }) {
                Text("Start")
            }
            Button(onClick = { onSendCommand(HexService.CMD_STOP, null, null) }) {
                Text("Stop")
            }
            Button(onClick = { onClearAll() }) {
                Text("Clear")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MaterialTheme {
        MainScreenContent(
            status = "RUNNING",
            isGenerating = true,
            interval = 1000L,
            codes = listOf(
                HexCode(1, "ABCDEF", System.currentTimeMillis()),
                HexCode(2, "123456", System.currentTimeMillis() - 10000)
            ),
            onSendCommand = { _, _, _ -> },
            onDeleteCode = {},
            onClearAll = {}
        )
    }
}
