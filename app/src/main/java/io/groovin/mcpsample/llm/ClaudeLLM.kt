package io.groovin.mcpsample.llm

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.MessageParam
import com.anthropic.models.messages.ToolUnion
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.groovin.mcpsample.util.logd
import io.groovin.mcpsample.util.suspendRetry
import io.modelcontextprotocol.kotlin.sdk.CallToolResultBase
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.coroutines.yield
import kotlinx.serialization.json.JsonObject
import kotlin.jvm.optionals.getOrNull

class ClaudeLLM: LLM {
    companion object {
        private const val TAG = "ClaudeLLM"
    }

    private var anthropic: AnthropicClient? = null
    private var messageParamsBuilder: MessageCreateParams.Builder? = null

    override fun setApiKey(apiKey: String) {
        anthropic?.close()
        anthropic = AnthropicOkHttpClient.builder()
            .apiKey(apiKey)
            .build()
    }

    override fun setModel(model: String) {
        messageParamsBuilder = MessageCreateParams.builder()
            .model(model)
            .maxTokens(8192)
    }

    override suspend fun handleMessage(
        systemPrompt: String,
        message: String,
        mcpTools: List<Tool>,
        onResponse: (type: ResponseType, message: String) -> Unit,
        onToolCall: suspend (name: String, arguments: Map<String, Any?>) -> CallToolResultBase?
    ) {
        val messages: MutableList<MessageParam> = mutableListOf()
        if (systemPrompt.isNotEmpty()) {
            messages.add(
                MessageParam.builder()
                    .role(MessageParam.Role.USER)
                    .content(systemPrompt)
                    .build()
            )
        }
        val anthropicTools = mcpTools.map { tool ->
            ToolUnion.ofTool(
                com.anthropic.models.messages.Tool.builder()
                    .name(tool.name)
                    .description(tool.description ?: "")
                    .inputSchema(
                        com.anthropic.models.messages.Tool.InputSchema.builder()
                            .type(JsonValue.from(tool.inputSchema.type))
                            .properties(tool.inputSchema.properties.toJsonValue())
                            .putAdditionalProperty("required", JsonValue.from(tool.inputSchema.required))
                            .build()
                    )
                    .build()
            )
        }
        anthropicHandleMessage(message, anthropicTools, messages, onResponse, onToolCall)
    }


    private suspend fun anthropicHandleMessage(
        message: String,
        anthropicTools: List<ToolUnion>,
        messages: MutableList<MessageParam>,
        onResponse: (type: ResponseType, message: String) -> Unit,
        onToolCall: suspend (name: String, arguments: Map<String, Any?>) -> CallToolResultBase?
    ) {
        val anthropicClient = anthropic ?: throw Throwable("Anthropic client is not initialized")
        val messageBuilder = messageParamsBuilder ?: throw Throwable("Message Params Builder is not initialized")

        // Send the query to the Anthropic model and get the response

        val response = try {
            suspendRetry {
                yield()
                messages.add(
                    MessageParam.builder()
                        .role(MessageParam.Role.USER)
                        .content(message)
                        .build()
                )
                return@suspendRetry anthropicClient.messages().create(
                    messageBuilder
                        .messages(messages)
                        .tools(anthropicTools)
                        .build()
                )
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            onResponse(ResponseType.System, "Error: ${e.message}")
            return
        }
        response.content().forEach { content ->
            yield()
            when {
                content.isText() -> {
                    onResponse(ResponseType.Text, content.text().getOrNull()?.text() ?: "")
                }
                content.isThinking() -> {
                    onResponse(ResponseType.Thinking, content.thinking().get().thinking())
                }
                content.isRedactedThinking() -> {
                    onResponse(ResponseType.Thinking, content.redactedThinking().get().data())
                }
                // If the response indicates a tool use, process it further
                content.isToolUse() -> {
                    val toolName = content.toolUse().get().name()
                    val toolArgs = content.toolUse().get()._input().convert(object : TypeReference<Map<String, JsonValue>>() {})
                    logd(TAG, "Tool use: $toolName with args $toolArgs")
                    val result = onToolCall(toolName, toolArgs ?: emptyMap())

                    logd(TAG, "Tool use: CallTool result = ${result?.content}")
                    // Add the tool result message to the conversation
                    val toolCallResultMessage =
                        """
                            "type": "tool_result",
                            "tool_name": $toolName,
                            "result": ${result?.content?.joinToString("\n") { (it as TextContent).text ?: "" }}
                        """.trimIndent()

                    // recursive call anthropicHandleMessage for continuation
                    anthropicHandleMessage(
                        message = toolCallResultMessage,
                        anthropicTools = anthropicTools,
                        messages = messages,
                        onResponse = onResponse,
                        onToolCall = onToolCall
                    )
                }
            }
        }
    }

    private fun JsonObject.toJsonValue(): JsonValue {
        val mapper = ObjectMapper()
        val node = mapper.readTree(this.toString())
        return JsonValue.fromJsonNode(node)
    }
}
