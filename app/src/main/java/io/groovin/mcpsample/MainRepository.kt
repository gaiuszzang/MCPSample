package io.groovin.mcpsample

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import io.groovin.mcpsample.llm.LLMManager
import io.groovin.mcpsample.mcp.localserver.McpLocalServer
import io.groovin.mcpsample.mcp.McpClient
import io.groovin.mcpsample.mcp.McpLocalTransport
import io.groovin.mcpsample.util.logd
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MainRepository(
    private val sharedPreferences: SharedPreferences,
    private val mcpLocalServer: McpLocalServer
) {
    companion object {
        private const val TAG = "MainRepository"
        private const val SELECTED_LLM_MODEL = "SELECTED_LLM_MODEL"
        private const val CLAUDE_API_KEY_PREF_KEY = "CLAUDE-API-KEY"
        private const val GEMINI_API_KEY_PREF_KEY = "GEMINI-API-KEY"
        private const val OPENAI_API_KEY_PREF_KEY = "OPENAI-API-KEY"
        private const val SYSTEM_PROMPT_PREF_KEY = "SYSTEM-PROMPT"
        private const val MCP_REMOTE_SERVER_LIST = "MCP_REMOTE_SERVER_LIST"
    }
    private val coroutineScope = CoroutineScope(SupervisorJob())

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
        return sharedPreferences.getString(CLAUDE_API_KEY_PREF_KEY, "") ?: ""
    }

    fun getGeminiApiKey(): String {
        return sharedPreferences.getString(GEMINI_API_KEY_PREF_KEY, "") ?: ""
    }

    fun getOpenAiApiKey(): String {
        return sharedPreferences.getString(OPENAI_API_KEY_PREF_KEY, "") ?: ""
    }

    fun setClaudeApiKey(apiKey: String) {
        sharedPreferences.edit {
            putString(CLAUDE_API_KEY_PREF_KEY, apiKey)
            commit()
        }
    }

    fun setGeminiApiKey(apiKey: String) {
        sharedPreferences.edit {
            putString(GEMINI_API_KEY_PREF_KEY, apiKey)
            commit()
        }
    }

    fun setOpenAiApiKey(apiKey: String) {
        sharedPreferences.edit {
            putString(OPENAI_API_KEY_PREF_KEY, apiKey)
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

    private val _mcpConnectionStateState = MutableStateFlow(McpConnectionState())
    val mcpConnectionState = _mcpConnectionStateState.asStateFlow()

    init {
        val list = mutableListOf<McpConnection>()
        // Local MCP
        list.add(McpConnection(connectionName = "LocalMcp"))
        // Remote MCP
        list.addAll(
            loadMcpRemoteServerList().list.map { McpConnection(connectionName = it.connectionName, url = it.url) }
        )
        _mcpConnectionStateState.update { McpConnectionState(list = list.toImmutableList() ) }
    }

    suspend fun connect(connectionName: String) {
        val connection = mcpConnectionState.value.getConnection(connectionName) ?: return
        if (connection.mcpClient != null) return
        if (connection.connectionName == "LocalMcp") {
            Log.d(TAG, "connect() try to enableLocalMcp()")
            enableLocalMcp()
        } else {
            Log.d(TAG, "connect() try to enableRemoteMcp($connectionName)")
            enableRemoteMcp(connectionName)
        }
    }



    suspend fun disconnect(connectionName: String) {
        val connection = mcpConnectionState.value.getConnection(connectionName) ?: return
        if (connection.mcpClient == null) return

        if (connection.connectionName == "LocalMcp") {
            Log.d(TAG, "connect() try to disableLocalMcp()")
            disableLocalMcp()
        } else {
            Log.d(TAG, "connect() try to disableRemoteMcp($connectionName)")
            disableRemoteMcp(connectionName)
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
        delay(500L)
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
        delay(500)
        localServerJob?.cancel()
        closeJob.cancel() //TODO : 강제 종료
        updateConnectionList("LocalMcp", null)
    }

    suspend fun enableRemoteMcp(connectionName: String) {
        val connection = mcpConnectionState.value.getConnection(connectionName) ?: return
        if (connection.url == null) return
        val httpClient = HttpClient(CIO) {
            install(SSE)
            install(ContentNegotiation)
        }
        val transport = SseClientTransport(httpClient, connection.url)
        val mcpClient = McpClient("MCP Remote Client", "1.0.0")

        try {
            mcpClient.connect(transport)
            // Connection successful
        } catch (e: Exception) {
            // Handle connection error
            e.printStackTrace()
            return //TODO
        }
        updateConnectionList(connectionName, mcpClient)
    }

    suspend fun disableRemoteMcp(connectionName: String) {
        val closeJob = coroutineScope.launch(Dispatchers.IO) {
            mcpConnectionState.value.getConnection(connectionName)?.mcpClient?.close()
        }
        delay(500)
        closeJob.cancel() //TODO : 강제 종료
        updateConnectionList(connectionName, null)
    }

    fun addRemoteMcpConnection(connectionName: String, url: String) {
        val prevRemoteList = loadMcpRemoteServerList()
        val newRemoteList = prevRemoteList.copy(
            list = prevRemoteList.list.plus(McpRemoteServerItem(connectionName, url))
        )
        saveMcpRemoteServerList(newRemoteList)
        addConnectionList(connectionName, url, null)
    }

    fun editRemoteMcpConnection(prevConnectionName: String, connectionName: String, url: String) {
        val prevRemoteList = loadMcpRemoteServerList()
        val newRemoteList = prevRemoteList.copy(
            list = prevRemoteList.list.map {
                if (it.connectionName == prevConnectionName) it.copy(connectionName = connectionName, url = url) else it
            }
        )
        saveMcpRemoteServerList(newRemoteList)
        editConnectionList(prevConnectionName, connectionName, url)
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
        return Json.decodeFromString<McpRemoteServerList>(jsonString)
    }

    private fun saveMcpRemoteServerList(remoteServerList: McpRemoteServerList) {
        val newJsonString = Json.encodeToString(remoteServerList)
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
    private fun addConnectionList(connectionName: String, url: String, mcpClient: McpClient?) {
        _mcpConnectionStateState.update {
            McpConnectionState(list = it.list.plus(McpConnection(connectionName, url, mcpClient)).toImmutableList())
        }
    }
    private fun editConnectionList(prevConnectionName: String, connectionName: String, url: String) {
        _mcpConnectionStateState.update {
            McpConnectionState(list = it.list.map { item ->
                if (item.connectionName == prevConnectionName) {
                    item.copy(connectionName = connectionName, url = url)
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
    val connectionName: String = "",
    val url: String? = null,
    val mcpClient: McpClient? = null,
) {
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
private data class McpRemoteServerItem(
    val connectionName: String = "",
    val url: String = ""
)
