package io.groovin.mcpsample.mcp.localserver

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive


abstract class LocalTool(
    private val context: Context,
    private val permissionHandler: LocalToolPermissionHandler
) {
    abstract fun toolName(): String
    abstract fun toolDescription(): String
    abstract fun toolProperties(): JsonObject
    abstract fun toolRequiredProperties(): List<String>
    abstract fun requiredPermissions(): List<String>
    abstract suspend fun toolFunction(request: CallToolRequest): CallToolResult

    fun addToolToServer(server: Server) {
        server.addTool(
            name = toolName(),
            description = toolDescription(),
            inputSchema = Tool.Input(
                properties = toolProperties(),
                required = toolRequiredProperties()
            )
        ) { request ->
            val invalidParameter = getInvalidParameter(request, requireList = toolRequiredProperties())
            if (invalidParameter.isNotEmpty()) {
                return@addTool createInvalidParameterCallToolResult(invalidParameter)
            }
            val noGrantedPermissions = getNoGrantedPermissions()
            if (noGrantedPermissions.isNotEmpty() && !permissionHandler.request(noGrantedPermissions)) {
                return@addTool createPermissionErrorCallToolResult(noGrantedPermissions)
            }
            return@addTool toolFunction(request)
        }
    }

    private fun getNoGrantedPermissions(): List<String> {
        return requiredPermissions().filter { perm ->
            (ActivityCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED)
        }
    }

    private fun createPermissionErrorCallToolResult(noGrantedPermissions: List<String>): CallToolResult {
        val errorMessage = noGrantedPermissions.joinToString(separator = ", ")
        val result = CommonError(errorMessage = "Permission $errorMessage is not granted.")
        return CallToolResult(
            content = listOf(TextContent(Json.encodeToString(result))),
            isError = true
        )
    }

    private fun getInvalidParameter(request: CallToolRequest, requireList: List<String>): List<String> {
        return requireList.filter { paramName ->
            request.arguments[paramName] == null
        }
    }

    private fun createInvalidParameterCallToolResult(invalidList: List<String>): CallToolResult {
        val errorMessage = invalidList.joinToString(separator = ", ")
        val result = CommonError(errorMessage = "Parameter $errorMessage is required.")
        return CallToolResult(
            content = listOf(TextContent(Json.encodeToString(result))),
            isError = true
        )
    }
}

fun Server.addTool(localTool: LocalTool) {
    localTool.addToolToServer(this)
}

fun CallToolRequest.getStringParameter(key: String): String? {
    return try {
        arguments[key]?.jsonPrimitive?.content
    } catch (e: Throwable) {
        null
    }
}

fun CallToolRequest.getIntParameter(key: String): Int? {
    return try {
        arguments[key]?.jsonPrimitive?.intOrNull
    } catch (e: Throwable) {
        null
    }
}

@Serializable
data class CommonError(
    val isSucceed: Boolean = false,
    val errorMessage: String
)

@Serializable
data class CommonSuccess(
    val isSucceed: Boolean = true,
    val message: String
)
