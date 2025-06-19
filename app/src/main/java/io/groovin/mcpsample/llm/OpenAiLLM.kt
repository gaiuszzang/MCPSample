package io.groovin.mcpsample.llm

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.core.JsonValue
import com.openai.core.jsonMapper
import com.openai.models.FunctionDefinition
import com.openai.models.FunctionParameters
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam
import com.openai.models.chat.completions.ChatCompletionCreateParams
import com.openai.models.chat.completions.ChatCompletionTool
import com.openai.models.chat.completions.ChatCompletionToolMessageParam
import io.groovin.mcpsample.llm.LLMManager.MessageItem
import io.groovin.mcpsample.util.logd
import io.modelcontextprotocol.kotlin.sdk.CallToolResultBase
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.stream.consumeAsFlow
import kotlinx.coroutines.yield
import kotlinx.serialization.json.JsonObject

class OpenAiLLM: LLM {
    companion object {
        private const val TAG = "OpenAiLLM"
    }

    private var openAi: OpenAIClient? = null
    private var model: String? = null

    override fun setApiKey(apiKey: String) {
        openAi?.close()
        openAi = OpenAIOkHttpClient.builder()
            .timeout(java.time.Duration.ofSeconds(20))
            .apiKey(apiKey)
            .build()
    }

    override fun setModel(model: String) {
        this.model = model
    }

    override suspend fun handleMessage(
        systemPrompt: String,
        messages: List<MessageItem>,
        mcpTools: List<Tool>,
        onResponse: (type: ResponseType, message: String) -> Unit,
        onToolCall: suspend (name: String, arguments: Map<String, Any?>) -> CallToolResultBase?
    ) {
        val openAiModel = model ?: throw RuntimeException("OpenAI model is not set")
        // Create tools
        val toolList = mcpTools.map { tool ->
            val toolBuilder = ChatCompletionTool.builder()
            val parameters = FunctionParameters.builder()
                .putAdditionalProperty("type", JsonValue.from(tool.inputSchema.type))
                .putAdditionalProperty("properties", tool.inputSchema.properties.toJsonValue())
                .putAdditionalProperty("required", JsonValue.from(tool.inputSchema.required?: emptyList<String>()))
                .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                .build()
            val function = FunctionDefinition.builder()
                .name(tool.name)
                .description(tool.description ?: "")
                .parameters(parameters)
                .build()
            toolBuilder.function(function).build()
        }

        val chatParamBuilder = ChatCompletionCreateParams.builder()
            .model(openAiModel)
            .maxCompletionTokens(10240)
            .addSystemMessage(systemPrompt) // Register System Prompt

        if (toolList.isNotEmpty()) {
            logd(TAG, "OpenAI tool defined : $toolList")
            chatParamBuilder.tools(toolList)
        } // Register Tool

        // Register User Prompt
        for (message in messages) {
            if (message.type == MessageItem.Type.User) {
                chatParamBuilder.addUserMessage(message.message)
            } else if (message.type == MessageItem.Type.Assistant) {
                chatParamBuilder.addMessage(
                    ChatCompletionAssistantMessageParam.builder()
                        .content(message.message)
                        .build()
                )
            }
        }

        openAiHandleMessage(chatParamBuilder, onResponse, onToolCall)
    }

    private suspend fun openAiHandleMessage(
        chatParamBuilder: ChatCompletionCreateParams.Builder,
        onResponse: (type: ResponseType, message: String) -> Unit,
        onToolCall: suspend (name: String, arguments: Map<String, Any?>) -> CallToolResultBase?
    ) {
        val openAIClient = openAi ?: throw Throwable("OpenAI client is not initialized")

        yield()

        openAIClient.chat().completions().create(chatParamBuilder.build()).choices().stream().consumeAsFlow()
            .map { it.message() }
            .collect { message ->
                logd(TAG, "receive message $message")
                yield()
                chatParamBuilder.addMessage(message)
                message.content().ifPresent { content ->
                    onResponse(ResponseType.Text, content)
                }
                if (message.toolCalls().isPresent) {
                    val toolCalls = message.toolCalls().get()
                    for (toolCall in toolCalls) {
                        yield()
                        val toolName = toolCall.function().name()
                        val toolArgs = JsonValue.from(jsonMapper().readTree(toolCall.function().arguments()))
                            .convert(object : TypeReference<Map<String, JsonValue>>() {})
                        logd(TAG, "Tool use: $toolName with args $toolArgs")
                        val result = onToolCall(toolName, toolArgs ?: emptyMap())
                        logd(TAG, "Tool use: CallTool result = ${result?.content}")
                        yield()
                        val resultContent = result?.content?.joinToString("\n") { (it as TextContent).text ?: "" } ?: "Failed to call tool."
                        chatParamBuilder.addMessage(
                            ChatCompletionToolMessageParam.builder()
                                .toolCallId(toolCall.id())
                                .content(resultContent)
                                .build()
                        )
                    }
                    if (toolCalls.isNotEmpty()) {
                        openAiHandleMessage(chatParamBuilder, onResponse, onToolCall)
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
