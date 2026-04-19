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
            CMD_START -> { isGenerating = true; isPaused = false }
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
        startLoop()
        return START_STICKY
    }

    private fun startLoop() {
        if (job?.isActive == true) return
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
                    })
                }
                delay(interval)
                broadcastStatus()
                updateNotification()
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
        sendBroadcast(statusIntent)
    }

    private fun sendHistoryToClient(receiver: ResultReceiver?) {
        lifecycleScope.launch {
            val history = hexDao.getLastN(100)
            val bundle = Bundle().apply {
                putStringArray("codes", history.map { it.value }.toTypedArray())
                putLongArray("times", history.map { it.timestamp }.toLongArray())
            }
            receiver?.send(200, bundle)
        }
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, buildNotification())
    }

    private fun buildNotification(): Notification {
        val stopIntent = PendingIntent.getService(this, 0, Intent(this, HexService::class.java).apply { action = CMD_STOP }, PendingIntent.FLAG_IMMUTABLE)
        val toggleIntent = PendingIntent.getService(this, 1, Intent(this, HexService::class.java).apply { action = CMD_TOGGLE_GEN }, PendingIntent.FLAG_IMMUTABLE)
        val pauseIntent = PendingIntent.getService(this, 2, Intent(this, HexService::class.java).apply { action = CMD_PAUSE }, PendingIntent.FLAG_IMMUTABLE)

        val statusText = when {
            isPaused -> "PAUSED"
            isGenerating -> "GENERATING"
            else -> "IDLE"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("HEX Generator: $statusText")
            .setContentText("Generated: $totalGenerated | Interval: ${interval}ms")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .addAction(android.R.drawable.ic_media_play, if(isGenerating) "Stop Gen" else "Start Gen", toggleIntent)
            .addAction(android.R.drawable.ic_media_pause, if(isPaused) "Resume" else "Pause", pauseIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Exit", stopIntent)
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
