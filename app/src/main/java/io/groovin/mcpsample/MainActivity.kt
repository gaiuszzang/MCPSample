package io.groovin.mcpsample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.groovin.mcpsample.ui.CommonSettingPopup
import io.groovin.mcpsample.ui.LLMSelectPopup
import io.groovin.mcpsample.ui.RemoteMcpSettingPopup
import io.groovin.mcpsample.ui.component.IconButton
import io.groovin.mcpsample.ui.component.IconButtonStyle
import io.groovin.mcpsample.ui.component.InputTextField
import io.groovin.mcpsample.ui.theme.MCPSampleTheme
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel = koinViewModel<MainViewModel>()
            MCPSampleTheme {
                MCPHome(
                    modifier = Modifier.safeContentPadding(),
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun MCPHome(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel
) {
    val llmSelectUiState by viewModel.llmSelectUiState.collectAsStateWithLifecycle()
    val chatUiState by viewModel.chatUiState.collectAsStateWithLifecycle()
    val settingUiState by viewModel.settingUiState.collectAsStateWithLifecycle()
    val mcpUiState by viewModel.mcpUiState.collectAsStateWithLifecycle()
    var showLLMSelectPopup by remember { mutableStateOf(false) }
    var showSettingPopup by remember { mutableStateOf(false) }
    var showMcpToolPopup by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier)
    ) {
        // AppTopBar
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val selectedLLMItemName = llmSelectUiState.llmItemList.firstOrNull { it.isSelected }?.llmType?.llmName ?: "No Selected"
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(50))
                    .clickable {
                        showLLMSelectPopup = true
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = selectedLLMItemName
                )
                IconButton(
                    resourceId = R.drawable.icon_dropdown,
                    style = IconButtonStyle.XSmall
                ) {
                    showLLMSelectPopup = true
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                resourceId = R.drawable.icon_settings,
                onClick = {
                    showSettingPopup = true
                }
            )
        }

        val scrollState = rememberLazyListState()
        LaunchedEffect(chatUiState.messageList) {
            // Auto-scroll to the bottom of the message list when new messages are added
            scrollState.animateScrollToItem(chatUiState.messageList.size)
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            state = scrollState
        ) {
            items(chatUiState.messageList) { message ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    if (message.isUser) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(16.dp)
                                .background(
                                    color = Color(0xA0000000),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(vertical = 8.dp, horizontal = 16.dp),
                            text = message.message,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    } else {
                        Text(
                            text = message.message,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxWidth()
                                .padding(8.dp),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color.White, shape = RoundedCornerShape(16.dp))
                .border(width = 0.3.dp, color = Color(0x20000000), shape = RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            var text by remember { mutableStateOf("") }
            InputTextField(
                modifier = Modifier.fillMaxWidth(),
                value = text,
                enabled = !chatUiState.isOnHandleChatting,
                onValueChange = {
                    text = it
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                IconButton(
                    resourceId = R.drawable.icon_package,
                    style = IconButtonStyle.SmallBorder,
                    onClick = {
                        showMcpToolPopup = true
                    }
                )
                Spacer(modifier = Modifier.weight(1f))
                if (chatUiState.isOnHandleChatting) {
                    IconButton(
                        resourceId = R.drawable.icon_stop,
                        style = IconButtonStyle.SmallBorder,
                        onClick = {
                            viewModel.cancelChatRequest()
                        }
                    )
                } else {
                    IconButton(
                        resourceId = R.drawable.icon_send,
                        style = IconButtonStyle.SmallHighlight,
                        onClick = {
                            viewModel.chatRequest(text)
                            text = ""
                        }
                    )
                }
            }
        }
    }

    if (chatUiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }

    if (showLLMSelectPopup) {
        LLMSelectPopup(
            uiState = llmSelectUiState,
            onDismissRequest = {
                showLLMSelectPopup = false
            }
        )
    }
    if (showSettingPopup) {
        CommonSettingPopup(
            uiState = settingUiState,
            onDismissRequest = {
                showSettingPopup = false
            }
        )
    }
    if (showMcpToolPopup) {
        RemoteMcpSettingPopup(
            uiState = mcpUiState,
            onDismissRequest = {
                showMcpToolPopup = false
            }
        )
    }
}
