package io.groovin.mcpsample

import android.app.Application
import io.groovin.mcpsample.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class McpSampleApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@McpSampleApplication)
            modules(appModule)
        }
    }
}