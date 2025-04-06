package io.groovin.mcpsample.di

import android.content.Context
import android.content.SharedPreferences
import io.groovin.mcpsample.MainRepository
import io.groovin.mcpsample.MainViewModel
import io.groovin.mcpsample.localserver.ContactTool
import io.groovin.mcpsample.localserver.McpLocalServer
import io.groovin.mcpsample.localserver.PhoneCallTool
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<McpLocalServer> {
        McpLocalServer(name = "Mcp Local Server", version = "1.0.0", contactTool = get(), phoneCallTool = get())
    }
    single<ContactTool> {
        ContactTool(get())
    }
    single<PhoneCallTool> {
        PhoneCallTool(get())
    }
    single<SharedPreferences> {
        androidApplication().getSharedPreferences("MCPSample", Context.MODE_PRIVATE)
    }
    single<MainRepository> {
        MainRepository(get(), get())
    }

    viewModel { MainViewModel(get()) }
}