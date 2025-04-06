package io.groovin.mcpsample.mcp.localserver.notification

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


class NotificationReadTool(
    private val context: Context,
    private val permissionHandler: LocalToolPermissionHandler
): LocalTool(context, permissionHandler) {
    companion object {
        private const val TAG = "NotificationReadTool"
    }
    private val dbDao = NotificationDatabaseProvider.getDao(context)

    override fun toolName(): String = "notification-read-tool"
    override fun toolDescription(): String = "Get Notification List from Android System"
    override fun toolProperties(): JsonObject =
        buildJsonObject {
            putJsonObject("appName") {
                put("type", "string")
                put("description", "(optional) app name to filter")
            }
            putJsonObject("content") {
                put("type", "string")
                put("description", "(optional) notification content to filter")
            }
        }
    override fun toolRequiredProperties(): List<String> = listOf()
    override fun requiredPermissions(): List<String> = listOf()

    override suspend fun toolFunction(request: CallToolRequest): CallToolResult {
        if (!isNotificationServiceEnabled(context)) {
            permissionHandler.requestCustomPermission(
                guideText = "Notification Read Permission is required.",
                onActionIntent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            )
            val result = CommonError(errorMessage = "Notification Read Permission is not granted.")
            return CallToolResult(
                content = listOf(TextContent(Json.encodeToString(result))),
                isError = true
            )
        }
        val appName = request.getStringParameter("appName")
        val content = request.getStringParameter("content")
        val notiList = when {
            (appName != null && content != null) -> dbDao.searchByAppNameWithContent(appName, content)
            (appName != null) -> dbDao.searchByAppName(appName)
            (content != null) -> dbDao.searchByContent(content)
            else -> dbDao.getAll()
        }.toNotificationItemList()

        return if (notiList.isNotEmpty()) {
            val result = NotificationResult(notificationList = notiList)
            CallToolResult(
                content = listOf(TextContent(Json.encodeToString(result)))
            )
        } else {
            val result = CommonError(errorMessage = "Notification List is empty.")
            CallToolResult(
                content = listOf(TextContent(Json.encodeToString(result))),
                isError = true
            )
        }
    }

    private fun List<UserNotification>.toNotificationItemList(
    ): List<NotificationResult.NotificationItem> {
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return map { userNotification ->
            NotificationResult.NotificationItem(
                id = userNotification.id,
                title = userNotification.title,
                text = userNotification.text,
                subText = userNotification.subText,
                packageName = userNotification.packageName,
                appName = userNotification.appName,
                datetime = dateTimeFormat.format(Date(userNotification.datetime))
            )
        }
    }

    @Serializable
    data class NotificationResult(
        val notificationList: List<NotificationItem>
    ) {
        @Serializable
        data class NotificationItem(
            val id: Int = 0,
            val title: String,
            val text: String,
            val subText: String?,
            val packageName: String,
            val appName: String,
            val datetime: String
        )
    }
}

