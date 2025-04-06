package io.groovin.mcpsample.llm

import io.groovin.mcpsample.llm.LLMManager.MessageItem
import io.modelcontextprotocol.kotlin.sdk.CallToolResultBase
import io.modelcontextprotocol.kotlin.sdk.Tool

interface LLM {
    fun setApiKey(apiKey: String)
    fun setModel(model: String)
    suspend fun handleMessage(
        systemPrompt: String,
        messages: List<MessageItem>,
        mcpTools: List<Tool>,
        onResponse: (type: ResponseType, response: String) -> Unit,
        onToolCall: suspend (name: String, arguments: Map<String, Any?>) -> CallToolResultBase?
    )
}

enum class ResponseType {
    Text,
    Tool,
    Thinking,
    System
}
