package io.groovin.mcpsample

import android.app.Application
import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import io.groovin.mcpsample.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class McpSampleApplication: Application() {

    override fun onCreate() {
        setupComposeFlags()
        super.onCreate()
        startKoin {
            androidContext(this@McpSampleApplication)
            modules(appModule)
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun setupComposeFlags() {
        ComposeFoundationFlags.DragGesturePickUpEnabled = false
    }
}
