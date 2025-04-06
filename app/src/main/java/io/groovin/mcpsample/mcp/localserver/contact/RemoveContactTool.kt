package io.groovin.mcpsample.mcp.localserver.contact

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
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


class RemoveContactTool(
    private val context: Context,
    permissionHandler: LocalToolPermissionHandler
): LocalTool(context, permissionHandler) {
    companion object {
        private const val TAG = "RemoveContactTool"
    }
    override fun toolName(): String = "remove-contact-tool"
    override fun toolDescription(): String = "Remove the Contact Information"
    override fun toolProperties(): JsonObject =
        buildJsonObject {
            putJsonObject("name") {
                put("type", "string")
                put("description", "person name, or person nickname who want to remove")
            }
            putJsonObject("number") {
                put("type", "string")
                put("description", "phone number which want to remove")
            }
        }
    override fun toolRequiredProperties(): List<String> = listOf()
    override fun requiredPermissions(): List<String> = listOf(Manifest.permission.WRITE_CONTACTS)

    override suspend fun toolFunction(request: CallToolRequest): CallToolResult {
        val name = request.getStringParameter("name")
        val number = request.getStringParameter("number")

        if (name == null && number == null) {
            val result = CommonError(errorMessage = "Contact name or number is required.")
            return CallToolResult(
                content = listOf(TextContent(Json.encodeToString(result))),
                isError = true
            )
        }
        val isSucceed = if (name != null) {
            deleteContactByName(context, name)
        } else if (number != null) {
            deleteContactByPhoneNumber(context, number)
        } else {
            false
        }
        return if (isSucceed) {
            val result = CommonSuccess(message = "Contact ${name ?: number} has been removed.")
            CallToolResult(
                content = listOf(TextContent(Json.encodeToString(result)))
            )
        } else {
            val result = CommonError(errorMessage = "Failed to remove contact ${name ?: number}.")
            CallToolResult(
                content = listOf(TextContent(Json.encodeToString(result))),
                isError = true
            )
        }
    }


    private fun deleteContactByName(context: Context, name: String): Boolean {
        val contentResolver: ContentResolver = context.contentResolver

        // 이름으로 RawContact ID 검색
        val cursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data.RAW_CONTACT_ID),
            "${ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME} = ?",
            arrayOf(name),
            null
        )

        return if (cursor != null && cursor.moveToFirst()) {
            val rawContactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Data.RAW_CONTACT_ID))

            // RawContact 삭제
            val rowsDeleted = contentResolver.delete(
                ContactsContract.RawContacts.CONTENT_URI,
                "${ContactsContract.RawContacts._ID} = ?",
                arrayOf(rawContactId)
            )
            cursor.close()
            rowsDeleted > 0
        } else {
            cursor?.close()
            false
        }
    }

    private fun deleteContactByPhoneNumber(context: Context, phoneNumber: String): Boolean {
        val contentResolver: ContentResolver = context.contentResolver

        // 전화번호로 RawContact ID 검색
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID),
            "${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?",
            arrayOf(phoneNumber),
            null
        )

        return if (cursor != null && cursor.moveToFirst()) {
            val contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID))

            // RawContact 삭제
            val rowsDeleted = contentResolver.delete(
                ContactsContract.RawContacts.CONTENT_URI,
                "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                arrayOf(contactId)
            )
            cursor.close()
            rowsDeleted > 0
        } else {
            cursor?.close()
            false
        }
    }
}
