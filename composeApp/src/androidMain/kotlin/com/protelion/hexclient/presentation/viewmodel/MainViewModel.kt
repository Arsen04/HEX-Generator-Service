package com.protelion.hexclient.presentation.viewmodel

import android.app.Application
import android.content.*
import android.os.*
import androidx.lifecycle.*
import com.protelion.hexclient.data.service.HexService
import com.protelion.hexclient.domain.model.HexCode
import com.protelion.hexclient.domain.repository.HexRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    private val app: Application,
    private val repository: HexRepository
) : ViewModel() {

    private val _serviceStatus = MutableStateFlow("Stopped")
    val serviceStatus = _serviceStatus.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private val _currentInterval = MutableStateFlow(1000L)
    val currentInterval = _currentInterval.asStateFlow()

    val history: StateFlow<List<HexCode>> = repository.getLocalHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val gen = it.getBooleanExtra("isGenerating", false)
                val paused = it.getBooleanExtra("isPaused", false)
                _isGenerating.value = gen
                _currentInterval.value = it.getLongExtra("interval", 1000L)
                _serviceStatus.value = when {
                    paused -> "PAUSE"
                    gen -> "GENERATING"
                    else -> "RUNNING"
                }
            }
        }
    }

    init {
        val filter = IntentFilter(HexService.ACTION_STATUS_REPLY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            app.registerReceiver(statusReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            app.registerReceiver(statusReceiver, filter)
        }
        requestHistoryFromService()
    }

    fun sendCommand(action: String, extraKey: String? = null, extraValue: Long? = null) {
        val intent = Intent(app, HexService::class.java).apply {
            this.action = action
            extraKey?.let { putExtra(it, extraValue) }
        }
        app.startService(intent)
    }

    private fun requestHistoryFromService() {
        val receiver = object : ResultReceiver(Handler(Looper.getMainLooper())) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                val codes = resultData?.getStringArray("codes") ?: return
                val times = resultData.getLongArray("times") ?: return
                viewModelScope.launch {
                    codes.forEachIndexed { i, s ->
                        repository.saveCode(HexCode(value = s, timestamp = times[i]))
                    }
                }
            }
        }
        val intent = Intent(app, HexService::class.java).apply {
            action = HexService.CMD_GET_HISTORY
            putExtra("receiver", receiver)
        }
        app.startService(intent)
    }

    fun deleteCode(id: Long) = viewModelScope.launch { repository.deleteCode(id) }
    fun clearAll() = viewModelScope.launch { repository.clearAll() }

    override fun onCleared() {
        super.onCleared()
        app.unregisterReceiver(statusReceiver)
    }
}
