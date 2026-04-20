package com.protelion.hexserver

import android.app.Application
import com.protelion.hexserver.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class HexServerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@HexServerApp)
            modules(appModule)
        }
    }
}
