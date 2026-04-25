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
import com.protelion.hexserver.domain.model.ServiceStatus
import com.protelion.ipc.IpcConstants
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject

class HexService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var generationJob: Job? = null
    
    private val hexDao: ServiceHexDao by inject()
    private val generateHexUseCase: GenerateHexUseCase by inject()
    
    private var currentStatus = ServiceStatus.STOPPED
    private var isGenerating = false
    private var isPaused = false
    private var currentInterval = 1000L
    private var totalGeneratedCount = 0

    companion object {
        private const val CHANNEL_ID = "HexServiceChannel"
        private const val NOTIFICATION_ID = 1
        
        // Internal Actions for Notification Buttons
        private const val ACTION_NOTIF_TOGGLE = "com.protelion.hexserver.NOTIF_TOGGLE"
        private const val ACTION_NOTIF_PAUSE = "com.protelion.hexserver.NOTIF_PAUSE"
        private const val ACTION_NOTIF_EXIT = "com.protelion.hexserver.NOTIF_EXIT"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val receiver = intent?.getParcelableExtra<android.os.ResultReceiver>(IpcConstants.EXTRA_RESULT_RECEIVER)

        when (action) {
            IpcConstants.ACTION_START -> startService()
            IpcConstants.ACTION_STOP -> stopService()
            IpcConstants.ACTION_TOGGLE_GEN, ACTION_NOTIF_TOGGLE -> {
                toggleGeneration()
                sendConfirmation(receiver, getString(R.string.msg_gen_toggled))
            }
            IpcConstants.ACTION_PAUSE, ACTION_NOTIF_PAUSE -> {
                togglePause()
                sendConfirmation(receiver, getString(R.string.msg_pause_toggled))
            }
            IpcConstants.ACTION_SET_INTERVAL -> {
                currentInterval = intent.getLongExtra(IpcConstants.EXTRA_INTERVAL, 1000L)
                sendStatus()
                sendConfirmation(receiver, getString(R.string.msg_interval_updated, currentInterval))
            }
            IpcConstants.ACTION_GET_HISTORY -> sendHistory()
            IpcConstants.ACTION_GET_STATUS -> sendStatus()
            ACTION_NOTIF_EXIT -> stopService()
            else -> ensureForeground()
        }
        return START_STICKY
    }

    private fun sendConfirmation(receiver: android.os.ResultReceiver?, message: String) {
        receiver?.send(0, android.os.Bundle().apply { putString(IpcConstants.EXTRA_MESSAGE, message) })
    }

    private fun startService() {
        if (currentStatus == ServiceStatus.STOPPED) {
            currentStatus = ServiceStatus.IDLE
            ensureForeground()
            sendStatus()
        }
    }

    private fun ensureForeground() {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopService() {
        generationJob?.cancel()
        currentStatus = ServiceStatus.STOPPED
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
            currentStatus = ServiceStatus.IDLE
        } else {
            isGenerating = true
            isPaused = false
            currentStatus = ServiceStatus.GENERATING
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
                    totalGeneratedCount++
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
            currentStatus = if (isPaused) ServiceStatus.PAUSED else ServiceStatus.GENERATING
            updateNotification()
            sendStatus()
        }
    }

    private fun sendStatus() {
        val intent = Intent(IpcConstants.BROADCAST_STATUS).apply {
            putExtra(IpcConstants.EXTRA_STATUS, currentStatus.name)
            putExtra(IpcConstants.EXTRA_IS_GENERATING, isGenerating)
            putExtra(IpcConstants.EXTRA_IS_PAUSED, isPaused)
            putExtra(IpcConstants.EXTRA_INTERVAL, currentInterval)
            putExtra(IpcConstants.EXTRA_TOTAL_GENERATED, totalGeneratedCount)
            setPackage(IpcConstants.CLIENT_PACKAGE)
        }
        sendBroadcast(intent)
    }

    private fun sendHexBroadcast(hex: String) {
        val intent = Intent(IpcConstants.BROADCAST_NEW_HEX).apply {
            putExtra(IpcConstants.EXTRA_HEX, hex)
            setPackage(IpcConstants.CLIENT_PACKAGE)
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
                getString(R.string.notification_channel_name),
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

        // Notification actions
        val toggleIntent = Intent(this, HexService::class.java).apply { action = ACTION_NOTIF_TOGGLE }
        val togglePending = PendingIntent.getService(this, 1, toggleIntent, PendingIntent.FLAG_IMMUTABLE)

        val pauseIntent = Intent(this, HexService::class.java).apply { action = ACTION_NOTIF_PAUSE }
        val pausePending = PendingIntent.getService(this, 2, pauseIntent, PendingIntent.FLAG_IMMUTABLE)

        val exitIntent = Intent(this, HexService::class.java).apply { action = ACTION_NOTIF_EXIT }
        val exitPending = PendingIntent.getService(this, 3, exitIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_status_format, currentStatus.name))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(
                android.R.drawable.ic_media_play,
                if (isGenerating) getString(R.string.notif_stop_gen) else getString(R.string.notif_start_gen),
                togglePending
            )
            .addAction(
                android.R.drawable.ic_media_pause,
                if (isPaused) getString(R.string.notif_resume) else getString(R.string.notif_pause),
                pausePending
            )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.notif_exit), exitPending)

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
