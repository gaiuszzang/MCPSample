package io.groovin.mcpsample.localserver

import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.shared.Transport
import kotlinx.coroutines.Job

class McpLocalServer(
    val name: String,
    val version: String,
    val contactTool: ContactTool,
    val phoneCallTool: PhoneCallTool
) {
    companion object {
        private const val TAG = "McpLocalServer"
    }

    suspend fun listenServer(transport: Transport) {
        val server = createServer()
        server.addLocalTools()
        server.apply {
            val done = Job()
            onCloseCallback = {
                done.complete()
            }
            connect(transport)
            done.join()
        }
    }

    private fun createServer(): Server {
        return Server(
            Implementation(name = name, version = version),
            ServerOptions(
                capabilities = ServerCapabilities(
                    prompts = ServerCapabilities.Prompts(listChanged = true),
                    resources = ServerCapabilities.Resources(subscribe = true, listChanged = true),
                    tools = ServerCapabilities.Tools(listChanged = true),
                )
            )
        )
    }

    private fun Server.addLocalTools() {
        addTool(contactTool)
        addTool(phoneCallTool)
    }
}