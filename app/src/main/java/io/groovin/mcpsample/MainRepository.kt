package io.groovin.mcpsample

import android.content.SharedPreferences
import androidx.core.content.edit
import io.groovin.encrypthelper.EncryptHelper
import io.groovin.encrypthelper.KeyType
import io.groovin.mcpsample.McpServerItem.McpLocalServerItem
import io.groovin.mcpsample.McpServerItem.McpRemoteServerItem
import io.groovin.mcpsample.llm.LLMManager
import io.groovin.mcpsample.mcp.localserver.McpLocalServer
import io.groovin.mcpsample.mcp.McpClient
import io.groovin.mcpsample.mcp.McpLocalTransport
import io.groovin.mcpsample.util.logd
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.SSEClientException
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.InternalAPI
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class MainRepository(
    private val sharedPreferences: SharedPreferences,
    private val mcpLocalServer: McpLocalServer
) {
    companion object {
        private const val TAG = "MainRepository"
        private const val ENCRYPT_TOOL_KEY = "McpSampleEncryptor"
        private const val SELECTED_LLM_MODEL = "SELECTED_LLM_MODEL"
        private const val CLAUDE_API_KEY_PREF_KEY = "CLAUDE-API-KEY"
        private const val GEMINI_API_KEY_PREF_KEY = "GEMINI-API-KEY"
        private const val OPENAI_API_KEY_PREF_KEY = "OPENAI-API-KEY"
        private const val SYSTEM_PROMPT_PREF_KEY = "SYSTEM-PROMPT"
        private const val HOTWORD_ENABLED_PREF_KEY = "HOTWORD_ENABLED"
        private const val HOTWORD_TEXT_PREF_KEY = "HOTWORD_TEXT"
        private const val TEXT_TO_SPEECH_ENABLED = "TEXT_TO_SPEECH_ENABLED"
        private const val ASR_AUTO_RESULT_MODE_PREF_KEY = "ASR_AUTO_RESULT_MODE"
        private const val AUTO_ENABLE_MCP_CONNECTION = "AUTO_ENABLE_MCP_CONNECTION"
        private const val MCP_REMOTE_SERVER_LIST = "MCP_REMOTE_SERVER_LIST"
        private const val USER_MEMORY_PREF_KEY = "USER_MEMORY"
    }
    private val coroutineScope = CoroutineScope(SupervisorJob())
    private val encryptHelper = EncryptHelper(
        keyAlias = ENCRYPT_TOOL_KEY,
        keyType = KeyType.RSA_ECB_PKCS1_4096
    )

    fun getLlmModel(): LLMManager.LLMType {
        val selectedLlmModel = sharedPreferences.getString(SELECTED_LLM_MODEL, null)
        return selectedLlmModel?.let { LLMManager.LLMType.valueOf(it) } ?: LLMManager.LLMType.getDefault()
    }

    fun setLlmModel(llmType: LLMManager.LLMType) {
        sharedPreferences.edit {
            putString(SELECTED_LLM_MODEL, llmType.name)
        }
    }

    fun getClaudeApiKey(): String {
        val encryptedValue = sharedPreferences.getString(CLAUDE_API_KEY_PREF_KEY, "") ?: ""
        return if (encryptedValue.isBlank()) {
            return ""
        } else {
            encryptHelper.toDecrypt(encryptedValue)
        }
    }

    fun getGeminiApiKey(): String {
        val encryptedValue = sharedPreferences.getString(GEMINI_API_KEY_PREF_KEY, "") ?: ""
        return if (encryptedValue.isBlank()) {
            return ""
        } else {
            encryptHelper.toDecrypt(encryptedValue)
        }
    }

    fun getOpenAiApiKey(): String {
        val encryptedValue = sharedPreferences.getString(OPENAI_API_KEY_PREF_KEY, "") ?: ""
        return if (encryptedValue.isBlank()) {
            return ""
        } else {
            encryptHelper.toDecrypt(encryptedValue)
        }
    }

    fun getHotWordEnabled(): Boolean {
        return sharedPreferences.getBoolean(HOTWORD_ENABLED_PREF_KEY, false)
    }

    fun getHotWordText(): String {
        return sharedPreferences.getString(HOTWORD_TEXT_PREF_KEY, "") ?: ""
    }

    fun getTextToSpeechEnabled(): Boolean {
        return sharedPreferences.getBoolean(TEXT_TO_SPEECH_ENABLED, false)
    }

    fun getAsrAutoResultModeEnabled(): Boolean {
        return sharedPreferences.getBoolean(ASR_AUTO_RESULT_MODE_PREF_KEY, false)
    }

    fun getAutoEnableMcpConnection(): Boolean {
        return sharedPreferences.getBoolean(AUTO_ENABLE_MCP_CONNECTION, false)
    }

    fun getUserMemory(): String {
        return sharedPreferences.getString(USER_MEMORY_PREF_KEY, "") ?: ""
    }

    fun setUserMemory(memory: String) {
        sharedPreferences.edit {
            putString(USER_MEMORY_PREF_KEY, memory)
            commit()
        }
    }

    fun setClaudeApiKey(apiKey: String) {
        sharedPreferences.edit {
            putString(CLAUDE_API_KEY_PREF_KEY, encryptHelper.toEncrypt(apiKey))
            commit()
        }
    }

    fun setGeminiApiKey(apiKey: String) {
        sharedPreferences.edit {
            putString(GEMINI_API_KEY_PREF_KEY, encryptHelper.toEncrypt(apiKey))
            commit()
        }
    }

    fun setOpenAiApiKey(apiKey: String) {
        sharedPreferences.edit {
            putString(OPENAI_API_KEY_PREF_KEY, encryptHelper.toEncrypt(apiKey))
            commit()
        }
    }

    fun getSystemPrompt(): String {
        return sharedPreferences.getString(SYSTEM_PROMPT_PREF_KEY, "") ?: ""
    }

    fun setSystemPrompt(systemPrompt: String) {
        sharedPreferences.edit {
            putString(SYSTEM_PROMPT_PREF_KEY, systemPrompt)
            commit()
        }
    }

    fun setHotWordEnabled(isEnabled: Boolean) {
        sharedPreferences.edit {
            putBoolean(HOTWORD_ENABLED_PREF_KEY, isEnabled)
            commit()
        }
    }

    fun setHotWordText(hotWord: String) {
        sharedPreferences.edit {
            putString(HOTWORD_TEXT_PREF_KEY, hotWord)
            commit()
        }
    }
    fun setTextToSpeechEnabled(isEnabled: Boolean) {
        sharedPreferences.edit {
            putBoolean(TEXT_TO_SPEECH_ENABLED, isEnabled)
            commit()
        }
    }

    fun setAsrAutoResultModeEnabled(isEnabled: Boolean) {
        sharedPreferences.edit {
            putBoolean(ASR_AUTO_RESULT_MODE_PREF_KEY, isEnabled)
            commit()
        }
    }

    fun setAutoEnableMcpConnection(isEnabled: Boolean) {
        sharedPreferences.edit {
            putBoolean(AUTO_ENABLE_MCP_CONNECTION, isEnabled)
            commit()
        }
    }

    private val _mcpConnectionStateState = MutableStateFlow(McpConnectionState())
    val mcpConnectionState = _mcpConnectionStateState.asStateFlow()

    init {
        val list = mutableListOf<McpConnection>()
        // Local MCP
        list.add(McpConnection(serverItem = McpLocalServerItem("LocalMcp")))
        // Remote MCP
        list.addAll(
            loadMcpRemoteServerList().list.map {
                McpConnection(serverItem = McpRemoteServerItem(connectionName = it.connectionName, url = it.url, headers = it.headers))
            }
        )
        _mcpConnectionStateState.update { McpConnectionState(list = list.toImmutableList() ) }
    }

    suspend fun connect(connectionName: String) {
        val connection = mcpConnectionState.value.getConnection(connectionName) ?: return
        if (connection.mcpClient != null) return
        if (connection.connectionName == "LocalMcp") {
            logd(TAG, "connect() try to enableLocalMcp()")
            enableLocalMcp()
        } else {
            logd(TAG, "connect() try to enableRemoteMcp($connectionName)")
            enableRemoteMcp(connectionName)
        }
    }

    suspend fun disconnect(connectionName: String) {
        val connection = mcpConnectionState.value.getConnection(connectionName) ?: return
        if (connection.mcpClient == null) return

        if (connection.connectionName == "LocalMcp") {
            logd(TAG, "connect() try to disableLocalMcp()")
            disableLocalMcp()
        } else {
            logd(TAG, "connect() try to disableRemoteMcp($connectionName)")
            disableRemoteMcp(connectionName)
        }
    }

    suspend fun checkConnection() {
        mcpConnectionState.value.list.forEach { connection ->
            val autoEnable = getAutoEnableMcpConnection()
            val mcpClient = connection.mcpClient
            if (!autoEnable && mcpClient == null) return@forEach //AutoEnable false이고 mcpClient null이면 따로 체크하지 않는다
            val pingResult = mcpClient?.ping() == true
            logd(TAG, "checkConnection() ${connection.connectionName} : $pingResult")
            if (!pingResult) {
                logd(TAG, "checkConnection() ${connection.connectionName} : reconnect it")
                disconnect(connection.connectionName)
                connect(connection.connectionName)
            }
        }
    }

    private var localServerJob: Job? = null
    suspend fun enableLocalMcp() {
        val transport = McpLocalTransport()
        // Server Listen
        localServerJob = coroutineScope.launch(Dispatchers.IO) {
            logd(TAG, "enableLocalMcp(): LocalServer listen start")
            mcpLocalServer.listenServer(transport.getServerTransport())
            logd(TAG, "enableLocalMcp(): LocalServer listen finished")
        }
        // Client Connect
        delay(100L)
        logd(TAG, "enableLocalMcp(): create Mcp Local Client and try connect")
        val mcpLocalClient = McpClient("MCP Local Client", "1.0.0")
        mcpLocalClient.connect(transport.getClientTransport())
        logd(TAG, "enableLocalMcp(): Mcp Local Client connected")
        // Update connectionList
        updateConnectionList("LocalMcp", mcpLocalClient)
    }

    suspend fun disableLocalMcp() {
        val closeJob = coroutineScope.launch(Dispatchers.IO) {
            mcpConnectionState.value.getConnection("LocalMcp")?.mcpClient?.close()
        }
        delay(100L)
        localServerJob?.cancel()
        closeJob.cancel() //TODO : 강제 종료
        updateConnectionList("LocalMcp", null)
    }

    @OptIn(InternalAPI::class)
    suspend fun enableRemoteMcp(connectionName: String) {
        val connection = mcpConnectionState.value.getConnection(connectionName) ?: return
        if (connection.serverItem !is McpRemoteServerItem) return
        if (connection.serverItem.url.isBlank()) return
        val httpClient = HttpClient(CIO) {
            install(SSE)
            install(ContentNegotiation)
            defaultRequest {
                if (connection.serverItem.headers != null) {
                    for (header in connection.serverItem.headers) {
                        header(header.key, header.value)
                    }
                }
            }
        }
        val transport = SseClientTransport(httpClient, connection.serverItem.url)
        val mcpClient = McpClient("MCP Remote Client", "1.0.0")

        try {
            mcpClient.connect(transport)
            // Connection successful
        } catch (e: Exception) {
            // Handle connection error
            e.printStackTrace()
            if (e is SSEClientException) {
                logd(TAG, e.response?.toString() ?: "")
                val wwwAuthenticate = e.response?.headers?.get(HttpHeaders.WWWAuthenticate)
                logd(TAG, "WWW-Authenticate header: $wwwAuthenticate")
            }
            return //TODO
        }
        updateConnectionList(connectionName, mcpClient)
    }

    suspend fun disableRemoteMcp(connectionName: String) {
        val closeJob = coroutineScope.launch(Dispatchers.IO) {
            mcpConnectionState.value.getConnection(connectionName)?.mcpClient?.close()
        }
        delay(100L)
        closeJob.cancel() //TODO : 강제 종료
        updateConnectionList(connectionName, null)
    }

    fun addRemoteMcpConnection(item: McpRemoteServerItem) {
        val prevRemoteList = loadMcpRemoteServerList()
        val newRemoteList = prevRemoteList.copy(
            list = prevRemoteList.list.plus(item)
        )
        saveMcpRemoteServerList(newRemoteList)
        val serverItem = McpRemoteServerItem(item.connectionName, item.url, item.headers)
        addConnectionList(serverItem, null)
    }

    fun editRemoteMcpConnection(prevConnectionName: String, item: McpRemoteServerItem) {
        val prevRemoteList = loadMcpRemoteServerList()
        val newRemoteList = prevRemoteList.copy(
            list = prevRemoteList.list.map {
                if (it.connectionName == prevConnectionName) item else it
            }
        )
        saveMcpRemoteServerList(newRemoteList)
        val serverItem = McpRemoteServerItem(item.connectionName, item.url, item.headers)
        editConnectionList(prevConnectionName, serverItem)
    }

    fun removeRemoteMcpConnection(connectionName: String) {
        val prevRemoteList = loadMcpRemoteServerList()
        val removedList = prevRemoteList.list.toMutableList()
        removedList.removeIf { it.connectionName == connectionName }
        val newRemoteList = prevRemoteList.copy(list = removedList)
        saveMcpRemoteServerList(newRemoteList)
        removeConnectionList(connectionName)
    }

    private fun loadMcpRemoteServerList(): McpRemoteServerList {
        val jsonString = sharedPreferences.getString(MCP_REMOTE_SERVER_LIST, "{}") ?: "{}"
        logd(TAG, "loadMcpRemoteServerList() : $jsonString")
        return Json.decodeFromString<McpRemoteServerList>(jsonString)
    }

    private fun saveMcpRemoteServerList(remoteServerList: McpRemoteServerList) {
        val newJsonString = Json.encodeToString(remoteServerList)
        logd(TAG, "saveMcpRemoteServerList() : $newJsonString")
        sharedPreferences.edit {
            putString(MCP_REMOTE_SERVER_LIST, newJsonString)
            commit()
        }
    }

    private fun updateConnectionList(connectionName: String, mcpClient: McpClient?) {
        logd(TAG, "updateConnectionList(): $connectionName, mcpClient = ${mcpClient != null}")
        _mcpConnectionStateState.update {
            val newList = it.list.toMutableList()
            newList.replaceAll { item ->
                logd(TAG, "updateConnectionList(): item.connectionName = ${item.connectionName}, connectionName = $connectionName")
                if (item.connectionName == connectionName) {
                    logd(TAG, "updateConnectionList(): replaced")
                    item.copy(mcpClient = mcpClient)
                } else {
                    logd(TAG, "updateConnectionList(): reused")
                    item
                }
            }
            logd(TAG, "updateConnectionList(): $newList")
            McpConnectionState(list = newList.toImmutableList())
        }
        logd(TAG, "updateConnectionList(): done ")
    }

    private fun addConnectionList(serverItem: McpServerItem, mcpClient: McpClient?) {
        _mcpConnectionStateState.update {
            McpConnectionState(list = it.list.plus(McpConnection(serverItem, mcpClient)).toImmutableList())
        }
    }
    private fun editConnectionList(prevConnectionName: String, serverItem: McpServerItem) {
        _mcpConnectionStateState.update {
            McpConnectionState(list = it.list.map { item ->
                if (item.connectionName == prevConnectionName) {
                    item.copy(serverItem = serverItem)
                } else {
                    item
                }
            }.toImmutableList())
        }
    }
    private fun removeConnectionList(connectionName: String) {
        _mcpConnectionStateState.update {
            val removedList = it.list.toMutableList()
            removedList.removeIf { item -> item.connectionName == connectionName }
            McpConnectionState(list = removedList.toImmutableList())
        }
    }

}

data class McpConnectionState(
    val list: ImmutableList<McpConnection> = persistentListOf()
) {
    fun getConnection(connectionName: String): McpConnection? {
        return list.find { it.connectionName == connectionName }
    }
}

data class McpConnection(
    val serverItem: McpServerItem,
    val mcpClient: McpClient? = null,
) {
    val connectionName: String
        get() {
            return when (serverItem) {
                is McpLocalServerItem -> serverItem.connectionName
                is McpRemoteServerItem -> serverItem.connectionName
            }
        }
    val clientName: String?
        get() = mcpClient?.name
    val clientVersion: String?
        get() = mcpClient?.version
    val serverName: String?
        get() = mcpClient?.serverName
    val serverVersion: String?
        get() = mcpClient?.serverVersion
    val isConnected: Boolean
        get() = mcpClient != null
}

@Serializable
private data class McpRemoteServerList(
    val list: List<McpRemoteServerItem> = emptyList(),
)

@Serializable
sealed class McpServerItem {
    @Serializable
    class McpLocalServerItem(
        val connectionName: String = ""
    ): McpServerItem()

    @Serializable
    data class McpRemoteServerItem(
        val connectionName: String = "",
        val url: String = "",
        val headers: Map<String, String>? = null
    ): McpServerItem()
}
