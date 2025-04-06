package io.groovin.mcpsample.mcp.localserver

import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.shared.Transport
import kotlinx.coroutines.Job

class McpLocalServer(
    val name: String,
    val version: String,
    val tools: List<LocalTool>
) {
    companion object {
        private const val TAG = "McpLocalServer"
    }

    suspend fun listenServer(transport: Transport) {
        val server = Server(
            Implementation(name = name, version = version),
            ServerOptions(
                capabilities = ServerCapabilities(
                    prompts = ServerCapabilities.Prompts(listChanged = true),
                    resources = ServerCapabilities.Resources(subscribe = true, listChanged = true),
                    tools = ServerCapabilities.Tools(listChanged = true),
                )
            )
        )
        tools.forEach { tool ->
            server.addTool(tool)
        }
        server.apply {
            val done = Job()
            this.onClose {
                done.complete()
            }
            connect(transport)
            done.join()
        }
    }
}
