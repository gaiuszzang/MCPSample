package io.groovin.mcpsample.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.groovin.mcpsample.R
import io.groovin.mcpsample.SettingUiState
import io.groovin.mcpsample.ui.bottomsheet.BottomSheetPopup
import io.groovin.mcpsample.ui.component.IconButton
import io.groovin.mcpsample.ui.component.IconButtonStyle
import io.groovin.mcpsample.ui.component.InputTextField
import io.groovin.mcpsample.ui.component.Text
import io.groovin.mcpsample.ui.component.ToggleSwitch
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
                SettingTitle(text = "Claude API-KEY")
                SecretInputTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.claudeApiKey,
                    onValueChange = uiState.onClaudeApiKeyUpdate
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            item(key = "gemini_api_key") {
                SettingTitle(text = "Gemini API-KEY")
                SecretInputTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.geminiApiKey,
                    onValueChange = uiState.onGeminiApiKeyUpdate
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            item(key = "openai_api_key") {
                SettingTitle(text = "OpenAI API-KEY")
                SecretInputTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.openAiApiKey,
                    onValueChange = uiState.onOpenAiApiKeyUpdate
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            item(key = "system_prompt") {
                SettingTitle(text = "System Prompt")
                InputTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = McpTheme.inputTextFieldBackgroundColor, shape = RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    value = uiState.systemPrompt,
                    minLines = 5,
                    maxLines = 5,
                    textStyle = McpTheme.textStyle.copy(fontSize = 14.sp),
                    onValueChange = uiState.onSystemPromptUpdate
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            item(key = "memory") {
                SettingTitle(text = "Memory")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "Use Memory",
                        style = McpTheme.textStyle.copy(
                            fontSize = 16.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    ToggleSwitch(
                        isOn = uiState.memoryEnabled,
                        onChanged = {
                            uiState.onMemoryEnabled(!uiState.memoryEnabled)
                        }
                    )
                }
                InputTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = McpTheme.inputTextFieldBackgroundColor, shape = RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    value = uiState.memoryContext,
                    minLines = 5,
                    maxLines = 5,
                    textStyle = McpTheme.textStyle.copy(fontSize = 14.sp),
                    onValueChange = uiState.onMemoryContextUpdate
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            item(key = "hotword") {
                SettingTitle(text = "Hotword")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InputTextField(
                        modifier = Modifier
                            .weight(1f)
                            .background(color = McpTheme.inputTextFieldBackgroundColor, shape = RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        enabled = uiState.hotWordEnabled,
                        value = uiState.hotWordText,
                        singleLine = true,
                        textStyle = McpTheme.textStyle.copy(fontSize = 14.sp),
                        onValueChange = uiState.onHotWordUpdate
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    ToggleSwitch(
                        isOn = uiState.hotWordEnabled,
                        onChanged = {
                            uiState.onHotWordEnabled(!uiState.hotWordEnabled)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            item(key = "textToSpeech") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "Text To Speech",
                        style = McpTheme.textStyle.copy(
                            fontSize = 16.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    ToggleSwitch(
                        isOn = uiState.textToSpeechEnabled,
                        onChanged = {
                            uiState.onTextToSpeechEnabled(!uiState.textToSpeechEnabled)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            item(key = "asrAutoResultMode") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "ASR Auto Result Mode",
                        style = McpTheme.textStyle.copy(
                            fontSize = 16.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    ToggleSwitch(
                        isOn = uiState.asrAutoResultModeEnabled,
                        onChanged = {
                            uiState.onAsrAutoResultModeEnabled(!uiState.asrAutoResultModeEnabled)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            item(key = "autoEnableMcpConnection") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "MCP Auto Enable",
                        style = McpTheme.textStyle.copy(
                            fontSize = 16.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    ToggleSwitch(
                        isOn = uiState.autoEnableMcpConnection,
                        onChanged = {
                            uiState.onAutoEnableMcpConnection(!uiState.autoEnableMcpConnection)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SettingTitle(
    text: String
) {
    Text(
        modifier = Modifier.padding(vertical = 2.dp),
        text = text
    )
}

@Composable
private fun SecretInputTextField(
    modifier: Modifier,
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        var isLocked by remember { mutableStateOf(true) }
        InputTextField(
            modifier = Modifier
                .weight(1f)
                .background(color = McpTheme.inputTextFieldBackgroundColor, shape = RoundedCornerShape(8.dp))
                .padding(8.dp),
            enabled = !isLocked,
            value = value,
            singleLine = true,
            textStyle = McpTheme.textStyle.copy(fontSize = 14.sp),
            visualTransformation = if (isLocked) PasswordVisualTransformation() else VisualTransformation.None,
            onValueChange = onValueChange
        )
        Spacer(modifier = Modifier.width(6.dp))
        IconButton(
            resourceId = if (isLocked) R.drawable.icon_lock else R.drawable.icon_unlock,
            style = if (isLocked) IconButtonStyle.SmallBorder else IconButtonStyle.SmallHighlight,
            onClick = {
                isLocked = !isLocked
            }
        )
    }
}
