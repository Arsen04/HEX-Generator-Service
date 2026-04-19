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
import org.koin.android.ext.android.inject

class HexService : LifecycleService() {
    private val hexDao: HexDao by inject()
    private val generateHexUseCase: GenerateHexUseCase by inject()

    private var interval = 1000L
    private var isGenerating = false
    private var isPaused = false
    private var totalGenerated = 0
    private var job: Job? = null

    companion object {
        const val CHANNEL_ID = "hex_gen_channel"
        const val ACTION_NEW_CODE = "com.protelion.hex.NEW_CODE"
        const val ACTION_STATUS_REPLY = "com.protelion.hex.STATUS_REPLY"

        // Commands
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
            CMD_START -> { isGenerating = false; isPaused = false }
            CMD_STOP -> stopSelf()
            CMD_TOGGLE_GEN -> isGenerating = !isGenerating
            CMD_PAUSE -> isPaused = !isPaused //  [cite: 59, 61]
            CMD_SET_INTERVAL -> interval = intent.getLongExtra("interval", 1000L)
            CMD_GET_HISTORY -> {
                val receiver = intent.getParcelableExtra<ResultReceiver>("receiver")
                sendHistoryToClient(receiver)
            }
        }

        startForeground(1, buildNotification())
        broadcastStatus()
        startLoop()
        return START_STICKY
    }

    private fun startLoop() {
        job?.cancel()
        job = lifecycleScope.launch {
            while (true) {
                if (isGenerating && !isPaused) {
                    val code = generateHexUseCase()
                    val entity = HexEntity(value = code, timestamp = System.currentTimeMillis())
                    hexDao.insert(entity)
                    totalGenerated++

                    sendBroadcast(Intent(ACTION_NEW_CODE).apply {
                        putExtra("code", code)
                        putExtra("time", entity.timestamp)
                    }) // [cite: 24, 42]
                }
                delay(interval) // [cite: 60, 61]
                broadcastStatus()
            }
        }
    }

    private fun broadcastStatus() {
        val statusIntent = Intent(ACTION_STATUS_REPLY).apply {
            putExtra("isGenerating", isGenerating)
            putExtra("isPaused", isPaused)
            putExtra("interval", interval)
            putExtra("count", totalGenerated)
        }
        sendBroadcast(statusIntent) // [cite: 34]
    }

    private fun sendHistoryToClient(receiver: ResultReceiver?) {
        lifecycleScope.launch {
            val history = hexDao.getLastN(50) // [cite: 62]
            val bundle = Bundle().apply {
                putStringArray("codes", history.map { it.value }.toTypedArray())
                putLongArray("times", history.map { it.timestamp }.toLongArray())
            }
            receiver?.send(200, bundle) // [cite: 29, 56]
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this,
                HexService::class.java
            ).apply { action = CMD_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("HEX Generator: ${if(isPaused) "PAUSED" else if(isGenerating) "GENERATING" else "IDLE"}")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Exit", stopIntent) // [cite: 63, 66]
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "HEX Generator Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}