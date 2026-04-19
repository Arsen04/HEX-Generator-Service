package com.protelion.hexclient.presentation.viewmodel

import android.content.*
import android.os.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.protelion.hexclient.data.local.dao.HexDao
import com.protelion.hexclient.data.local.entity.HexEntity
import com.protelion.hexclient.domain.model.HexCode
import com.protelion.hexclient.data.service.HexService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    private val context: Context,
    private val repository: HexDao
) : ViewModel() {

    private val _serviceStatus = MutableStateFlow("STOPPED")
    val serviceStatus = _serviceStatus.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused = _isPaused.asStateFlow()

    private val _currentInterval = MutableStateFlow(1000L)
    val currentInterval = _currentInterval.asStateFlow()

    private val _totalCount = MutableStateFlow(0)
    val totalCount = _totalCount.asStateFlow()

    val history: StateFlow<List<HexCode>> = repository.getHistory()
        .map { list -> list.map { HexCode(it.id, it.value, it.timestamp) } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                HexService.ACTION_STATUS_REPLY -> {
                    val gen = intent.getBooleanExtra("isGenerating", false)
                    val paused = intent.getBooleanExtra("isPaused", false)
                    _isGenerating.value = gen
                    _isPaused.value = paused
                    _currentInterval.value = intent.getLongExtra("interval", 1000L)
                    _totalCount.value = intent.getIntExtra("count", 0)
                    
                    _serviceStatus.value = when {
                        paused -> "PAUSED"
                        gen -> "GENERATING"
                        else -> "IDLE"
                    }
                }
            }
        }
    }

    init {
        val filter = IntentFilter(HexService.ACTION_STATUS_REPLY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        restoreHistory()
    }

    fun sendCommand(action: String, key: String? = null, value: Long? = null) {
        val intent = Intent(context, HexService::class.java).apply {
            this.action = action
            if (key != null && value != null) putExtra(key, value)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun restoreHistory() {
        val intent = Intent(context, HexService::class.java).apply {
            action = HexService.CMD_GET_HISTORY
            putExtra("receiver", object : ResultReceiver(Handler(Looper.getMainLooper())) {
                override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                    if (resultCode == 200 && resultData != null) {
                        val codes = resultData.getStringArray("codes") ?: emptyArray()
                        val times = resultData.getLongArray("times") ?: longArrayOf()
                        
                        viewModelScope.launch {
                            codes.indices.forEach { i ->
                                repository.insert(HexEntity(value = codes[i], timestamp = times[i]))
                            }
                        }
                    }
                }
            })
        }
        context.startService(intent)
    }

    fun deleteCode(id: Long) = viewModelScope.launch { repository.deleteById(id) }

    fun clearAll() = viewModelScope.launch { repository.clearAll() }

    override fun onCleared() {
        super.onCleared()
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {}
    }
}
