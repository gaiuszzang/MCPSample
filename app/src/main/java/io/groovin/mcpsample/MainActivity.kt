package io.groovin.mcpsample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.groovin.mcpsample.ui.RemoteMcpSettingPopup
import io.groovin.mcpsample.ui.bottomsheet.BottomSheetPopup
import io.groovin.mcpsample.ui.component.InputTextField
import io.groovin.mcpsample.ui.component.RoundButton
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
    val chatUiState by viewModel.chatUiState.collectAsStateWithLifecycle()
    val mcpUiState by viewModel.mcpUiState.collectAsStateWithLifecycle()
    var showApiKeySettingUi by remember { mutableStateOf(false) }
    var showMcpSettingUi by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier)
    ) {
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
                                .padding(16.dp),
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
                onValueChange = {
                    text = it
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                Text(
                    modifier = Modifier
                        .background(color = Color(0xFF808080), shape = RoundedCornerShape(50))
                        .padding(vertical = 6.dp, horizontal = 12.dp)
                        .clickable {
                            showApiKeySettingUi = true
                        },
                    text = "API KEY",
                    fontSize = 12.sp,
                    color = Color.White
                )
                Text(
                    modifier = Modifier
                        .background(color = Color(0xFF808080), shape = RoundedCornerShape(50))
                        .padding(vertical = 6.dp, horizontal = 12.dp)
                        .clickable {
                            showMcpSettingUi = true
                        },
                    text = "MCP Setting",
                    fontSize = 12.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.weight(1f))
                RoundButton(
                    text = "SEND",
                    onClick = {
                        viewModel.chatRequest(text)
                        text = ""
                    }
                )
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

    if (showApiKeySettingUi) {
        BottomSheetPopup(
            onDismissRequest = {
                showApiKeySettingUi = false
            }
        ) {
            ApiKeySettingUi(
                uiState = chatUiState,
                onKeyChanged = {
                    viewModel.updateApiKey(it)
                }
            )
        }
    }
    if (showMcpSettingUi) {
        RemoteMcpSettingPopup(
            uiState = mcpUiState,
            onDismissRequest = {
                showMcpSettingUi = false
            }
        )
    }
}

@Composable
private fun ApiKeySettingUi(
    uiState: ChatUiState,
    onKeyChanged: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "API-KEY : ", fontSize = 14.sp)
            TextField(
                modifier = Modifier.weight(1f),
                value = uiState.apiKey,
                minLines = 4,
                maxLines = 4,
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                onValueChange = onKeyChanged
            )
        }
    }
}

