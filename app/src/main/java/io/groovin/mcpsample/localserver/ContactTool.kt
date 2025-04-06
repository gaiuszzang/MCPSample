package io.groovin.mcpsample.localserver

import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
import io.groovin.mcpsample.util.logd
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject


class ContactTool(
    private val context: Context
): LocalTool {
    companion object {
        private const val TAG = "ContactTool"
    }
    override fun addTool(server: Server) {
        server.addTool(
            name = "contact-detail-info-tool",
            description = "Contact Information that contains phone number and email address",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("name") {
                        put("type", "string")
                        put("description", "person name")
                    }
                },
                required = listOf("name")
            )
        ) { request ->
            val name = try {
                (request.arguments["name"]?.jsonPrimitive?.content ?: "unknown").lowercase()
            } catch (e: Throwable) {
                "unknown"
            }

            val contactList = getContact()
            val number = contactList.firstOrNull {
                logd(TAG, "getContact() check (${it.first.lowercase()} == $name) = ${it.first.lowercase() == name}")
                it.first.lowercase() == name
            }?.second

            if (number != null) {
                CallToolResult(
                    content = listOf(TextContent("$name, $number"))
                )
            } else {
                CallToolResult(
                    content = listOf(TextContent("Cannot find contact information for $name"))
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

}