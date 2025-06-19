package io.groovin.mcpsample.mcp

import io.groovin.mcpsample.util.logd
import io.modelcontextprotocol.kotlin.sdk.CallToolResultBase
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.Prompt
import io.modelcontextprotocol.kotlin.sdk.Resource
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.shared.RequestOptions
import io.modelcontextprotocol.kotlin.sdk.shared.Transport
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

class McpClient(
    val name: String,
    val version: String
) {
    private val mcp: Client = Client(clientInfo = Implementation(name = name, version = version))
    // List of prompts, tools, resources offered by the server
    lateinit var prompts: List<Prompt>
    lateinit var tools: List<Tool>
    lateinit var resources: List<Resource>

    private val TAG = "McpClient::$name"

    val serverName = mcp.serverVersion?.name
    val serverVersion = mcp.serverVersion?.version

    suspend fun connect(transport: Transport) {
        try {
            // Connect the MCP client to the server using the transport
            mcp.connect(transport)

            prompts = try {
                mcp.listPrompts()?.prompts ?: emptyList()
            } catch (e: Throwable) {
                e.printStackTrace()
                emptyList()
            }
            // Request the list of available tools from the server
            val toolsResult = mcp.listTools()
            tools = toolsResult?.tools ?: emptyList()
            resources = try {
                mcp.listResources()?.resources ?: emptyList()
            } catch (e: Throwable) {
                e.printStackTrace()
                emptyList()
            }

            logd(TAG, "Connected")
            logd(TAG, "get tools: ${tools.joinToString(", ") { it.name }}")
            logd(TAG, "get Prompts: ${prompts.joinToString(", ") { it.name }}")
            logd(TAG, "get resources: ${resources.joinToString(", ") { "${it.name}:${it.uri}" }}")

        } catch (e: Throwable) {
            throw e
        }
    }

    suspend fun close() {
        mcp.close()
    }

    suspend fun ping(): Boolean {
        return try {
            withTimeout(2.seconds) {
                mcp.ping()
            }
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    suspend fun callTool(
        name: String,
        arguments: Map<String, Any?>,
        compatibility: Boolean = false,
        options: RequestOptions? = null,
    ): CallToolResultBase? {
        return mcp.callTool(name, arguments, compatibility, options)
    }
}
