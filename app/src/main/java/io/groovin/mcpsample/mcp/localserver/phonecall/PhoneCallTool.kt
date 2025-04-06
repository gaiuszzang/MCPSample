package io.groovin.mcpsample.mcp.localserver.phonecall

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
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


class PhoneCallTool(
    private val context: Context,
    permissionHandler: LocalToolPermissionHandler
): LocalTool(context, permissionHandler) {
    companion object {
        private const val TAG = "PhoneCallTool"
    }

    override fun toolName(): String = "phone-call-tool"
    override fun toolDescription(): String = "Make a phone call to requested number"
    override fun toolProperties(): JsonObject =
        buildJsonObject {
            putJsonObject("number") {
                put("type", "string")
                put("description", "The number to call")
            }
        }
    override fun toolRequiredProperties(): List<String> = listOf("number")
    override fun requiredPermissions(): List<String> = listOf(Manifest.permission.CALL_PHONE)
    override suspend fun toolFunction(request: CallToolRequest): CallToolResult {
        val number = request.getStringParameter("number")?.lowercase() ?: ""
        return try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = "tel:$number".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val result = PhoneCallResult(phoneNumber = number, callResult = true)
            CallToolResult(content = listOf(TextContent(Json.encodeToString(result))))
        } catch (e: Throwable) {
            val result = CommonError(errorMessage = "Make call failed due to ${e.message}")
            CallToolResult(content = listOf(TextContent(Json.encodeToString(result))))
        }
    }

    @Serializable
    data class PhoneCallResult(
        val phoneNumber: String,
        val callResult: Boolean
    )
}
