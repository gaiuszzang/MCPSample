package io.groovin.mcpsample.mcp.localserver.memory

import android.content.Context
import android.content.Intent
import android.provider.Settings
import io.groovin.mcpsample.mcp.localserver.CommonError
import io.groovin.mcpsample.mcp.localserver.LocalTool
import io.groovin.mcpsample.mcp.localserver.LocalToolPermissionHandler
import io.groovin.mcpsample.mcp.localserver.getStringParameter
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class SearchMemoryTool(
    private val context: Context,
    private val permissionHandler: LocalToolPermissionHandler
): LocalTool(context, permissionHandler) {
    companion object {
        private const val TAG = "SearchMemoryTool"
    }
    private val dbDao = UserMemoryDatabaseProvider.getDao(context)

    override fun toolName(): String = "search-memory"
    override fun toolDescription(): String = "Search memory items using keyword-based matching."
    override fun toolProperties(): JsonObject =
        buildJsonObject {
            putJsonObject("keyword") {
                put("type", "string")
                put("description", "Keyword to search for in memory content or tags.")
            }
        }
    override fun toolRequiredProperties(): List<String> = listOf("keyword")
    override fun requiredPermissions(): List<String> = listOf()

    override suspend fun toolFunction(request: CallToolRequest): CallToolResult {
        val keyword = request.getStringParameter("keyword")
        if (keyword.isNullOrEmpty()) {
            val result = CommonError(errorMessage = "Keyword is empty.")
            return CallToolResult(
                content = listOf(TextContent(Json.encodeToString(result))),
                isError = true
            )
        }
        val memoryList = dbDao.searchMemory(keyword)

        return if (memoryList.isNotEmpty()) {
            val result = MemoryResult(notificationList = memoryList)
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
        val notificationList: List<UserMemory>
    )
}

