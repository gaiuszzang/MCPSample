package io.groovin.mcpsample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anthropic.models.messages.MessageParam
import io.groovin.mcpsample.llm.LLMManager
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MainViewModel(
    private val mainRepository: MainRepository,
    private val llmManager: LLMManager
): ViewModel() {
    companion object {
        private const val TAG = "MainViewModel"
    }

    val llmSelectUiState = llmManager.selectedLLMType.map { selectedLLMItem ->
        val llmItemList = LLMManager.LLMType.entries.map { llmItem ->
            LLMSelectUiState.LLMItem(
                llmType = llmItem,
                isSelected = (selectedLLMItem == llmItem)
            )
        }.toImmutableList()
        LLMSelectUiState(
            llmItemList = llmItemList,
            onItemSelectClick = { item ->
                llmManager.setLLM(item.llmType)
                mainRepository.setLlmModel(item.llmType)
            }
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = LLMSelectUiState(persistentListOf()))

        private val _chatUiState = MutableStateFlow(ChatUiState())
    val chatUiState = _chatUiState.asStateFlow()

    private val _settingUiState = MutableStateFlow(SettingUiState())
    val settingUiState = _settingUiState.asStateFlow()

    val mcpUiState = mainRepository.mcpConnectionState.map { mcpConnectionState ->
        McpUiState(
            mcpInfoList = mcpConnectionState.list.map { connection ->
                McpUiState.McpInfo(
                    connectionName = connection.connectionName,
                    url = connection.url,
                    serverName = connection.serverName,
                    version = connection.serverVersion,
                    isOn = connection.isConnected,
                    isRemovable = connection.connectionName != "LocalMcp",
                    supportFunctionDesc = connection.mcpClient?.tools?.joinToString { it.name },
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
            onAddMcpItem = { connectionName, url ->
                mainRepository.addRemoteMcpConnection(connectionName, url)
            },
            onEditMcpItem = { prevMcpItem, connectionName, url ->
                mainRepository.editRemoteMcpConnection(prevMcpItem.connectionName, connectionName, url)
            }
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = McpUiState())

    init {
        _settingUiState.update {
            SettingUiState(
                claudeApiKey = mainRepository.getClaudeApiKey(),
                geminiApiKey = mainRepository.getGeminiApiKey(),
                openAiApiKey = mainRepository.getOpenAiApiKey(),
                systemPrompt = mainRepository.getSystemPrompt(),
                onClaudeApiKeyUpdate = ::updateClaudeApiKey,
                onGeminiApiKeyUpdate = ::updateGeminiApiKey,
                onOpenAiApiKeyUpdate = ::updateOpenAiApiKey,
                onSystemPromptUpdate = ::updateSystemPrompt
            )
        }
        // LLMManager Init
        llmManager.setLLM(mainRepository.getLlmModel())
        llmManager.setApiKey(LLMManager.LLMService.Claude, mainRepository.getClaudeApiKey())
        llmManager.setApiKey(LLMManager.LLMService.Gemini, mainRepository.getGeminiApiKey())
        llmManager.setApiKey(LLMManager.LLMService.OpenAi, mainRepository.getOpenAiApiKey())
    }

    private var chatJob: Job? = null
    // Main chat loop for interacting with the user
    fun chatRequest(message: String) {
        if (chatJob != null) {
            addMessage(false, "Chat is not completed yet.")
            return
        }
        addMessage(true, message)
        viewModelScope.launch {
            //state change
            _chatUiState.update {
                it.copy(isOnHandleChatting = true)
            }
            chatJob = launch (Dispatchers.IO) {
                handleMessage(message)
                doneChatRequest()
            }
        }
    }

    fun cancelChatRequest() {
        chatJob?.cancel()
        doneChatRequest()
    }

    private fun doneChatRequest() {
        chatJob = null
        _chatUiState.update {
            it.copy(isOnHandleChatting = false)
        }
    }

    private suspend fun handleMessage(query: String) {
        val systemPrompt = settingUiState.value.systemPrompt.trim()
        try {
            llmManager.handleMessage(
                systemPrompt = systemPrompt,
                message = query,
                mcpTools = mainRepository.mcpConnectionState.value.list.flatMap { connection ->
                    connection.mcpClient?.tools ?: emptyList()
                },
                onResponse = { type, response ->
                    addMessage(false, response) //tODO USE TYPE
                },
                onToolCall = { toolName, arguments ->
                    val mcpClient =
                        mainRepository.mcpConnectionState.value.list.firstOrNull { connection ->
                            connection.mcpClient?.tools?.any { it.name == toolName } ?: false
                        }?.mcpClient
                    mcpClient?.callTool(name = toolName, arguments = arguments)
                }
            )
        } catch (e: Throwable) {
            e.printStackTrace()
            addMessage(false, "Error: ${e.message}")
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

    fun updateClaudeApiKey(apiKey: String) {
        _settingUiState.update { it.copy(claudeApiKey = apiKey) }
        mainRepository.setClaudeApiKey(apiKey)
        llmManager.setApiKey(LLMManager.LLMService.Claude, apiKey)
    }

    fun updateGeminiApiKey(apiKey: String) {
        _settingUiState.update { it.copy(geminiApiKey = apiKey) }
        mainRepository.setGeminiApiKey(apiKey)
        llmManager.setApiKey(LLMManager.LLMService.Gemini, apiKey)
    }

    fun updateOpenAiApiKey(apiKey: String) {
        _settingUiState.update { it.copy(openAiApiKey = apiKey) }
        mainRepository.setOpenAiApiKey(apiKey)
        llmManager.setApiKey(LLMManager.LLMService.OpenAi, apiKey)
    }

    fun updateSystemPrompt(systemPrompt: String) {
        _settingUiState.update {
            it.copy(systemPrompt = systemPrompt)
        }
        mainRepository.setSystemPrompt(systemPrompt)
    }
}

data class LLMSelectUiState(
    val llmItemList: ImmutableList<LLMItem>,
    val onItemSelectClick: (LLMItem) -> Unit = {}
) {
    data class LLMItem(
        val llmType: LLMManager.LLMType,
        val isSelected: Boolean = false,
    )
}

data class ChatUiState(
    val isLoading: Boolean = false,
    val isOnHandleChatting: Boolean = false,
    val messageList: ImmutableList<Message> = persistentListOf()
) {
    data class Message(
        val isUser: Boolean,
        val message: String
    )
}

data class SettingUiState(
    val claudeApiKey: String = "",
    val geminiApiKey: String = "",
    val openAiApiKey: String = "",
    val systemPrompt: String = "",
    val onClaudeApiKeyUpdate: (String) -> Unit = {},
    val onGeminiApiKeyUpdate: (String) -> Unit = {},
    val onOpenAiApiKeyUpdate: (String) -> Unit = {},
    val onSystemPromptUpdate: (String) -> Unit = {}
)

data class McpUiState(
    val mcpInfoList: ImmutableList<McpInfo> = persistentListOf(),
    val onAddMcpItem: (String, String) -> Unit = { _, _ -> },
    val onEditMcpItem: (McpInfo, String, String) -> Unit = { _, _, _ -> },
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
        val onRemoveClick: (() -> Unit) = {},
    )
}
