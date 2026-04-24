package com.protelion.ipc

object IpcConstants {
    // Actions for Client -> Service
    const val ACTION_START = "com.protelion.hexserver.ACTION_START"
    const val ACTION_STOP = "com.protelion.hexserver.ACTION_STOP"
    const val ACTION_TOGGLE_GEN = "com.protelion.hexserver.ACTION_TOGGLE_GEN"
    const val ACTION_PAUSE = "com.protelion.hexserver.ACTION_PAUSE"
    const val ACTION_SET_INTERVAL = "com.protelion.hexserver.ACTION_SET_INTERVAL"
    const val ACTION_GET_HISTORY = "com.protelion.hexserver.ACTION_GET_HISTORY"
    const val ACTION_GET_STATUS = "com.protelion.hexserver.ACTION_GET_STATUS"

    // Broadcast Actions
    const val BROADCAST_STATUS = "com.protelion.hexserver.STATUS_UPDATE"
    const val BROADCAST_NEW_HEX = "com.protelion.hexserver.NEW_HEX"

    // Extra Keys
    const val EXTRA_INTERVAL = "extra_interval"
    const val EXTRA_HEX = "extra_hex"
    const val EXTRA_STATUS = "extra_status"
    const val EXTRA_IS_GENERATING = "extra_is_generating"
    const val EXTRA_IS_PAUSED = "extra_is_paused"
    const val EXTRA_TOTAL_GENERATED = "extra_total_generated"
    const val EXTRA_RESULT_RECEIVER = "extra_receiver"
    const val EXTRA_MESSAGE = "extra_message"
    
    // Package Names
    const val CLIENT_PACKAGE = "com.protelion.hexclient"
    const val SERVER_PACKAGE = "com.protelion.hexserver"
    const val SERVER_SERVICE_CLASS = "com.protelion.hexserver.data.service.HexService"
}
