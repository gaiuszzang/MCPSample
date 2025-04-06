package io.groovin.mcpsample.localserver

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import io.groovin.mcpsample.util.logd
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject


class PhoneCallTool(
    private val context: Context
): LocalTool {
    companion object {
        private const val TAG = "PhoneCallTool"
    }
    override fun addTool(server: Server) {
        server.addTool(
            name = "phone-call-tool",
            description = "Make a phone call to requested number",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("number") {
                        put("type", "string")
                        put("description", "The number to call")
                    }
                },
                required = listOf("name")
            )
        ) { request ->
            val number = try {
                request.arguments["number"]?.jsonPrimitive?.content?.lowercase()
            } catch (e: Throwable) {
                null
            }
            val permCheck = ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
            if (permCheck && number != null) {
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = "tel:$number".toUri()
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                CallToolResult(
                    content = listOf(TextContent("Calling $number"))
                )
            } else {
                CallToolResult(
                    isError = true,
                    content = listOf(TextContent(
                        text = if (permCheck) "Invalid or missing 'number' parameter" else "CALL_PHONE permission not granted"
                        )
                    )
                )
            }
        }
    }
}