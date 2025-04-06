package io.groovin.mcpsample.mcp.localserver.contact

import android.Manifest
import android.content.ContentProviderOperation
import android.content.Context
import android.provider.ContactsContract
import io.groovin.mcpsample.mcp.localserver.CommonError
import io.groovin.mcpsample.mcp.localserver.CommonSuccess
import io.groovin.mcpsample.mcp.localserver.LocalTool
import io.groovin.mcpsample.mcp.localserver.LocalToolPermissionHandler
import io.groovin.mcpsample.mcp.localserver.getStringParameter
import io.groovin.mcpsample.util.logd
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject


class AddContactTool(
    private val context: Context,
    permissionHandler: LocalToolPermissionHandler
): LocalTool(context, permissionHandler) {
    companion object {
        private const val TAG = "AddContactTool"
    }
    override fun toolName(): String = "add-contact-tool"
    override fun toolDescription(): String = "Add the Contact Information with name and phone number"
    override fun toolProperties(): JsonObject =
        buildJsonObject {
            putJsonObject("name") {
                put("type", "string")
                put("description", "person name, or person nickname")
            }
            putJsonObject("number") {
                put("type", "string")
                put("description", "phone number")
            }
        }
    override fun toolRequiredProperties(): List<String> = listOf("name", "number")
    override fun requiredPermissions(): List<String> = listOf(Manifest.permission.WRITE_CONTACTS)

    override suspend fun toolFunction(request: CallToolRequest): CallToolResult {
        val name = request.getStringParameter("name") ?: "unknown"
        val number = request.getStringParameter("number") ?: "unknown"
        val isSucceed = addContact(context, name, number)
        return if (isSucceed) {
            val result = CommonSuccess(message = "Contact $name with number $number has been added.")
            CallToolResult(
                content = listOf(TextContent(Json.encodeToString(result)))
            )
        } else {
            val result = CommonError(errorMessage = "Failed to add contact $name with number $number.")
            CallToolResult(
                content = listOf(TextContent(Json.encodeToString(result))),
                isError = true
            )
        }
    }

    private fun addContact(context: Context, name: String, phoneNumber: String): Boolean {
        val operations = ArrayList<ContentProviderOperation>()

        // 이름 추가
        operations.add(
            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build()
        )

        // 이름 필드 추가
        operations.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                .build()
        )

        // 전화번호 필드 추가
        operations.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build()
        )

        try {
            // 연락처 추가 실행
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
            return true
        } catch (e: Exception) {
            logd(TAG, "Error adding contact: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
}
