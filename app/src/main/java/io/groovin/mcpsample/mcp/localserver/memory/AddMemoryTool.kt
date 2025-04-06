package io.groovin.mcpsample.mcp.localserver.memory

import android.content.Context
import io.groovin.mcpsample.mcp.localserver.CommonError
import io.groovin.mcpsample.mcp.localserver.CommonSuccess
import io.groovin.mcpsample.mcp.localserver.LocalTool
import io.groovin.mcpsample.mcp.localserver.LocalToolPermissionHandler
import io.groovin.mcpsample.mcp.localserver.getStringParameter
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject


class AddMemoryTool(
    private val context: Context,
    private val permissionHandler: LocalToolPermissionHandler
): LocalTool(context, permissionHandler) {
    companion object {
        private const val TAG = "AddMemoryTool"
    }
    private val dbDao = UserMemoryDatabaseProvider.getDao(context)

    override fun toolName(): String = "add-memory"
    override fun toolDescription(): String = "Add a new memory item to the memory database."
    override fun toolProperties(): JsonObject =
        buildJsonObject {
            putJsonObject("content") {
                put("type", "string")
                put("description", "The full text content of the memory to store.")
            }
            putJsonObject("tags") {
                put("type", "string")
                put("description", "Comma-separated keywords used to categorize and retrieve the memory. Optional.")
            }
        }
    override fun toolRequiredProperties(): List<String> = listOf("content")
    override fun requiredPermissions(): List<String> = listOf()

    override suspend fun toolFunction(request: CallToolRequest): CallToolResult {
        val content = request.getStringParameter("content")
        val tags = request.getStringParameter("tags")
        if (content.isNullOrEmpty()) {
            val result = CommonError(errorMessage = "Key or Value is empty.")
            return CallToolResult(
                content = listOf(TextContent(Json.encodeToString(result))),
                isError = true
            )
        }
        try {
            dbDao.insert(UserMemory(content = content, tags = tags))
            val result = CommonSuccess(message = "Memory $content, $tags has been added.")
            return CallToolResult(
                content = listOf(TextContent(Json.encodeToString(result)))
            )
        } catch (e: Throwable) {
            val result = CommonError(errorMessage = "Failed to save memory with key $content and value $tags.")
            return CallToolResult(
                content = listOf(TextContent(Json.encodeToString(result))),
                isError = true
            )
        }
    }
}

