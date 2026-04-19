package com.protelion.hexclient

import android.app.Application
import com.protelion.hexclient.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class HexApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@HexApp)
            modules(appModule)
        }
    }
}
