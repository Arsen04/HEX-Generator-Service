package com.protelion.hexserver.data.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.protelion.hexserver.MainActivity
import com.protelion.hexserver.R
import com.protelion.hexserver.data.local.ServiceHexDao
import com.protelion.hexserver.data.local.entity.HexEntity
import com.protelion.hexserver.domain.usecase.GenerateHexUseCase
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject

class HexService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var generationJob: Job? = null
    
    private val hexDao: ServiceHexDao by inject()
    private val generateHexUseCase: GenerateHexUseCase by inject()
    
    private var currentStatus = "STOPPED"
    private var isGenerating = false
    private var isPaused = false
    private var currentInterval = 1000L

    companion object {
        private const val CHANNEL_ID = "HexServiceChannel"
        private const val NOTIFICATION_ID = 1
        const val CMD_START = "ACTION_START"
        const val CMD_STOP = "ACTION_STOP"
        const val CMD_TOGGLE_GEN = "ACTION_GENERATE"
        const val CMD_PAUSE = "ACTION_PAUSE"
        const val CMD_SET_INTERVAL = "ACTION_UPDATE_INTERVAL"
        const val CMD_RESTORE = "ACTION_REQUEST_HISTORY"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureForeground()
        val action = intent?.action?.removePrefix("com.protelion.hexservice.")
        when (action) {
            "ACTION_START" -> startService()
            "ACTION_STOP" -> stopService()
            "ACTION_GENERATE" -> toggleGeneration()
            "ACTION_PAUSE" -> togglePause()
            "ACTION_UPDATE_INTERVAL" -> {
                currentInterval = intent.getLongExtra("interval", 1000L)
                if (isGenerating && !isPaused) {
                    startGeneration()
                } else {
                    sendStatus()
                }
            }
            "ACTION_REQUEST_HISTORY" -> sendHistory()
            "ACTION_GET_STATUS" -> sendStatus()
        }
        return START_STICKY
    }

    private fun startService() {
        if (currentStatus == "STOPPED") {
            currentStatus = "IDLE"
            ensureForeground()
            sendStatus()
        }
    }

    private fun ensureForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID, createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
    }

    private fun stopService() {
        generationJob?.cancel()
        currentStatus = "STOPPED"
        isGenerating = false
        isPaused = false
        sendStatus()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun toggleGeneration() {
        if (isGenerating) {
            generationJob?.cancel()
            isGenerating = false
            isPaused = false
            currentStatus = "IDLE"
        } else {
            isGenerating = true
            isPaused = false
            currentStatus = "GENERATING"
            startGeneration()
        }
        updateNotification()
        sendStatus()
    }

    private fun startGeneration() {
        generationJob?.cancel()
        generationJob = serviceScope.launch {
            while (isActive) {
                if (!isPaused) {
                    val hexCode = generateHexUseCase()
                    val entity = HexEntity(value = hexCode, timestamp = System.currentTimeMillis())
                    hexDao.insertAndTrim(entity)
                    sendHexBroadcast(hexCode)
                }
                delay(currentInterval)
            }
        }
    }

    private fun togglePause() {
        if (isGenerating) {
            isPaused = !isPaused
            currentStatus = if (isPaused) "PAUSED" else "GENERATING"
            updateNotification()
            sendStatus()
        }
    }

    private fun sendStatus() {
        val intent = Intent("com.protelion.hexservice.STATUS_UPDATE").apply {
            putExtra("status", currentStatus)
            putExtra("isGenerating", isGenerating)
            putExtra("isPaused", isPaused)
            putExtra("interval", currentInterval)
            setPackage("com.protelion.hexclient")
        }
        sendBroadcast(intent)
    }

    private fun sendHexBroadcast(hex: String) {
        val intent = Intent("com.protelion.hexservice.NEW_HEX").apply {
            putExtra("hex", hex)
            setPackage("com.protelion.hexclient")
        }
        sendBroadcast(intent)
    }

    private fun sendHistory() {
        serviceScope.launch {
            val history = hexDao.getLast50()
            history.forEach {
                sendHexBroadcast(it.value)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Hex Generator Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Hex Generator Service")
            .setContentText("Status: $currentStatus")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        return builder.build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
