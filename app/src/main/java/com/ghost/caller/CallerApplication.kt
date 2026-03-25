package com.ghost.caller

import android.app.Application
import com.ghost.caller.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class CallerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@CallerApplication)
            modules(appModule)
        }
    }
}