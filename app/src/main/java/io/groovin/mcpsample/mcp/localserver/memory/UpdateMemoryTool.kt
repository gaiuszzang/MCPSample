package io.groovin.mcpsample.mcp.localserver.memory

import android.content.Context
import android.content.Intent
import android.provider.Settings
import io.groovin.mcpsample.mcp.localserver.CommonError
import io.groovin.mcpsample.mcp.localserver.CommonSuccess
import io.groovin.mcpsample.mcp.localserver.LocalTool
import io.groovin.mcpsample.mcp.localserver.LocalToolPermissionHandler
import io.groovin.mcpsample.mcp.localserver.getIntParameter
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


class UpdateMemoryTool(
    private val context: Context,
    private val permissionHandler: LocalToolPermissionHandler
): LocalTool(context, permissionHandler) {
    companion object {
        private const val TAG = "UpdateMemoryTool"
    }
    private val dbDao = UserMemoryDatabaseProvider.getDao(context)

    override fun toolName(): String = "update-memory"
    override fun toolDescription(): String = "Update an existing memory item with new content or tags."
    override fun toolProperties(): JsonObject =
        buildJsonObject {
            putJsonObject("id") {
                put("type", "integer")
                put("description", "The unique identifier of the memory item to update.")
            }
            putJsonObject("content") {
                put("type", "string")
                put("description", "The new content to replace the existing memory. Optional.")
            }
            putJsonObject("tags") {
                put("type", "string")
                put("description", "The updated comma-separated tags to associate with the memory. Optional.")
            }
        }
    override fun toolRequiredProperties(): List<String> = listOf("id")
    override fun requiredPermissions(): List<String> = listOf()

    override suspend fun toolFunction(request: CallToolRequest): CallToolResult {
        val id = request.getIntParameter("id")
        val content = request.getStringParameter("content")
        val tags = request.getStringParameter("tags")
        if (id == null) {
            val result = CommonError(errorMessage = "id is empty.")
            return CallToolResult(
                content = listOf(TextContent(Json.encodeToString(result))),
                isError = true
            )
        }
        if (content == null && tags == null) {
            val result = CommonError(errorMessage = "content & tags both are empty.")
            return CallToolResult(
                content = listOf(TextContent(Json.encodeToString(result))),
                isError = true
            )
        }

        try {
            if (content != null) dbDao.updateContentById(id, content)
            if (tags != null) dbDao.updateTagsById(id, tags)
            val result = CommonSuccess(message = "Memory $id, $content, $tags has been updated.")
            return CallToolResult(
                content = listOf(TextContent(Json.encodeToString(result)))
            )
        } catch (e: Throwable) {
            val result = CommonError(errorMessage = "Failed to update memory about $id with $content, $tags.")
            return CallToolResult(
                content = listOf(TextContent(Json.encodeToString(result))),
                isError = true
            )
        }
    }
}

