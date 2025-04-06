package io.groovin.mcpsample.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.groovin.mcpsample.SettingUiState
import io.groovin.mcpsample.ui.bottomsheet.BottomSheetPopup
import io.groovin.mcpsample.ui.component.InputTextField
import io.groovin.mcpsample.ui.component.Text
import io.groovin.mcpsample.ui.theme.McpTheme

@Composable
fun CommonSettingPopup(
    uiState: SettingUiState,
    onDismissRequest: () -> Unit
) {
    BottomSheetPopup(
        onDismissRequest = {
            onDismissRequest()
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            item(key = "claude_api_key") {
                Text(text = "Claude API-KEY")
                InputTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = McpTheme.inputTextFieldBackgroundColor, shape = RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    value = uiState.claudeApiKey,
                    minLines = 4,
                    maxLines = 4,
                    textStyle = McpTheme.textStyle.copy(fontSize = 14.sp),
                    onValueChange = uiState.onClaudeApiKeyUpdate
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            item(key = "gemini_api_key") {
                Text(text = "Gemini API-KEY")
                InputTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = McpTheme.inputTextFieldBackgroundColor, shape = RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    value = uiState.geminiApiKey,
                    minLines = 4,
                    maxLines = 4,
                    textStyle = McpTheme.textStyle.copy(fontSize = 14.sp),
                    onValueChange = uiState.onGeminiApiKeyUpdate
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            item(key = "openai_api_key") {
                Text(text = "OpenAI API-KEY")
                InputTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = McpTheme.inputTextFieldBackgroundColor, shape = RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    value = uiState.openAiApiKey,
                    minLines = 4,
                    maxLines = 4,
                    textStyle = McpTheme.textStyle.copy(fontSize = 14.sp),
                    onValueChange = uiState.onOpenAiApiKeyUpdate
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            item(key = "system_prompt") {
                Text(text = "System Prompt")
                InputTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = McpTheme.inputTextFieldBackgroundColor, shape = RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    value = uiState.systemPrompt,
                    minLines = 4,
                    maxLines = 4,
                    textStyle = McpTheme.textStyle.copy(fontSize = 14.sp),
                    onValueChange = uiState.onSystemPromptUpdate
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
