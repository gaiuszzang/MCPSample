package io.groovin.mcpsample.ui

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import io.groovin.mcpsample.InputMode
import io.groovin.mcpsample.R
import io.groovin.mcpsample.mcp.localserver.LocalToolPermissionHandler
import io.groovin.mcpsample.ui.component.IconButton
import io.groovin.mcpsample.ui.component.IconButtonStyle
import io.groovin.mcpsample.ui.component.InputTextField
import io.groovin.mcpsample.ui.component.Text
import io.groovin.mcpsample.ui.theme.McpTheme
import io.groovin.mcpsample.ui.theme.McpThemeScheme
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
        if (useHotWord && hotWordText.isNotBlank() && lifecycle == Lifecycle.State.RESUMED) {
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
                                val trimResult = result.lowercase().replace(" ", "")
                                val trimHotWordTextList = hotWordText.lowercase().split(",", "|").map { it.replace(" ", "") }
                                logd("UserInputKeyboard", "HotWord Result : User said $trimResult, compare HotWord $trimHotWordTextList")
                                if (trimHotWordTextList.contains(trimResult)) {
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
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(modifier = Modifier.weight(1f))
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
    val infiniteTransition = rememberInfiniteTransition(label = "BrushTransition")

// 애니메이션 진행 상태 (0f ~ 1f)
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ProgressAnimation"
    )

    // Brush 생성
    val animatedBrush = Brush.linearGradient(
        colors = lerpColors(
            listOf(Color(0xFF4264f5), Color(0xFF6a58d2), Color(0xFFe24f8e)),
            listOf(Color(0xFFe24f8e), Color(0xFF4264f5), Color(0xFF6a58d2)),
            progress
        ),
        start = Offset.Zero,
        end = Offset.Infinite
    )
    Text(
        text = text,
        modifier = modifier,
        style = style.copy(
            brush = animatedBrush
        )
    )
}
// 두 색상 리스트를 보간하는 함수
fun lerpColors(startColors: List<Color>, endColors: List<Color>, progress: Float): List<Color> {
    return startColors.zip(endColors) { start, end ->
        androidx.compose.ui.graphics.lerp(start, end, progress)
    }
}