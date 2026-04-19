package com.protelion.hexclient.data.service

import android.app.*
import android.content.*
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.protelion.hexclient.data.local.dao.HexDao
import com.protelion.hexclient.data.local.entity.HexEntity
import com.protelion.hexclient.domain.usecase.GenerateHexUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.koin.android.ext.android.inject
import java.util.concurrent.CopyOnWriteArrayList

class HexService : LifecycleService() {
    private val hexDao: HexDao by inject()
    private val generateHexUseCase: GenerateHexUseCase by inject()

    private var interval = 1000L
    private var isGenerating = false
    private var isPaused = false
    private var totalGenerated = 0
    private var job: Job? = null

    private val serviceHistory = CopyOnWriteArrayList<HexEntity>()

    companion object {
        const val CHANNEL_ID = "hex_gen_channel"
        const val ACTION_NEW_CODE = "com.protelion.hex.NEW_CODE"
        const val ACTION_STATUS_REPLY = "com.protelion.hex.STATUS_REPLY"

        const val CMD_START = "START"
        const val CMD_STOP = "STOP"
        const val CMD_TOGGLE_GEN = "TOGGLE_GEN"
        const val CMD_PAUSE = "PAUSE"
        const val CMD_SET_INTERVAL = "SET_INTERVAL"
        const val CMD_GET_HISTORY = "GET_HISTORY"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        createNotificationChannel()

        when (intent?.action) {
            CMD_START -> { 
                isGenerating = false 
                isPaused = false 
            }
            CMD_STOP -> stopSelf()
            CMD_TOGGLE_GEN -> isGenerating = !isGenerating
            CMD_PAUSE -> isPaused = !isPaused
            CMD_SET_INTERVAL -> interval = intent.getLongExtra("interval", 1000L)
            CMD_GET_HISTORY -> {
                val receiver = intent.getParcelableExtra<ResultReceiver>("receiver")
                sendHistoryToClient(receiver)
            }
        }

        updateNotification()
        broadcastStatus()
        startGenerationFlow()
        return START_STICKY
    }

    private fun startGenerationFlow() {
        if (job?.isActive == true) return
        job = lifecycleScope.launch {
            tickerFlow().collect {
                if (isGenerating && !isPaused) {
                    val code = generateHexUseCase()
                    val entity = HexEntity(value = code, timestamp = System.currentTimeMillis())
                    
                    serviceHistory.add(0, entity)
                    if (serviceHistory.size > 100) {
                        serviceHistory.removeAt(serviceHistory.size - 1)
                    }

                    hexDao.insertAndTrim(entity)
                    totalGenerated++

                    sendBroadcast(Intent(ACTION_NEW_CODE).apply {
                        putExtra("code", code)
                        putExtra("time", entity.timestamp)
                    })
                }
                broadcastStatus()
                updateNotification()
            }
        }
    }

    private fun tickerFlow() = flow {
        while (true) {
            emit(Unit)
            delay(interval)
        }
    }.flowOn(Dispatchers.Default)

    private fun broadcastStatus() {
        sendBroadcast(Intent(ACTION_STATUS_REPLY).apply {
            putExtra("isGenerating", isGenerating)
            putExtra("isPaused", isPaused)
            putExtra("interval", interval)
            putExtra("count", totalGenerated)
        })
    }

    private fun sendHistoryToClient(receiver: ResultReceiver?) {
        val history = serviceHistory.take(50)
        val bundle = Bundle().apply {
            putStringArray("codes", history.map { it.value }.toTypedArray())
            putLongArray("times", history.map { it.timestamp }.toLongArray())
        }
        receiver?.send(200, bundle)
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1, buildNotification())
    }

    private fun buildNotification(): Notification {
        val stopPending = createPendingIntent(CMD_STOP)
        val togglePending = createPendingIntent(CMD_TOGGLE_GEN)
        val pausePending = createPendingIntent(CMD_PAUSE)

        val statusText = when {
            isPaused -> "PAUSED"
            isGenerating -> "GENERATING"
            else -> "IDLE"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("HEX Service: $statusText")
            .setContentText("Total: $totalGenerated | Interval: ${interval}ms")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .addAction(0, if (isGenerating) "Stop Gen" else "Start Gen", togglePending)
            .addAction(0, if (isPaused) "Resume" else "Pause", pausePending)
            .addAction(0, "Exit", stopPending)
            .setOngoing(true)
            .build()
    }

    private fun createPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, HexService::class.java).apply { this.action = action }
        return PendingIntent.getService(this, action.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "HEX Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
