package io.groovin.mcpsample.llm

import com.anthropic.models.messages.Model
import com.openai.models.ChatModel
import io.modelcontextprotocol.kotlin.sdk.CallToolResultBase
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LLMManager {
    private val claudeLLM = ClaudeLLM()
    private val openAiLLM = OpenAiLLM()
    private val geminiLLM = GeminiLLM()
    private val _selectedLLMType = MutableStateFlow(LLMType.getDefault())
    val selectedLLMType = _selectedLLMType.asStateFlow()
    private val activeLLM: LLM
        get() {
            return when (selectedLLMType.value.llmService) {
                LLMService.Claude -> claudeLLM
                LLMService.Gemini -> geminiLLM
                LLMService.OpenAi -> openAiLLM
            }
        }

    fun setLLM(llmType: LLMType) {
        _selectedLLMType.update { llmType }
        activeLLM.setModel(llmType.modelName)
    }

    fun setApiKey(llmService: LLMService, apiKey: String) {
        when (llmService) {
            LLMService.Claude -> claudeLLM.setApiKey(apiKey)
            LLMService.Gemini -> geminiLLM.setApiKey(apiKey)
            LLMService.OpenAi -> openAiLLM.setApiKey(apiKey)
        }
    }

    suspend fun handleMessage(
        systemPrompt: String,
        message: String,
        mcpTools: List<Tool>,
        onResponse: (type: ResponseType, response: String) -> Unit,
        onToolCall: suspend (name: String, arguments: Map<String, Any?>) -> CallToolResultBase?
    ) {
        activeLLM.handleMessage(systemPrompt, message, mcpTools, onResponse, onToolCall)
    }



    enum class LLMService {
        Claude, Gemini, OpenAi
    }

    enum class LLMType(
        val llmName: String,
        val llmService: LLMService,
        val modelName: String
    ) {
        Claude_3_5_Sonnet(llmName = "Claude 3.5 Sonnet", llmService = LLMService.Claude, modelName = Model.CLAUDE_3_5_SONNET_LATEST.asString()),
        Claude_3_7_Sonnet(llmName = "Claude 3.7 Sonnet", llmService = LLMService.Claude, modelName = Model.CLAUDE_3_7_SONNET_LATEST.asString()),
        Gemini_1_5_Flash(llmName = "Gemini 1.5 Flash", llmService = LLMService.Gemini, modelName = "gemini-1.5-flash-latest"),
        Gemini_2_0_Flash_Light(llmName = "Gemini 2.0 Flash-Lite", llmService = LLMService.Gemini, modelName = "gemini-2.0-flash-lite"),
        Gemini_2_0_Flash(llmName = "Gemini 2.0 Flash", llmService = LLMService.Gemini, modelName = "gemini-2.0-flash"),
        Chat_GPT_4O(llmName = "Chat GPT 4o", llmService = LLMService.OpenAi, modelName = ChatModel.GPT_4O.asString()),
        Chat_GPT_4O_Mini(llmName = "Chat GPT 4o mini", llmService = LLMService.OpenAi, modelName = ChatModel.GPT_4O_MINI.asString());

        companion object {
            fun getDefault(): LLMType = Claude_3_5_Sonnet
        }
    }
}