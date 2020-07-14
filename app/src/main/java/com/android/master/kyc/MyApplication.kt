package com.android.master.kyc

import android.app.Application
import com.android.master.kyc.extension.apiModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // start Koin!
        startKoin {
            // declare used Android context
            androidContext(this@MyApplication)
            // declare modules
            modules(listOf(apiModule))
        }
    }
}