package io.groovin.mcpsample.mcp.localserver.contact

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
import io.groovin.mcpsample.mcp.localserver.CommonError
import io.groovin.mcpsample.mcp.localserver.LocalTool
import io.groovin.mcpsample.mcp.localserver.LocalToolPermissionHandler
import io.groovin.mcpsample.mcp.localserver.getStringParameter
import io.groovin.mcpsample.util.logd
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject


class GetContactDetailTool(
    private val context: Context,
    permissionHandler: LocalToolPermissionHandler
): LocalTool(context, permissionHandler) {
    companion object {
        private const val TAG = "GetContactDetailTool"
    }
    override fun toolName(): String = "contact-detail-info-tool"
    override fun toolDescription(): String = "Get Detail Contact Information that contains phone number"
    override fun toolProperties(): JsonObject =
        buildJsonObject {
            putJsonObject("name") {
                put("type", "string")
                put("description", "person name, or person nickname")
            }
        }
    override fun toolRequiredProperties(): List<String> = listOf("name")
    override fun requiredPermissions(): List<String> = listOf(Manifest.permission.READ_CONTACTS)

    override suspend fun toolFunction(request: CallToolRequest): CallToolResult {
        val name = request.getStringParameter("name")?.lowercase() ?: "unknown"
        val contactList = getContact()
        val number = contactList.firstOrNull {
            val contactName = it.first.lowercase()
            logd(TAG, "getContact() check (${contactName} == $name) = ${contactName == name}")
            contactName == name
        }?.second

        return if (number != null) {
            val result = ContactResult(name = name, phoneNumber = number)
            CallToolResult(
                content = listOf(TextContent(Json.encodeToString(result)))
            )
        } else {
            val similarResult = contactList.filter {
                val contactName = it.first.lowercase()
                contactName.contains(name)
            }.map {
                ContactResult(it.first, it.second)
            }
            return if (similarResult.isEmpty()) {
                val result = CommonError(errorMessage = "Cannot find contact information for $name")
                CallToolResult(
                    content = listOf(TextContent(Json.encodeToString(result))),
                    isError = true
                )
            } else {
                CallToolResult(
                    content = listOf(TextContent(Json.encodeToString(similarResult)))
                )
            }
        }
    }

    private fun getContact(): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        val resolver: ContentResolver = context.contentResolver
        val phoneUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val cursor = resolver.query(phoneUri, projection, null, null, null)
        cursor?.use {
            while (it.moveToNext()) {
                val nameIndex = it.getColumnIndex(projection[1])
                val numberIndex = it.getColumnIndex(projection[2])
                val name = it.getString(nameIndex)
                val number = it.getString(numberIndex)
                result.add(Pair(name, number))
            }
        }
        return result
    }

    @Serializable
    data class ContactResult(
        val name: String,
        val phoneNumber: String
    )
}
