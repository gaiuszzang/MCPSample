package io.groovin.mcpsample.llm

import io.modelcontextprotocol.kotlin.sdk.CallToolResultBase
import io.modelcontextprotocol.kotlin.sdk.Tool

interface LLM {
    fun setApiKey(apiKey: String)
    fun setModel(model: String)
    suspend fun handleMessage(
        systemPrompt: String,
        message: String,
        mcpTools: List<Tool>,
        onResponse: (type: ResponseType, response: String) -> Unit,
        onToolCall: suspend (name: String, arguments: Map<String, Any?>) -> CallToolResultBase?
    )
}

enum class ResponseType {
    Text,
    Thinking,
    System
}