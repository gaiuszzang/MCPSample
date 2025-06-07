package io.groovin.mcpsample.mcp.localserver.memory

import android.content.Context
import io.groovin.mcpsample.mcp.localserver.CommonError
import io.groovin.mcpsample.mcp.localserver.LocalTool
import io.groovin.mcpsample.mcp.localserver.LocalToolPermissionHandler
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject


class GetAllMemoryTool(
    private val context: Context,
    private val permissionHandler: LocalToolPermissionHandler
): LocalTool(context, permissionHandler) {
    companion object {
        private const val TAG = "GetAllMemoryTool"
    }
    private val dbDao = UserMemoryDatabaseProvider.getDao(context)

    override fun toolName(): String = "get-all-memory"
    override fun toolDescription(): String = "Retrieve the complete list of stored memory items."
    override fun toolProperties(): JsonObject =
        buildJsonObject {}
    override fun toolRequiredProperties(): List<String> = listOf()
    override fun requiredPermissions(): List<String> = listOf()

    override suspend fun toolFunction(request: CallToolRequest): CallToolResult {
        val memoryList = dbDao.getAllMemory()

        return if (memoryList.isNotEmpty()) {
            val result = MemoryResult(memoryList = memoryList)
            CallToolResult(
                content = listOf(TextContent(Json.encodeToString(result)))
            )
        } else {
            val result = CommonError(errorMessage = "Memory is empty.")
            CallToolResult(
                content = listOf(TextContent(Json.encodeToString(result))),
                isError = true
            )
        }
    }

    @Serializable
    data class MemoryResult(
        val memoryList: List<UserMemory>
    )
}

