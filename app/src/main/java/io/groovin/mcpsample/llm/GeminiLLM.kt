package io.groovin.mcpsample.llm

import dev.shreyaspatil.ai.client.generativeai.Chat
import dev.shreyaspatil.ai.client.generativeai.GenerativeModel
import dev.shreyaspatil.ai.client.generativeai.type.Content
import dev.shreyaspatil.ai.client.generativeai.type.FunctionCallPart
import dev.shreyaspatil.ai.client.generativeai.type.FunctionDeclaration
import dev.shreyaspatil.ai.client.generativeai.type.FunctionResponsePart
import dev.shreyaspatil.ai.client.generativeai.type.RequestOptions
import dev.shreyaspatil.ai.client.generativeai.type.Schema
import dev.shreyaspatil.ai.client.generativeai.type.TextPart
import dev.shreyaspatil.ai.client.generativeai.type.Tool as GeminiTool
import dev.shreyaspatil.ai.client.generativeai.type.generationConfig
import io.groovin.mcpsample.llm.LLMManager.MessageItem
import io.groovin.mcpsample.util.logd
import io.groovin.mcpsample.util.suspendRetry
import io.modelcontextprotocol.kotlin.sdk.CallToolResultBase
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.coroutines.yield
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.seconds

class GeminiLLM: LLM {
    companion object {
        private const val TAG = "GeminiLLM"
    }

    private var apiKey: String = ""
    private var modelName: String = ""

    override fun setApiKey(apiKey: String) {
        this.apiKey = apiKey
    }

    override fun setModel(model: String) {
        modelName = model
    }

    override suspend fun handleMessage(
        systemPrompt: String,
        messages: List<MessageItem>,
        mcpTools: List<Tool>,
        onResponse: (type: ResponseType, message: String) -> Unit,
        onToolCall: suspend (name: String, arguments: Map<String, Any?>) -> CallToolResultBase?
    ) {
        val tools = if (mcpTools.isNotEmpty()) {
            listOf(
                GeminiTool(
                    functionDeclarations = mcpTools.map { convertMcpToolToGeminiFunction(it) }
                )
            )
        } else {
            null
        }
        val historyChat = if (messages.size > 1) {
            messages.subList(0, messages.size - 1).map {
                Content(
                    role = if (it.type == MessageItem.Type.Assistant) "model" else "user",
                    parts = listOf(TextPart(text = it.message))
                )
            }
        } else {
            emptyList()
        }
        val firstMessage = if (messages.isNotEmpty()) {
            Content(parts = listOf(TextPart(text = messages.last().message)))
        } else {
            onResponse(ResponseType.System, "Error: No message to send")
            return
        }
        val chat = GenerativeModel(
            apiKey = apiKey,
            modelName = modelName,
            generationConfig = generationConfig {
                temperature = 0.7f
            },
            tools = tools,
            requestOptions = RequestOptions(timeout = 20.seconds),
            systemInstruction = Content(
                parts = listOf(TextPart(text = systemPrompt))
            )
        ).startChat(history = historyChat)
        geminiHandleMessage(firstMessage, chat, onResponse, onToolCall)
    }


    private suspend fun geminiHandleMessage(
        content: Content,
        chat: Chat,
        onResponse: (type: ResponseType, message: String) -> Unit,
        onToolCall: suspend (name: String, arguments: Map<String, Any?>) -> CallToolResultBase?
    ) {
        // Send the query to the Gemini model and get the response
        val response = try {
            suspendRetry {
                yield()
                return@suspendRetry chat.sendMessage(content)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            onResponse(ResponseType.System, "Error: ${e.message}")
            return
        }

        response.candidates.first().content.parts.forEach { part ->
            yield()
            when (part) {
                is TextPart -> {
                    onResponse(ResponseType.Text, part.text)
                }
                is FunctionCallPart -> {
                    logd(TAG, "Tool use: ${part.name} with args ${part.args}")
                    val result = onToolCall(part.name, part.args ?: emptyMap())
                    logd(TAG, "Tool use: CallTool result = ${result?.content}")

                    // 모델에 함수 실행 결과 전달
                    val resultJsonObject = buildJsonObject {
                        put("result", result?.content?.joinToString("\n") { (it as TextContent).text ?: "" })
                    }
                    val funcCallResultContent = Content(
                        role = "function",
                        parts = listOf(FunctionResponsePart(part.name, resultJsonObject))
                    )
                    geminiHandleMessage(funcCallResultContent, chat, onResponse, onToolCall)
                }
            }
        }
    }

    private fun convertMcpToolToGeminiFunction(tool: Tool): FunctionDeclaration {
        logd(TAG, "convertMcpToolToGeminiFunction: ${tool.name} ${tool.inputSchema}")

        val parameters = tool.inputSchema.properties.map {(name, jsonElement) -> toSchema(name, jsonElement)}
        return FunctionDeclaration(
            name = tool.name,
            description = tool.description ?: "",
            parameters = parameters,
            requiredParameters = tool.inputSchema.required ?: emptyList()
        )
    }

    private fun toSchema(name: String, jsonElement: JsonElement): Schema<out Any> {
        val jsonObject = jsonElement.jsonObject
        val typeString = jsonObject["type"]?.jsonPrimitive?.content?.lowercase() ?: "string"
        val description = jsonObject["description"]?.jsonPrimitive?.content ?: ""
        return when (typeString) {
            "string" -> {
                val enum = jsonObject["enum"]
                if (enum != null) {
                    val enumValues = enum.jsonArray.map { it.jsonPrimitive.content }
                    Schema.enum(name, description, enumValues)
                } else {
                    Schema.str(name, description)
                }
            }
            "integer" -> Schema.int(name, description)
            "number" -> Schema.double(name, description)
            "boolean" -> Schema.bool(name, description)
            "array" -> {
                val items = jsonObject.toInnerSchemaList("items")
                if (items == null) {
                    Schema.arr(name, description)
                } else {
                    Schema.arr(name, description, items.firstOrNull())
                }
            }
            "object" -> {
                val objects = jsonObject.toInnerSchemaList("properties")
                if (objects == null) {
                    Schema.obj(name, description)
                } else {
                    Schema.obj(name, description, *(objects.toTypedArray()))
                }
            }
            else -> Schema.str(name, description)
        }
    }

    private fun JsonObject.toInnerSchemaList(key: String): List<Schema<out Any>>? {
        val properties = get(key)?.jsonObject?.get("properties")?.jsonObject
        return properties?.map { (name, jsonElement) ->
            toSchema(name, jsonElement)
        }
    }
}
