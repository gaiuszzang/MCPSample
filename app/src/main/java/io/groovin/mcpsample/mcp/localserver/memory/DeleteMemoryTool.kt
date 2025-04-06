package io.groovin.mcpsample.mcp.localserver.memory

import android.content.Context
import io.groovin.mcpsample.mcp.localserver.CommonError
import io.groovin.mcpsample.mcp.localserver.CommonSuccess
import io.groovin.mcpsample.mcp.localserver.LocalTool
import io.groovin.mcpsample.mcp.localserver.LocalToolPermissionHandler
import io.groovin.mcpsample.mcp.localserver.getIntParameter
import io.groovin.mcpsample.mcp.localserver.getStringParameter
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject


class DeleteMemoryTool(
    private val context: Context,
    private val permissionHandler: LocalToolPermissionHandler
): LocalTool(context, permissionHandler) {
    companion object {
        private const val TAG = "DeleteMemoryTool"
    }
    private val dbDao = UserMemoryDatabaseProvider.getDao(context)

    override fun toolName(): String = "delete-memory"
    override fun toolDescription(): String = "Delete a specific memory entry by its id."
    override fun toolProperties(): JsonObject =
        buildJsonObject {
            putJsonObject("id") {
                put("type", "integer")
                put("description", "The unique identifier of the memory item to be deleted.")
            }
        }
    override fun toolRequiredProperties(): List<String> = listOf("id")
    override fun requiredPermissions(): List<String> = listOf()

    override suspend fun toolFunction(request: CallToolRequest): CallToolResult {
        val id = request.getIntParameter("id")
        if (id == null) {
            val result = CommonError(errorMessage = "id is empty.")
            return CallToolResult(
                content = listOf(TextContent(Json.encodeToString(result))),
                isError = true
            )
        }
        try {
            dbDao.deleteById(id = id)
            val result = CommonSuccess(message = "Memory $id has been deleted.")
            return CallToolResult(
                content = listOf(TextContent(Json.encodeToString(result)))
            )
        } catch (e: Throwable) {
            val result = CommonError(errorMessage = "Failed to delete memory with key $id.")
            return CallToolResult(
                content = listOf(TextContent(Json.encodeToString(result))),
                isError = true
            )
        }
    }
}

