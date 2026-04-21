package com.protelion.hexclient.presentation.viewmodel

import android.app.Application
import android.content.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.protelion.hexclient.domain.model.HexCode
import com.protelion.hexclient.domain.model.ServiceStatus
import com.protelion.hexclient.domain.repository.HexRepository
import com.protelion.hexclient.domain.repository.SettingsRepository
import com.protelion.hexclient.ipc.IpcConstants
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
                IpcConstants.BROADCAST_STATUS -> {
                    val statusStr = intent.getStringExtra(IpcConstants.EXTRA_STATUS) ?: ServiceStatus.STOPPED.name
                    _serviceStatus.value = try {
                        ServiceStatus.valueOf(statusStr)
                    } catch (e: Exception) {
                        ServiceStatus.STOPPED
                    }
                    _isGenerating.value = intent.getBooleanExtra(IpcConstants.EXTRA_IS_GENERATING, false)
                    _isPaused.value = intent.getBooleanExtra(IpcConstants.EXTRA_IS_PAUSED, false)
                    _currentInterval.value = intent.getLongExtra(IpcConstants.EXTRA_INTERVAL, 1000L)
                    _totalGenerated.value = intent.getIntExtra(IpcConstants.EXTRA_TOTAL_GENERATED, 0)
                }
                IpcConstants.BROADCAST_NEW_HEX -> {
                    val value = intent.getStringExtra(IpcConstants.EXTRA_HEX)
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
            addAction(IpcConstants.BROADCAST_STATUS)
            addAction(IpcConstants.BROADCAST_NEW_HEX)
        }
        val flag = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Context.RECEIVER_EXPORTED
        } else {
            0
        }
        application.registerReceiver(receiver, filter, flag)
        getStatus()
    }

    fun sendCommand(action: String, key: String? = null, value: Long? = null) {
        val intent = Intent(action).apply {
            component = ComponentName(IpcConstants.SERVER_PACKAGE, IpcConstants.SERVER_SERVICE_CLASS)
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
        sendCommand(IpcConstants.ACTION_GET_HISTORY)
    }

    fun getStatus() {
        sendCommand(IpcConstants.ACTION_GET_STATUS)
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
