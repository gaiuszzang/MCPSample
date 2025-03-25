package io.groovin.mcpsample

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.MessageParam
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.ToolUnion
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.GetPromptResult
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.PromptArgument
import io.modelcontextprotocol.kotlin.sdk.PromptMessage
import io.modelcontextprotocol.kotlin.sdk.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.Role
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.TextResourceContents
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.PipedInputStream
import java.io.PipedOutputStream
import kotlin.jvm.optionals.getOrNull
import androidx.core.content.edit
import io.modelcontextprotocol.kotlin.sdk.GetPromptRequest
import io.modelcontextprotocol.kotlin.sdk.Prompt
import io.modelcontextprotocol.kotlin.sdk.ReadResourceRequest
import io.modelcontextprotocol.kotlin.sdk.Resource

class MainViewModel(
    private val sharedPreference: SharedPreferences
): ViewModel() {
    companion object {
        private const val TAG = "MCPSample"
        private const val API_KEY_PREF_KEY = "API-KEY"
    }

    private val _uiState = MutableStateFlow(UiState(
        apiKey = sharedPreference.getString(API_KEY_PREF_KEY, "") ?: ""
    ))
    val uiState = _uiState.asStateFlow()

    private val mcp: Client = Client(clientInfo = Implementation(name = "mcp-client-cli", version = "1.0.0"))

    private val anthropic by lazy {
        AnthropicOkHttpClient.builder()
            .apiKey(uiState.value.apiKey)
            .build()
    }

    private val messageParamsBuilder: MessageCreateParams.Builder = MessageCreateParams.builder()
        .model(Model.CLAUDE_3_5_SONNET_20241022)
        .maxTokens(1024)

    private fun configureServer(): Server {
        val def = CompletableDeferred<Unit>()

        val server = Server(
            Implementation(
                name = "mcp-personal assistant",
                version = "0.1.0"
            ),
            ServerOptions(
                capabilities = ServerCapabilities(
                    prompts = ServerCapabilities.Prompts(listChanged = true),
                    resources = ServerCapabilities.Resources(subscribe = true, listChanged = true),
                    tools = ServerCapabilities.Tools(listChanged = true),
                )
            ),
            onCloseCallback = {
                def.complete(Unit)
            }
        )
        server.addPrompt(
            name = "ask-contact-prompt",
            description = "Ask contact information",
            arguments = listOf(
                PromptArgument(
                    name = "name",
                    description = "Name who finds contact information",
                    required = true
                )
            )
        ) { request ->
            GetPromptResult(
                "Description for ${request.name}",
                messages = listOf(
                    PromptMessage(
                        role = Role.user,
                        content = TextContent("What are ${request.arguments?.get("name")}'s phone number and email address?")
                    )
                )
            )
        }

        // Add a tool
        server.addTool(
            name = "contact-tool",
            description = "Contact Information that contains phone number and email address",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("name") {
                        put("type", "string")
                    }
                },
                required = listOf("name")
            )
        ) { request ->
            val name = try {
                (request.arguments["name"]?.toString() ?: "unknown").lowercase()
            } catch (e: Throwable) {
                Log.d(TAG, "fail to parse name")
                "unknown"
            }
            return@addTool if (name.contains("fry")) {
                CallToolResult(
                    content = listOf(TextContent("$name Phone : 8210-111-2222, Mail : fry@gmail.com"))
                )
            } else if (name.contains("bender")) {
                CallToolResult(
                    content = listOf(TextContent("$name Phone : 010-0010-1101, Mail : bender@naver.com"))
                )
            } else if (name.contains("leela")) {
                CallToolResult(
                    content = listOf(TextContent("$name Phone : 8210-333-4444, Mail : leela@kakao.com"))
                )
            } else {
                CallToolResult(
                    content = listOf(TextContent("Cannot find contact information for $name"))
                )
            }
        }

        // Add a resource
        server.addResource(
            uri = "sample://code/hello-world",
            name = "Local Code Search",
            description = "Local code",
            mimeType = "text/plain"
        ) { request ->
            return@addResource ReadResourceResult(
                contents = listOf(
                    TextResourceContents(text = sampleCode, uri = request.uri, mimeType = "text/plain")
                )
            )
        }

        return server
    }

    private val bufferClientInput = PipedInputStream()
    private val bufferServerInput = PipedInputStream()
    private val bufferClientOutput = PipedOutputStream(bufferServerInput)
    private val bufferServerOutput = PipedOutputStream(bufferClientInput)

    private var server: Server? = null

    fun startServer() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                server = configureServer()
                server?.apply {
                    val done = Job()
                    onCloseCallback = {
                        done.complete()
                        addMessage("App: MCP Server closed")
                    }
                    addMessage("App: MCP Server start")
                    connect(
                        StdioServerTransport(
                            inputStream = bufferServerInput.asSource().buffered(),
                            outputStream = bufferServerOutput.asSink().buffered()
                        )
                    )
                    addMessage("App: MCP Server connect to transport")
                    done.join()
                }
            }
        }
    }

    fun stopServer() {
        viewModelScope.launch {
            server?.let {
                Log.d(TAG, "MCP Server stop")
                server?.close()
                server = null
            }
        }
    }

    // List of prompts, tools, resources offered by the server
    private lateinit var prompts: List<Prompt>
    private lateinit var tools: List<ToolUnion>
    private lateinit var resources: List<Resource>
    fun connectClient() {
        viewModelScope.launch {
            try {
                val transport = StdioClientTransport(
                    input = bufferClientInput.asSource().buffered(),
                    output = bufferClientOutput.asSink().buffered()
                )
                // Connect the MCP client to the server using the transport
                mcp.connect(transport)

                prompts = mcp.listPrompts()?.prompts ?: emptyList()
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

                resources = mcp.listResources()?.resources ?: emptyList()
                addMessage("App: MCP Client Connected")
                addMessage("App: MCP Client get tools: ${tools.joinToString(", ") { it.tool().get().name() }}")
                addMessage("App: MCP Client get Prompts: ${prompts.joinToString(", ") { it.name }}")
                addMessage("App: MCP Client get resources: ${resources.joinToString(", ") { "${it.name}:${it.uri}" }}")

            } catch (e: Throwable) {
                Log.e(TAG, e.toString())
                addMessage("App: Error ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun disconnectClient() {
        viewModelScope.launch {
            mcp.close()
        }
    }

    private fun JsonObject.toJsonValue(): JsonValue {
        val mapper = ObjectMapper()
        val node = mapper.readTree(this.toString())
        return JsonValue.fromJsonNode(node)
    }


    // Main chat loop for interacting with the user
    fun chatRequest(message: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                addMessage("User: $message")
                val response = processQuery(message)
                addMessage("AI: $response")
            }
        }
    }

    fun promptRequest(message: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val promptResponse = mcp.getPrompt(GetPromptRequest(
                    name = "ask-contact-prompt",
                    arguments = mapOf("name" to message)
                    )
                )
                val promptBuilder = StringBuilder()
                promptResponse?.messages?.forEach { message ->
                    when (message.content.type) {
                        TextContent.TYPE -> {
                            val textContent = message.content as? TextContent
                            textContent?.let {
                                promptBuilder.append(it.text)
                            }
                        }
                    }
                }
                val query = promptBuilder.toString()
                addMessage("User: $query")
                val response = processQuery(query)
                addMessage("AI: $response")
            }
        }
    }

    fun withResourceRequest(message: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                addMessage("User: $message")
                val readResourceResponse = mcp.readResource(ReadResourceRequest(uri = "sample://code/hello-world"))
                val resourceContentsBuilder = StringBuilder()
                readResourceResponse?.contents?.forEach { content ->
                    if (content is TextResourceContents) {
                        resourceContentsBuilder.append(content.text)
                    }
                }
                val resourceContents = resourceContentsBuilder.toString()
                addMessage("App: MCP Client get the resources:\n$resourceContents")
                val query = "$message\n\n<code>\n$resourceContents\n</code>"
                val response = processQuery(query)
                addMessage("AI: $response")
            }
        }
    }

    // Process a user query and return a string response
    suspend fun processQuery(query: String): String {
        // Create an initial message with a user's query
        val messages = mutableListOf(
            MessageParam.builder()
                .role(MessageParam.Role.USER)
                .content(query)
                .build()
        )

        // Send the query to the Anthropic model and get the response
        val response = anthropic.messages().create(
            messageParamsBuilder
                .messages(messages)
                .tools(tools)
                .build()
        )

        val finalText = mutableListOf<String>()
        response.content().forEach { content ->
            when {
                // Append text outputs from the response
                content.isText() -> finalText.add(content.text().getOrNull()?.text() ?: "")

                // If the response indicates a tool use, process it further
                content.isToolUse() -> {
                    val toolName = content.toolUse().get().name()
                    val toolArgs = content.toolUse().get()._input().convert(object : TypeReference<Map<String, JsonValue>>() {})
                    Log.d(TAG, "Tool use: $toolName with args $toolArgs")
                    // Call the tool with provided arguments
                    val result = mcp.callTool(
                        name = toolName,
                        arguments = toolArgs ?: emptyMap()
                    )
                    Log.d(TAG, "Tool use: CallTool result = ${result?.content}")
                    // Add the tool result message to the conversation
                    messages.add(
                        MessageParam.builder()
                            .role(MessageParam.Role.USER)
                            .content(
                                """
                                    "type": "tool_result",
                                    "tool_name": $toolName,
                                    "result": ${result?.content?.joinToString("\n") { (it as TextContent).text ?: "" }}
                                """.trimIndent()
                            )
                            .build()
                    )
                    Log.d(TAG, "Tool use: reask messages $messages")
                    // Retrieve an updated response after tool execution
                    val aiResponse = anthropic.messages().create(
                        messageParamsBuilder
                            .messages(messages)
                            .build()
                    )
                    Log.d(TAG, "Tool use result = $aiResponse")
                    // Append text outputs from the response
                    aiResponse.content().forEach { finalContent ->
                        when {
                            finalContent.isText() -> finalText.add(finalContent.text().getOrNull()?.text() ?: "")
                        }
                    }
                }
            }
        }

        return finalText.joinToString("\n", prefix = "", postfix = "")
    }

    private fun addMessage(message: String) {
        _uiState.update {
            val newMessageList = it.messageList.toMutableList()
            newMessageList.add(message)
            it.copy(
                messageList = newMessageList.toImmutableList()
            )
        }
    }

    fun updateApiKey(apiKey: String) {
        _uiState.update {
            it.copy(apiKey = apiKey)
        }
        sharedPreference.edit {
            putString(API_KEY_PREF_KEY, apiKey)
            commit()
        }
    }
}

data class UiState(
    val apiKey: String = "",
    val messageList: ImmutableList<String> = persistentListOf()
)


private const val sampleCode = """
fun main() {
    println("Hello, World!")
}
"""