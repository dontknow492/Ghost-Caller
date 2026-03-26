package com.ghost.caller

import android.app.Application
import com.ghost.caller.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin
import timber.log.Timber

class CallerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Timber logging for the entire app
        Timber.plant(Timber.DebugTree())
        Timber.d("CallerApplication started and Timber initialized.")

        startKoin {
            androidContext(this@CallerApplication)
            modules(appModule)
        }
    }
}