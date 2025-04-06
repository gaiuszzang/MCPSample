package io.groovin.mcpsample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.MessageParam
import com.anthropic.models.messages.Model
import com.fasterxml.jackson.core.type.TypeReference
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.jvm.optionals.getOrNull
import io.groovin.mcpsample.util.logd
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MainViewModel(
    private val mainRepository: MainRepository
): ViewModel() {
    companion object {
        private const val TAG = "MainViewModel"
    }

    private val _chatUiState = MutableStateFlow(ChatUiState(apiKey = ""))
    val chatUiState = _chatUiState.asStateFlow()

    val mcpUiState = mainRepository.mcpConnectionState.map { mcpConnectionState ->
        logd(TAG, "mcpUiState Updated")
        McpUiState(
            mcpInfoList = mcpConnectionState.list.map { connection ->
                logd(TAG, "mcpUiState Updated: $connection")
                McpUiState.McpInfo(
                    connectionName = connection.connectionName,
                    url = connection.url,
                    serverName = connection.serverName,
                    version = connection.serverVersion,
                    isOn = connection.isConnected,
                    isRemovable = connection.connectionName != "LocalMcp",
                    supportFunctionDesc = connection.mcpClient?.tools?.joinToString { it.tool().get().name() },
                    onToggleClick = {
                        viewModelScope.launch {
                            if (connection.isConnected) {
                                mainRepository.disconnect(connection.connectionName)
                            } else {
                                mainRepository.connect(connection.connectionName)
                            }
                        }
                    },
                    onRemoveClick = {
                        if (!connection.isConnected) {
                            mainRepository.removeRemoteMcpConnection(connection.connectionName)
                        }
                    }
                )
            }.toImmutableList(),
            onAddRequest = { connectionName, url ->
                mainRepository.addRemoteMcpConnection(connectionName, url)
            }
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = McpUiState())

    private val anthropic by lazy {
        AnthropicOkHttpClient.builder()
            .apiKey(chatUiState.value.apiKey)
            .build()
    }
    private val messageParamsBuilder: MessageCreateParams.Builder = MessageCreateParams.builder()
        .model(Model.CLAUDE_3_5_SONNET_20241022)
        .maxTokens(1024)

    init {
        _chatUiState.update {
            ChatUiState(apiKey = mainRepository.getApiKey())
        }
    }

    // Main chat loop for interacting with the user
    fun chatRequest(message: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                addMessage(true, message)
                handleMessage(message)
            }
        }
    }

    private suspend fun handleMessage(query: String, messages: MutableList<MessageParam> = mutableListOf()) {
        messages.add(
            MessageParam.builder()
                .role(MessageParam.Role.USER)
                .content(query)
                .build()
        )

        // Send the query to the Anthropic model and get the response
        val tools = mainRepository.mcpConnectionState.value.list.flatMap { connection ->
            connection.mcpClient?.tools ?: emptyList()
        }
        val response = anthropic.messages().create(
            messageParamsBuilder
                .messages(messages)
                .tools(tools)
                .build()
        )

        response.content().forEach { content ->
            when {
                content.isText() -> {
                    addMessage(isUser = false, content.text().getOrNull()?.text() ?: "")
                }
                content.isThinking() -> {
                    addMessage(false, content.thinking().get().thinking())
                }
                content.isRedactedThinking() -> {
                    addMessage(false, content.redactedThinking().get().data())
                }
                // If the response indicates a tool use, process it further
                content.isToolUse() -> {
                    val toolName = content.toolUse().get().name()
                    val toolArgs = content.toolUse().get()._input().convert(object : TypeReference<Map<String, JsonValue>>() {})
                    logd(TAG, "Tool use: $toolName with args $toolArgs")
                    // Call the tool with provided arguments
                    val mcpClient = mainRepository.mcpConnectionState.value.list.firstOrNull { connection ->
                        connection.mcpClient?.tools?.any { it.tool().get().name() == toolName } ?: false
                    }?.mcpClient
                    val result = mcpClient?.callTool(
                        name = toolName,
                        arguments = toolArgs ?: emptyMap()
                    )
                    logd(TAG, "Tool use: CallTool result = ${result?.content}")
                    // Add the tool result message to the conversation
                    handleMessage(
                        query = """
                                    "type": "tool_result",
                                    "tool_name": $toolName,
                                    "result": ${result?.content?.joinToString("\n") { (it as TextContent).text ?: "" }}
                                """.trimIndent(),
                        messages = messages
                    )
                }
            }
        }
    }

    private fun addMessage(isUser: Boolean, message: String) {
        _chatUiState.update {
            val newMessageList = it.messageList.toMutableList()
            newMessageList.add(ChatUiState.Message(isUser, message))
            it.copy(
                messageList = newMessageList.toImmutableList()
            )
        }
    }

    fun updateApiKey(apiKey: String) {
        _chatUiState.update {
            it.copy(apiKey = apiKey)
        }
        mainRepository.setApiKey(apiKey)
    }
}

data class ChatUiState(
    val isLoading: Boolean = false,
    val apiKey: String = "",
    val messageList: ImmutableList<Message> = persistentListOf()
) {
    data class Message(
        val isUser: Boolean,
        val message: String
    )
}

data class McpUiState(
    val mcpInfoList: ImmutableList<McpInfo> = persistentListOf(),
    val onAddRequest: (String, String) -> Unit = { _, _ -> }
) {
    data class McpInfo(
        val connectionName: String = "",
        val serverName: String? = null,
        val version: String? = null,
        val supportFunctionDesc: String? = null,
        val isOn: Boolean = false,
        val url: String? = null,
        val isRemovable: Boolean = true,
        val onToggleClick: (() -> Unit) = {},
        val onRemoveClick: (() -> Unit) = {}, //TODO m.c.shin
    )
}