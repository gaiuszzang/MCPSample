package io.groovin.mcpsample.ui

import android.Manifest
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.groovin.mcpsample.ChatUiState
import io.groovin.mcpsample.InputMode
import io.groovin.mcpsample.R
import io.groovin.mcpsample.mcp.localserver.LocalToolPermissionHandler
import io.groovin.mcpsample.ui.component.IconButton
import io.groovin.mcpsample.ui.component.IconButtonStyle
import io.groovin.mcpsample.ui.component.InputTextField
import io.groovin.mcpsample.ui.theme.McpTheme
import io.groovin.mcpsample.util.MicInputController
import io.groovin.mcpsample.util.MicInputListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

@Composable
fun UserInputKeyboard(
    visibility: Boolean,
    isOnHandleChatting: Boolean,
    hotwordOption: ChatUiState.HotwordOption,
    modifier: Modifier = Modifier,
    onMcpToolClick: () -> Unit = {},
    onChatRequestClick: (String) -> Unit = {},
    onChatStopClick: () -> Unit = {},
    onInputModeChangeClick: (InputMode) -> Unit = {},
) {
    // InputKeyboard UI
    androidx.compose.animation.AnimatedVisibility(
        modifier = modifier,
        visible = visibility,
        enter = fadeIn() + slideInVertically(initialOffsetY = { height -> height / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { height -> height / 2 })
    ) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        //TODO : 이슈
        // UserInputMicrophone 변경될 때 그 안의 MicInputController 와 중복되서 하나가 동작 안되는 이슈 있음
        // 액티비티가 resume일때만 동작해야 하는데 안되는 이슈 있음

        if (hotwordOption.isOn && hotwordOption.hotword?.isNotEmpty() == true) {
            val permHandler = remember { inject<LocalToolPermissionHandler>(clazz = LocalToolPermissionHandler::class.java).value }
            val micInputController = remember { MicInputController(context) }
            DisposableEffect(Unit) {
                coroutineScope.launch {
                    val result = permHandler.request(listOf(Manifest.permission.RECORD_AUDIO))
                    if (result) {
                        micInputController.listener = object: MicInputListener {
                            override fun onStart() {}
                            override fun onStop() {}
                            override fun onError(error: Int) {}
                            override fun onPartialResults(partialResults: String) {}
                            override fun onResult(result: String) {
                                if (result == hotwordOption.hotword) {
                                    onInputModeChangeClick(InputMode.Microphone)
                                }
                            }
                            override fun onRmsDb(rmsDB: Int) {}
                        }
                    }
                    micInputController.startRecording()
                }
                onDispose {
                    micInputController.stopRecording()
                }
            }
        }


        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = McpTheme.surfaceColor,
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 0.3.dp,
                    color = McpTheme.borderColor,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(12.dp)
        ) {
            var text by remember { mutableStateOf("") }
            InputTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .animateContentSize(),
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
                IconButton(
                    resourceId = R.drawable.icon_package,
                    style = IconButtonStyle.SmallBorder,
                    onClick = onMcpToolClick
                )
                Spacer(modifier = Modifier.weight(1f))
                if (isOnHandleChatting) {
                    IconButton(
                        resourceId = R.drawable.icon_stop,
                        style = IconButtonStyle.SmallBorder,
                        onClick = onChatStopClick
                    )
                } else if (text.isEmpty()) {
                    IconButton(
                        modifier = Modifier,
                        resourceId = R.drawable.icon_mic,
                        style = IconButtonStyle.SmallBorder,
                        onClick = {
                            onInputModeChangeClick(InputMode.Microphone)
                        }
                    )
                } else {
                    Row {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            resourceId = R.drawable.icon_send,
                            style = IconButtonStyle.SmallHighlight,
                            onClick = {
                                onChatRequestClick(text)
                                text = ""
                            }
                        )
                    }

                }
            }
        }
    }
}
