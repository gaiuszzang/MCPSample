package io.groovin.mcpsample.ui

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import io.groovin.mcpsample.ChatUiState
import io.groovin.mcpsample.InputMode
import io.groovin.mcpsample.R
import io.groovin.mcpsample.mcp.localserver.LocalToolPermissionHandler
import io.groovin.mcpsample.ui.component.IconButton
import io.groovin.mcpsample.ui.component.IconButtonStyle
import io.groovin.mcpsample.ui.component.InputTextField
import io.groovin.mcpsample.ui.component.Text
import io.groovin.mcpsample.ui.theme.McpTheme
import io.groovin.mcpsample.ui.theme.McpThemeScheme
import io.groovin.mcpsample.ui.theme.darkSubTextColor
import io.groovin.mcpsample.ui.theme.lightSubTextColor
import io.groovin.mcpsample.util.MicInputController
import io.groovin.mcpsample.util.MicInputListener
import io.groovin.mcpsample.util.logd
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

@Composable
fun UserInputKeyboard(
    visibility: Boolean,
    isOnHandleChatting: Boolean,
    useHotWord: Boolean,
    hotWordText: String,
    modifier: Modifier = Modifier,
    onMcpToolClick: () -> Unit = {},
    onChatRequestClick: (String) -> Unit = {},
    onChatStopClick: () -> Unit = {},
    onInputModeChangeClick: (InputMode) -> Unit = {},
) {
    // InputKeyboard UI
    AnimatedVisibility(
        modifier = modifier,
        visible = visibility,
        enter = fadeIn() + slideInVertically(initialOffsetY = { height -> height / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { height -> height / 2 })
    ) {
        val context = LocalContext.current
        val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
        var isHotWordTurnOn by remember { mutableStateOf(false) }
        if (useHotWord && hotWordText.isNotBlank() == true && lifecycle == Lifecycle.State.RESUMED) {
            val permHandler = remember { inject<LocalToolPermissionHandler>(clazz = LocalToolPermissionHandler::class.java).value }
            val micInputController = remember { MicInputController(context) }
            val coroutineScope = rememberCoroutineScope()
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
                                val trimResult = result.replace(" ", "")
                                val trimHotWordText = hotWordText.replace(" ", "")
                                logd("UserInputKeyboard", "HotWord Result : User said $trimResult, compare HotWord $trimHotWordText")
                                if (trimResult == trimHotWordText) {
                                    //TODO Haptic
                                    onInputModeChangeClick(InputMode.Microphone)
                                }
                            }
                            override fun onRmsDb(rmsDB: Int) {}
                        }
                        delay(500)
                        micInputController.startRecording()
                        isHotWordTurnOn = true
                    }
                }
                onDispose {
                    micInputController.stopRecording()
                    isHotWordTurnOn = false
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    resourceId = R.drawable.icon_package,
                    style = IconButtonStyle.BasicBorder,
                    onClick = onMcpToolClick
                )
                if (isHotWordTurnOn) {
                    Spacer(modifier = Modifier.weight(1f))
                    AnimatedRainbowText(
                        modifier = Modifier,
                        text = "'$hotWordText'라고 말해 보세요",
                        style = McpTheme.textStyle.copy(
                            //textColor = if (isSystemInDarkTheme()) darkSubTextColor else lightSubTextColor,
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    /*
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "'$hotWordText'라고 말해 보세요",
                        style = McpTheme.textStyle.copy(
                            textColor = if (isSystemInDarkTheme()) darkSubTextColor else lightSubTextColor,
                            textAlign = TextAlign.Center
                        )
                    )*/
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                if (isOnHandleChatting) {
                    IconButton(
                        resourceId = R.drawable.icon_stop,
                        style = IconButtonStyle.BasicBorder,
                        onClick = onChatStopClick
                    )
                } else if (text.isEmpty()) {
                    IconButton(
                        modifier = Modifier,
                        resourceId = R.drawable.icon_mic,
                        style = IconButtonStyle.BasicBorder,
                        onClick = {
                            onInputModeChangeClick(InputMode.Microphone)
                        }
                    )
                } else {
                    Row {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            resourceId = R.drawable.icon_send,
                            style = IconButtonStyle.BasicHighlight,
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

//TODO m.c.shin : Color, start/end offset 다시 계산 필요
@Composable
fun AnimatedRainbowText(
    text: String,
    modifier: Modifier = Modifier,
    style: McpThemeScheme.TextStyle = McpTheme.textStyle,
) {
    var textWidth by remember { mutableIntStateOf(0) }
    val infiniteTransition = rememberInfiniteTransition(label = "OffsetTransition")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RelativeOffset"
    )

    val brush = remember(textWidth, animatedOffset) {
        val width = textWidth.toFloat()
        Brush.linearGradient(
            colors = listOf(Color.Red, Color.Yellow, Color.Red),
            start = Offset(x = width * (animatedOffset - 0.5f), y = 0f),
            end = Offset(x = width * (animatedOffset + 0.5f), y = 0f)
        )
    }
    Text(
        text = text,
        modifier = modifier,
        style = style.copy(
            brush = brush
        ),
        onTextLayout = {
            textWidth = it.size.width
        }
    )
}
