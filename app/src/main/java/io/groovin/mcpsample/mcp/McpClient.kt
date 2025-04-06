package io.groovin.mcpsample.mcp

import com.anthropic.core.JsonValue
import com.anthropic.models.messages.ToolUnion
import com.fasterxml.jackson.databind.ObjectMapper
import io.groovin.mcpsample.util.logd
import io.modelcontextprotocol.kotlin.sdk.CallToolResultBase
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.Prompt
import io.modelcontextprotocol.kotlin.sdk.Resource
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.shared.RequestOptions
import io.modelcontextprotocol.kotlin.sdk.shared.Transport
import kotlinx.serialization.json.JsonObject

class McpClient(
    val name: String,
    val version: String
) {
    private val mcp: Client = Client(clientInfo = Implementation(name = name, version = version))
    // List of prompts, tools, resources offered by the server
    lateinit var prompts: List<Prompt>
    lateinit var tools: List<ToolUnion>
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
            tools = toolsResult?.tools?.map { tool ->
                ToolUnion.ofTool(
                    com.anthropic.models.messages.Tool.builder()
                        .name(tool.name)
                        .description(tool.description ?: "")
                        .inputSchema(
                            com.anthropic.models.messages.Tool.InputSchema.builder()
                                .type(JsonValue.from(tool.inputSchema.type))
                                .properties(tool.inputSchema.properties.toJsonValue())
                                .putAdditionalProperty("required", JsonValue.from(tool.inputSchema.required))
                                .build()
                        )
                        .build()
                )
            } ?: emptyList()
            resources = try {
                mcp.listResources()?.resources ?: emptyList()
            } catch (e: Throwable) {
                e.printStackTrace()
                emptyList()
            }

            logd(TAG, "Connected")
            logd(TAG, "get tools: ${tools.joinToString(", ") { it.tool().get().name() }}")
            logd(TAG, "get Prompts: ${prompts.joinToString(", ") { it.name }}")
            logd(TAG, "get resources: ${resources.joinToString(", ") { "${it.name}:${it.uri}" }}")

        } catch (e: Throwable) {
            throw e
        }
    }

    suspend fun close() {
        mcp.close()
    }

    suspend fun callTool(
        name: String,
        arguments: Map<String, Any?>,
        compatibility: Boolean = false,
        options: RequestOptions? = null,
    ): CallToolResultBase? {
        return mcp.callTool(name, arguments, compatibility, options)
    }

    private fun JsonObject.toJsonValue(): JsonValue {
        val mapper = ObjectMapper()
        val node = mapper.readTree(this.toString())
        return JsonValue.fromJsonNode(node)
    }
}