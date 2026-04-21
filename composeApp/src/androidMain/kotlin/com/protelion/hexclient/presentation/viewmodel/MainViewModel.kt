package com.protelion.hexclient.presentation.viewmodel

import android.app.Application
import android.content.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.protelion.hexclient.domain.model.HexCode
import com.protelion.hexclient.domain.model.ServiceStatus
import com.protelion.hexclient.domain.repository.HexRepository
import com.protelion.hexclient.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
    private val repository: HexRepository,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    private val _serviceStatus = MutableStateFlow(ServiceStatus.STOPPED)
    val serviceStatus: StateFlow<ServiceStatus> = _serviceStatus.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _currentInterval = MutableStateFlow(1000L)
    val currentInterval: StateFlow<Long> = _currentInterval.asStateFlow()

    private val _totalGenerated = MutableStateFlow(0)
    val totalGenerated: StateFlow<Int> = _totalGenerated.asStateFlow()

    val isDarkTheme: StateFlow<Boolean?> = settingsRepository.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val history: StateFlow<List<HexCode>> = repository.getLatestCodes(50)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalCount: StateFlow<Int> = repository.getTotalCount()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.protelion.hexserver.STATUS_UPDATE" -> {
                    val statusStr = intent.getStringExtra("status") ?: "STOPPED"
                    _serviceStatus.value = try {
                        ServiceStatus.valueOf(statusStr)
                    } catch (e: Exception) {
                        ServiceStatus.STOPPED
                    }
                    _isGenerating.value = intent.getBooleanExtra("isGenerating", false)
                    _isPaused.value = intent.getBooleanExtra("isPaused", false)
                    _currentInterval.value = intent.getLongExtra("interval", 1000L)
                    _totalGenerated.value = intent.getIntExtra("totalGenerated", 0)
                }
                "com.protelion.hexserver.NEW_HEX" -> {
                    val value = intent.getStringExtra("hex")
                    value?.let { 
                        viewModelScope.launch {
                            repository.insertHex(it)
                        }
                    }
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction("com.protelion.hexserver.STATUS_UPDATE")
            addAction("com.protelion.hexserver.NEW_HEX")
        }
        val flag = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Context.RECEIVER_EXPORTED
        } else {
            0
        }
        application.registerReceiver(receiver, filter, flag)
        getStatus()
    }

    fun sendCommand(action: String, key: String?, value: Long?) {
        val fullAction = "com.protelion.hexserver.$action"
        val intent = Intent(fullAction).apply {
            component = ComponentName("com.protelion.hexserver", "com.protelion.hexserver.data.service.HexService")
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            key?.let { putExtra(it, value) }
        }
        try {
            androidx.core.content.ContextCompat.startForegroundService(getApplication(), intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleTheme(isDark: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDarkTheme(isDark)
        }
    }

    fun deleteCode(id: Long) {
        viewModelScope.launch {
            repository.deleteCode(id)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }

    fun restoreHistory() {
        sendCommand("ACTION_GET_HISTORY", null, null)
    }

    fun getStatus() {
        sendCommand("ACTION_GET_STATUS", null, null)
    }

    suspend fun getExportData(): String {
        val allCodes = repository.getAllCodesList()
        val sb = StringBuilder("ID,Hex Value,Timestamp\n")
        allCodes.forEach {
            sb.append("${it.id},${it.value},${it.timestamp}\n")
        }
        return sb.toString()
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unregisterReceiver(receiver)
    }
}
