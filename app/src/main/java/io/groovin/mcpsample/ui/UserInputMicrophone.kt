package io.groovin.mcpsample.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.groovin.mcpsample.R
import io.groovin.mcpsample.ui.component.IconButton
import io.groovin.mcpsample.ui.component.IconButtonStyle
import io.groovin.mcpsample.ui.component.Text
import io.groovin.mcpsample.ui.theme.McpTheme
import io.groovin.mcpsample.ui.theme.darkSubTextColor
import io.groovin.mcpsample.ui.theme.lightSubTextColor
import io.groovin.mcpsample.util.MicInputController
import io.groovin.mcpsample.util.MicInputListener
import io.groovin.mcpsample.util.logd
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun UserInputMicrophone(
    visibility: Boolean,
    autoResultMode: Boolean,
    modifier: Modifier = Modifier,
    onCancelClick: () -> Unit = {},
    onChatRequestClick: (String) -> Unit = {},
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = visibility,
        enter = fadeIn() + slideInVertically(initialOffsetY = { height -> height / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { height -> height / 2 })
    ) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val micInputController = remember { MicInputController(context, true) }
        var inputText by remember { mutableStateOf("") }
        var partialInputText by remember { mutableStateOf("") }
        var progressUiState by remember { mutableStateOf(RecordingProgressUiState(persistentListOf(*Array(10) { 0 })))}
        val latestRmsDbList = remember { mutableStateListOf(0) }

        LaunchedEffect(Unit) {
            while(true) {
                delay(50)
                val latestRmsDb = latestRmsDbList.average().toInt()
                latestRmsDbList.clear()
                val newRmsDbList = progressUiState.rmsDbList.toMutableList()
                newRmsDbList.add(0, latestRmsDb)
                newRmsDbList.removeAt(newRmsDbList.lastIndex)
                progressUiState = progressUiState.copy(
                    rmsDbList = newRmsDbList.toImmutableList()
                )
            }
        }

        DisposableEffect(Unit) {
            coroutineScope.launch {
                micInputController.listener = object: MicInputListener {
                    override fun onStart() {}
                    override fun onStop() {}
                    override fun onError(error: Int) {}
                    override fun onPartialResults(partialResults: String) {
                        partialInputText = partialResults
                    }
                    override fun onResult(result: String) {
                        partialInputText = ""
                        inputText = inputText + result

                        if (autoResultMode) {
                            micInputController.stopRecording()
                            coroutineScope.launch {
                                delay(200)
                                onChatRequestClick(inputText)
                            }
                        }
                    }
                    override fun onRmsDb(rmsDB: Int) {
                        latestRmsDbList.add(rmsDB)
                    }
                }
                delay(500)
                micInputController.startRecording()
            }

            onDispose {
                micInputController.stopRecording()
            }
        }


        Row(
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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                resourceId = R.drawable.icon_close,
                style = IconButtonStyle.LargeBorder,
                onClick = onCancelClick
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            ) {
                if (inputText.isBlank() && partialInputText.isBlank()) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "음성으로 입력해 주세요.",
                        style = McpTheme.textStyle.copy(
                            textColor = if (isSystemInDarkTheme()) darkSubTextColor else lightSubTextColor,
                            textAlign = TextAlign.Center
                        )
                    )
                } else {
                    Text(
                        modifier = Modifier.fillMaxWidth().animateContentSize(),
                        text = inputText + partialInputText,
                        style = McpTheme.textStyle.copy(
                            textAlign = TextAlign.Center
                        )
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    RecordingProgress(
                        uiState = progressUiState,
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(32.dp)
                    )
                }
            }
            if (autoResultMode) {
                Spacer(modifier = Modifier.width(36.dp))
            } else {
                IconButton(
                    resourceId = R.drawable.icon_send,
                    style = IconButtonStyle.LargeHighlight,
                    onClick = {
                        onChatRequestClick(inputText)
                    }
                )
            }
        }

        // Handle back press to cancel the microphone input
        BackHandler {
            onCancelClick()
        }
    }
}
