package com.protelion.hexclient.presentation.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.protelion.hexclient.R
import com.protelion.hexclient.domain.model.HexCode
import com.protelion.hexclient.domain.model.ServiceStatus
import com.protelion.hexclient.presentation.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val status by viewModel.serviceStatus.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val interval by viewModel.currentInterval.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val totalGeneratedByService by viewModel.totalGenerated.collectAsState()
    val codes by viewModel.history.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            scope.launch {
                val data = viewModel.getExportData()
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(data.toByteArray())
                }
            }
        }
    }

    MainScreenContent(
        status = status,
        isGenerating = isGenerating,
        isPaused = isPaused,
        interval = interval,
        totalCount = totalCount,
        totalGeneratedByService = totalGeneratedByService,
        codes = codes,
        isDarkTheme = isDarkTheme ?: systemDark,
        onToggleTheme = { viewModel.toggleTheme(it) },
        onSendCommand = { action, key, value -> viewModel.sendCommand(action, key, value) },
        onRestoreHistory = { viewModel.restoreHistory() },
        onDeleteCode = { viewModel.deleteCode(it) },
        onClearAll = { viewModel.clearAll() },
        onExport = { exportLauncher.launch("hex_history_${System.currentTimeMillis()}.csv") }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreenContent(
    status: ServiceStatus,
    isGenerating: Boolean,
    isPaused: Boolean,
    interval: Long,
    totalCount: Int,
    totalGeneratedByService: Int,
    codes: List<HexCode>,
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onSendCommand: (String, String?, Long?) -> Unit,
    onRestoreHistory: () -> Unit,
    onDeleteCode: (Long) -> Unit,
    onClearAll: () -> Unit,
    onExport: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val sdf = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    val statusColor by animateColorAsState(
        targetValue = when(status) {
            ServiceStatus.GENERATING -> MaterialTheme.colorScheme.primaryContainer
            ServiceStatus.PAUSED -> MaterialTheme.colorScheme.secondaryContainer
            ServiceStatus.STOPPED -> MaterialTheme.colorScheme.surfaceVariant
            ServiceStatus.IDLE -> MaterialTheme.colorScheme.tertiaryContainer
        },
        animationSpec = tween(500),
        label = "StatusColorAnimation"
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).safeDrawingPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.dark_mode), style = MaterialTheme.typography.titleMedium)
            Switch(checked = isDarkTheme, onCheckedChange = onToggleTheme)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = statusColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val displayStatus = when(status) {
                    ServiceStatus.STOPPED -> stringResource(R.string.status_stopped)
                    ServiceStatus.IDLE -> stringResource(R.string.status_idle)
                    ServiceStatus.GENERATING -> stringResource(R.string.status_generating)
                    ServiceStatus.PAUSED -> stringResource(R.string.status_paused)
                }
                Text(stringResource(R.string.status_format, displayStatus), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.local_count_format, totalCount))
                Text(stringResource(R.string.service_total_format, totalGeneratedByService))
                Text(stringResource(R.string.interval_format, interval))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResource(R.string.set_interval), style = MaterialTheme.typography.labelLarge)
        Slider(
            value = interval.toFloat(),
            onValueChange = { onSendCommand("ACTION_SET_INTERVAL", "interval", it.toLong()) },
            valueRange = 100f..5000f
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(codes, key = { "${it.id}_${it.timestamp}" }) { hex ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(
                            placementSpec = tween(durationMillis = 500),
                            fadeInSpec = null,
                            fadeOutSpec = null
                        ),
                    onClick = { clipboardManager.setText(AnnotatedString(hex.value)) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(hex.value, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                sdf.format(Date(hex.timestamp)),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        IconButton(onClick = { onDeleteCode(hex.id) }) {
                            Text(stringResource(R.string.delete_label))
                        }
                    }
                }
            }
        }

        val isServiceRunning = status != ServiceStatus.STOPPED

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onSendCommand("ACTION_START", null, null) },
                modifier = Modifier.weight(1f),
                enabled = !isServiceRunning
            ) { Text(stringResource(R.string.start_service)) }

            Button(
                onClick = { onSendCommand("ACTION_TOGGLE_GEN", null, null) },
                modifier = Modifier.weight(1f),
                enabled = isServiceRunning
            ) { Text(if (isGenerating) stringResource(R.string.stop_gen) else stringResource(R.string.start_gen)) }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onSendCommand("ACTION_PAUSE", null, null) },
                modifier = Modifier.weight(1f),
                enabled = isServiceRunning && isGenerating
            ) { Text(if (isPaused) stringResource(R.string.resume) else stringResource(R.string.pause)) }

            Button(
                onClick = { onRestoreHistory() },
                modifier = Modifier.weight(1f),
                enabled = isServiceRunning
            ) { Text(stringResource(R.string.restore_50)) }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onClearAll() },
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.clear_all)) }

            OutlinedButton(
                onClick = { onExport() },
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.export_csv)) }
        }

        Button(
            onClick = { onSendCommand("ACTION_STOP", null, null) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            enabled = isServiceRunning
        ) { Text(stringResource(R.string.stop_service)) }
    }
}
