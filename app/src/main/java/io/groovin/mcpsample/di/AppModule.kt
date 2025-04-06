package io.groovin.mcpsample.di

import android.content.Context
import android.content.SharedPreferences
import io.groovin.mcpsample.MainRepository
import io.groovin.mcpsample.MainViewModel
import io.groovin.mcpsample.llm.LLMManager
import io.groovin.mcpsample.mcp.localserver.contact.AddContactTool
import io.groovin.mcpsample.mcp.localserver.contact.GetContactDetailTool
import io.groovin.mcpsample.mcp.localserver.contact.GetContactListTool
import io.groovin.mcpsample.mcp.localserver.LocalToolPermissionHandler
import io.groovin.mcpsample.mcp.localserver.McpLocalServer
import io.groovin.mcpsample.mcp.localserver.notification.NotificationReadTool
import io.groovin.mcpsample.mcp.localserver.phonecall.PhoneCallTool
import io.groovin.mcpsample.mcp.localserver.contact.RemoveContactTool
import io.groovin.mcpsample.mcp.localserver.memory.DeleteMemoryTool
import io.groovin.mcpsample.mcp.localserver.memory.GetAllMemoryTool
import io.groovin.mcpsample.mcp.localserver.memory.SearchMemoryTool
import io.groovin.mcpsample.mcp.localserver.memory.AddMemoryTool
import io.groovin.mcpsample.mcp.localserver.memory.UpdateMemoryTool
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    /*
    single<CoroutineScope>(named("ApplicationScope")) {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
     */
    single<LocalToolPermissionHandler> {
        LocalToolPermissionHandler()
    }
    single<McpLocalServer> {
        McpLocalServer(
            name = "Mcp Local Server",
            version = "1.0.0",
            tools = listOf(
                GetContactListTool(get(), get()),
                GetContactDetailTool(get(), get()),
                AddContactTool(get(), get()),
                RemoveContactTool(get(), get()),
                PhoneCallTool(get(), get()),
                NotificationReadTool(get(), get()),
                GetAllMemoryTool(get(), get()),
                AddMemoryTool(get(), get()),
                SearchMemoryTool(get(), get()),
                DeleteMemoryTool(get(), get()),
                UpdateMemoryTool(get(), get()),
            )
        )
    }
    single<SharedPreferences> {
        androidApplication().getSharedPreferences("MCPSample", Context.MODE_PRIVATE)
    }
    single<MainRepository> {
        MainRepository(get(), get())
    }

    single<LLMManager> {
        LLMManager()
    }

    viewModel { MainViewModel(get(), get()) }
}
