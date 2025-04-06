package io.groovin.mcpsample

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import io.groovin.mcpsample.localserver.McpLocalServer
import io.groovin.mcpsample.mcp.McpClient
import io.groovin.mcpsample.mcp.McpLocalTransport
import io.groovin.mcpsample.mcp.SSETransport
import io.groovin.mcpsample.util.logd
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
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
        private const val API_KEY_PREF_KEY = "API-KEY"
        private const val MCP_REMOTE_SERVER_LIST = "MCP_REMOTE_SERVER_LIST"
    }
    private val coroutineScope = CoroutineScope(SupervisorJob())


    fun getApiKey(): String {
        return sharedPreferences.getString(API_KEY_PREF_KEY, "") ?: ""
    }

    fun setApiKey(apiKey: String) {
        sharedPreferences.edit {
            putString(API_KEY_PREF_KEY, apiKey)
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
        val transport = SSETransport(httpClient, connection.url)
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