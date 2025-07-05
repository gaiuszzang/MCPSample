package io.groovin.mcpsample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.groovin.mcpsample.ChatUiState.Message.Type.Companion.toMessageType
import io.groovin.mcpsample.McpServerItem.McpRemoteServerItem
import io.groovin.mcpsample.llm.LLMManager
import io.groovin.mcpsample.llm.ResponseType
import io.groovin.mcpsample.util.logd
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withTimeout
import org.koin.core.qualifier._q
import java.util.UUID
import java.lang.StringBuilder
import kotlin.time.Duration.Companion.seconds

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
                    url = if (connection.serverItem is McpRemoteServerItem) connection.serverItem.url else null,
                    headers = if (connection.serverItem is McpRemoteServerItem) connection.serverItem.headers else null,
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
            onAddMcpItem = { item ->
                logd(TAG, "onAddMcpItem : $item")
                mainRepository.addRemoteMcpConnection(item)
            },
            onEditMcpItem = { prevMcpItem, item ->
                mainRepository.editRemoteMcpConnection(prevMcpItem.connectionName, item)
            }
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = McpUiState())

    private val _ttsEvent = MutableSharedFlow<String>()
    val ttsEvent = _ttsEvent.asSharedFlow()

    init {
        _settingUiState.update {
            SettingUiState(
                claudeApiKey = mainRepository.getClaudeApiKey(),
                geminiApiKey = mainRepository.getGeminiApiKey(),
                openAiApiKey = mainRepository.getOpenAiApiKey(),
                systemPrompt = mainRepository.getSystemPrompt(),
                memoryContext = mainRepository.getUserMemory(),
                memoryEnabled = mainRepository.getUserMemoryEnabled(),
                hotWordEnabled = mainRepository.getHotWordEnabled(),
                hotWordText = mainRepository.getHotWordText(),
                textToSpeechEnabled = mainRepository.getTextToSpeechEnabled(),
                asrAutoResultModeEnabled = mainRepository.getAsrAutoResultModeEnabled(),
                autoEnableMcpConnection = mainRepository.getAutoEnableMcpConnection(),
                onClaudeApiKeyUpdate = ::updateClaudeApiKey,
                onGeminiApiKeyUpdate = ::updateGeminiApiKey,
                onOpenAiApiKeyUpdate = ::updateOpenAiApiKey,
                onSystemPromptUpdate = ::updateSystemPrompt,
                onMemoryContextUpdate = ::updateMemoryContext,
                onMemoryEnabled = ::updateMemoryEnabled,
                onHotWordEnabled = ::updateHotWordEnabled,
                onHotWordUpdate = ::updateHotWordText,
                onTextToSpeechEnabled = ::updateTextToSpeechEnabled,
                onAsrAutoResultModeEnabled = ::updateAsrAutoResultModeEnabled,
                onAutoEnableMcpConnection = :: updateAutoEnableMcpConnection
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
            addMessage(ChatUiState.Message.Type.System, "Chat is not completed yet.")
            return
        }
        addMessage(ChatUiState.Message.Type.User, message)
        viewModelScope.launch {
            //state change
            _chatUiState.update {
                it.copy(isOnHandleChatting = true)
            }
            chatJob = launch (Dispatchers.IO) {
                handleMessage(getRequestMessages(100)) //TODO Multi-turn size
                doneChatRequest()
            }
        }
    }

    private fun getRequestMessages(maxSize: Int = 1): List<LLMManager.MessageItem> {
        val messages = mutableListOf<LLMManager.MessageItem>()

        for (item in chatUiState.value.messageList.reversed()) {
            if (item is ChatUiState.Message.User) {
                messages.add(LLMManager.MessageItem(LLMManager.MessageItem.Type.User, item.message))
            } else if (item is ChatUiState.Message.Agent) {
                messages.add(LLMManager.MessageItem(LLMManager.MessageItem.Type.Assistant, item.message))
            }/* else if (item is ChatUiState.Message.Tool) {
                messages.add(LLMManager.MessageItem(LLMManager.MessageItem.Type.Tool, item.message))
            }*/ //TODO Tool Item 을 Multi-turn 에 넣을지 검토
            if (messages.size >= maxSize) break
        }
        return messages.reversed()
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

    private suspend fun handleMessage(messages: List<LLMManager.MessageItem>) {
        val userPrompt = messages.lastOrNull { it.type == LLMManager.MessageItem.Type.User }?.message ?: ""
        val relatedMemory = getUserMemoryRelatedOnUserPrompt(userPrompt)
        val basePrompt = settingUiState.value.systemPrompt.trim()
        val systemPrompt = buildString {
            append(basePrompt)
            if (relatedMemory.isNotBlank()) {
                append("\nrelatedMemory:$relatedMemory\n")
            }
        }
        try {
            llmManager.handleMessage(
                systemPrompt = systemPrompt,
                messages = messages,
                mcpTools = mainRepository.mcpConnectionState.value.list.flatMap { connection ->
                    connection.mcpClient?.tools ?: emptyList()
                },
                onResponse = { llmResponseType, response ->
                    addMessage(llmResponseType.toMessageType(), response)
                },
                onToolCall = { toolName, arguments ->
                    val mcpClient = mainRepository.mcpConnectionState.value.list.firstOrNull { connection ->
                        connection.mcpClient?.tools?.any { it.name == toolName } == true
                    }?.mcpClient
                    if (mcpClient != null) {
                        val toolMessageId = createMessageId()
                        addMessage(ChatUiState.Message.Type.Tool, "(Call Tool)\ntoolName:$toolName\nargument:$arguments", id = toolMessageId)
                        val result = try {
                            withTimeout(20.seconds) {
                                mcpClient.callTool(name = toolName, arguments = arguments)
                            }
                        } catch(e: TimeoutCancellationException) {
                            e.printStackTrace()
                            addMessage(ChatUiState.Message.Type.System, "Error: ${e.message}")
                            null
                        }
                        val isError = result?.isError != false
                        val resultMessage = result?.content?.joinToString() ?: "null"
                        updateMessage(id = toolMessageId, message = "\n\n(Tool Response)\nisError:$isError\nresponse:$resultMessage", appending = true)
                        result
                    } else {
                        addMessage(ChatUiState.Message.Type.Tool, "(Cannot find Tool)")
                        null
                    }
                }
            )
        } catch (e: Throwable) {
            e.printStackTrace()
            addMessage(ChatUiState.Message.Type.System, "Error: ${e.message}")
        }
    }

    private suspend fun getUserMemoryRelatedOnUserPrompt(userPrompt: String): String {
        if (!mainRepository.getUserMemoryEnabled()) return ""

        var userMemory = mainRepository.getUserMemory()
        val memoryMessageId = createMessageId()
        if (userPrompt.isNotBlank()) {
            addMessage(ChatUiState.Message.Type.Tool, "(Memory)\nrequest to update long term memory\ncurrent memory:$userMemory", id = memoryMessageId)
            val updatedMemory = runMemoryTask(
                systemPrompt = "Following is my prompt and current memory contents.\n" +
                        "For the given prompt, if there is a person's name, personal information, " +
                        "contact information, calendar, specific event, or something explicitly requested to be remembered, " +
                        "extract only that part and return it with the existing memory contents added or updated.\n" +
                        "In other words, the existing memory contents should be included along with the new contents.\n" +
                        "If possible, unnecessary contents do not need to be remembered additionally. " +
                        "If there is nothing to update, return an empty string. " +
                        "This is a system request, so answer as requested and do not ask again.",
                        //TODO : 다른 tool call에서 처리할 수 있는 것은 기억하지 말자. 최대한 중요한 사항만, 장기적으로 꼭 기억해야 할 사항만 판단해서 기억하자.
                userPrompt = userPrompt,
                memory = userMemory
            ).trim()
            if (updatedMemory.isNotEmpty()) {
                userMemory = updatedMemory
                mainRepository.setUserMemory(userMemory)
                updateMessage(memoryMessageId, "(Memory)\nlong term memory updated\nupdated memory:$userMemory", appending = true)
            } else {
                updateMessage(memoryMessageId, "(Memory)\nlong term memory not updated", appending = true)
            }
        }

        val relatedMemory = if (userPrompt.isNotBlank()) {
            updateMessage(memoryMessageId, "(Memory)\nget the relevant part from memory for given prompt", appending = true)
            val relatedMemory = runMemoryTask(
                systemPrompt = "Following is my prompt and current memory contents. " +
                        "If there is anything worth referencing in the prompt passed from memory, extract only that part separately. " +
                        "If there is no anything worth referencing, return an empty string. " +
                        "This request is a system request, so just answer as requested and do not ask me again.",
                userPrompt = userPrompt,
                memory = userMemory
            ).trim()
            updateMessage(memoryMessageId, "(Memory)\nget relevant part of memory:\n$relatedMemory", appending = true)
            relatedMemory
        } else ""
        return relatedMemory
    }

    private fun addMessage(messageType: ChatUiState.Message.Type, message: String, id: String = createMessageId()) {
        _chatUiState.update {
            val newMessageList = it.messageList.toMutableList()
            val newMessage = when (messageType) {
                ChatUiState.Message.Type.User -> ChatUiState.Message.User(id = id, message = message)
                ChatUiState.Message.Type.Agent -> {
                    textToSpeech(message) //TODO : TTS 보내는 위치 재고려 필요
                    ChatUiState.Message.Agent(id = id, message = message)
                }
                ChatUiState.Message.Type.System -> ChatUiState.Message.System(id = id, message = message)
                ChatUiState.Message.Type.Thinking -> ChatUiState.Message.Thinking(id = id, message = message)
                ChatUiState.Message.Type.Tool -> ChatUiState.Message.Tool(id = id, message = message, isExpand = false, onClickExpand = { toggleToolMessageExpand(id) })
            }
            newMessageList.add(newMessage)
            it.copy(
                messageList = newMessageList.toImmutableList()
            )
        }
    }

    private fun updateMessage(id: String, message: String, appending: Boolean) {
        _chatUiState.update {
            val newMessageList = it.messageList.toMutableList()
            newMessageList.replaceAll { messageItem ->
                if (messageItem.id == id) {
                    val newMessage = if (appending) messageItem.message + message else message
                    when (messageItem) {
                        is ChatUiState.Message.User -> messageItem.copy(message = newMessage)
                        is ChatUiState.Message.Agent -> messageItem.copy(message = newMessage)
                        is ChatUiState.Message.System -> messageItem.copy(message = newMessage)
                        is ChatUiState.Message.Thinking -> messageItem.copy(message = newMessage)
                        is ChatUiState.Message.Tool -> messageItem.copy(message = newMessage)
                    }
                } else {
                    messageItem
                }
            }
            it.copy(
                messageList = newMessageList.toImmutableList()
            )
        }
    }

    private fun toggleToolMessageExpand(id: String) {
        _chatUiState.update {
            val newMessageList = it.messageList.toMutableList()
            newMessageList.replaceAll { messageItem ->
                if (messageItem.id == id && messageItem is ChatUiState.Message.Tool) {
                    messageItem.copy(isExpand = !messageItem.isExpand)
                } else {
                    messageItem
                }
            }
            it.copy(
                messageList = newMessageList.toImmutableList()
            )
        }
    }

    private suspend fun runMemoryTask(systemPrompt: String, userPrompt: String, memory: String): String {
        val result = StringBuilder()
        llmManager.handleMessage(
            systemPrompt = systemPrompt,
            messages = listOf(
                LLMManager.MessageItem(
                    LLMManager.MessageItem.Type.User,
                    "prompt:\n$userPrompt\nmemory:\n$memory"
                )
            ),
            mcpTools = emptyList(),
            onResponse = { _, response -> result.append(response) },
            onToolCall = { _, _ -> null }
        )
        return result.toString()
    }

    private fun createMessageId(): String {
        return UUID.randomUUID().toString()
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

    fun updateMemoryContext(memoryContext: String) {
        _settingUiState.update {
            it.copy(memoryContext = memoryContext)
        }
        mainRepository.setUserMemory(memoryContext)
    }

    fun updateMemoryEnabled(isEnabled: Boolean) {
        _settingUiState.update {
            it.copy(memoryEnabled = isEnabled)
        }
        mainRepository.setUserMemoryEnabled(isEnabled)
    }

    fun updateHotWordEnabled(isEnabled: Boolean) {
        _settingUiState.update {
            it.copy(hotWordEnabled = isEnabled)
        }
        mainRepository.setHotWordEnabled(isEnabled)
    }

    fun updateHotWordText(hotWord: String) {
        _settingUiState.update {
            it.copy(hotWordText = hotWord)
        }
        mainRepository.setHotWordText(hotWord)
    }

    fun updateTextToSpeechEnabled(isEnabled: Boolean) {
        _settingUiState.update {
            it.copy(textToSpeechEnabled = isEnabled)
        }
        mainRepository.setTextToSpeechEnabled(isEnabled)
    }

    fun updateAsrAutoResultModeEnabled(isEnabled: Boolean) {
        _settingUiState.update {
            it.copy(asrAutoResultModeEnabled = isEnabled)
        }
        mainRepository.setAsrAutoResultModeEnabled(isEnabled)
    }

    fun updateAutoEnableMcpConnection(isEnabled: Boolean) {
        _settingUiState.update {
            it.copy(autoEnableMcpConnection = isEnabled)
        }
        mainRepository.setAutoEnableMcpConnection(isEnabled)
    }


    fun checkConnection() {
        viewModelScope.launch {
            mainRepository.checkConnection()
        }
    }

    private fun textToSpeech(text: String) {
        if (mainRepository.getTextToSpeechEnabled()) {
            viewModelScope.launch {
                _ttsEvent.emit(text)
            }
        }
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
    val isOnHandleChatting: Boolean = false,
    val messageList: ImmutableList<Message> = persistentListOf()
) {
    sealed class Message(
        open val id: String,
        open val message: String
    ) {
        data class User(
            override val id: String,
            override val message: String
        ): Message(id, message)
        data class Agent(
            override val id: String,
            override val message: String
        ): Message(id, message)
        data class System(
            override val id: String,
            override val message: String
        ): Message(id, message)
        data class Tool(
            override val id: String,
            override val message: String,
            val isExpand: Boolean = false,
            val onClickExpand: () -> Unit = {}
        ): Message(id, message)
        data class Thinking(
            override val id: String,
            override val message: String
        ): Message(id, message)

        enum class Type {
            User, Agent, System, Tool, Thinking;

            companion object {
                fun ResponseType.toMessageType(): Type {
                    return when (this) {
                        ResponseType.Text -> Agent
                        ResponseType.System -> System
                        ResponseType.Tool -> Tool
                        ResponseType.Thinking -> Thinking
                    }
                }
            }
        }
    }
}

data class SettingUiState(
    val claudeApiKey: String = "",
    val geminiApiKey: String = "",
    val openAiApiKey: String = "",
    val systemPrompt: String = "",
    val memoryContext: String = "",
    val onClaudeApiKeyUpdate: (String) -> Unit = {},
    val onGeminiApiKeyUpdate: (String) -> Unit = {},
    val onOpenAiApiKeyUpdate: (String) -> Unit = {},
    val onSystemPromptUpdate: (String) -> Unit = {},
    val onMemoryContextUpdate: (String) -> Unit = {},
    val memoryEnabled: Boolean = false,
    val onMemoryEnabled: (Boolean) -> Unit = {},
    val hotWordEnabled: Boolean = false,
    val hotWordText: String = "",
    val onHotWordEnabled: (Boolean) -> Unit = {},
    val onHotWordUpdate: (String) -> Unit = {},
    val textToSpeechEnabled: Boolean = false,
    val onTextToSpeechEnabled: (Boolean) -> Unit = {},
    val asrAutoResultModeEnabled: Boolean = false,
    val onAsrAutoResultModeEnabled: (Boolean) -> Unit = {},
    val autoEnableMcpConnection: Boolean = false,
    val onAutoEnableMcpConnection: (Boolean) -> Unit = {}
)

data class McpUiState(
    val mcpInfoList: ImmutableList<McpInfo> = persistentListOf(),
    val onAddMcpItem: (McpRemoteServerItem) -> Unit = { _ -> },
    val onEditMcpItem: (McpInfo, McpRemoteServerItem) -> Unit = { _, _ -> },
) {
    data class McpInfo(
        val connectionName: String = "",
        val serverName: String? = null,
        val version: String? = null,
        val supportFunctionDesc: String? = null,
        val isOn: Boolean = false,
        val url: String? = null,
        val headers: Map<String, String>? = null,
        val isRemovable: Boolean = true,
        val onToggleClick: (() -> Unit) = {},
        val onRemoveClick: (() -> Unit) = {},
    )
}
