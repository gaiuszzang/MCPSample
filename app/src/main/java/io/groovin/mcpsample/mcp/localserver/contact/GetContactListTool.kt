package io.groovin.mcpsample.mcp.localserver.contact

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
import io.groovin.mcpsample.mcp.localserver.CommonError
import io.groovin.mcpsample.mcp.localserver.LocalTool
import io.groovin.mcpsample.mcp.localserver.LocalToolPermissionHandler
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject


class GetContactListTool(
    private val context: Context,
    permissionHandler: LocalToolPermissionHandler
): LocalTool(context, permissionHandler) {
    companion object {
        private const val TAG = "GetContactListTool"
    }
    override fun toolName(): String = "contact-list-info-tool"
    override fun toolDescription(): String = "Get Contact List Information that contains phone number"
    override fun toolProperties(): JsonObject =
        buildJsonObject {}
    override fun toolRequiredProperties(): List<String> = listOf()
    override fun requiredPermissions(): List<String> = listOf(Manifest.permission.READ_CONTACTS)

    override suspend fun toolFunction(request: CallToolRequest): CallToolResult {
        val contactList = getContact()
        return if (contactList.isNotEmpty()) {
            val list = contactList.map {
                ContactResult.Contact(
                    name = it.first,
                    phoneNumber = it.second
                )
            }
            val result = ContactResult(
                contactCount = list.count(),
                contactList = list
            )
            CallToolResult(
                content = listOf(TextContent(Json.encodeToString(result)))
            )
        } else {
            val result = CommonError(errorMessage = "Contact List is empty.")
            CallToolResult(
                content = listOf(TextContent(Json.encodeToString(result))),
                isError = true
            )
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
        if (cursor != null) {
            while (cursor.moveToNext()) {
                val nameIndex = cursor.getColumnIndex(projection[1])
                val numberIndex = cursor.getColumnIndex(projection[2])
                val name = cursor.getString(nameIndex)
                val number = cursor.getString(numberIndex)
                result.add(Pair(name, number))
            }
        }
        // 데이터 계열은 반드시 닫아줘야 한다.
        cursor!!.close()
        return result
    }

    @Serializable
    data class ContactResult(
        val contactCount: Int,
        val contactList: List<Contact>
    ) {
        @Serializable
        data class Contact(
            val name: String,
            val phoneNumber: String
        )
    }
}
