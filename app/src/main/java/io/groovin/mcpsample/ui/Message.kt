package io.groovin.mcpsample.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.groovin.mcpsample.ChatUiState
import io.groovin.mcpsample.R
import io.groovin.mcpsample.ui.component.MarkdownText
import io.groovin.mcpsample.ui.component.Text
import io.groovin.mcpsample.ui.theme.MCPSampleTheme
import io.groovin.mcpsample.ui.theme.McpTheme
import io.groovin.mcpsample.ui.theme.darkChatBubbleColor
import io.groovin.mcpsample.ui.theme.darkChatSubBubbleColor
import io.groovin.mcpsample.ui.theme.darkSubTextColor
import io.groovin.mcpsample.ui.theme.lightChatBubbleColor
import io.groovin.mcpsample.ui.theme.lightChatSubBubbleColor
import io.groovin.mcpsample.ui.theme.lightSubTextColor

@Composable
fun Message(
    message: ChatUiState.Message,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
    ) {
        when (message) {
            is ChatUiState.Message.User -> {
                UserMessage(
                    message = message,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                )
            }

            is ChatUiState.Message.Agent -> {
                AgentMessage(
                    message = message,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                )
            }

            is ChatUiState.Message.System -> {
                SystemMessage(
                    message = message,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                )
            }

            is ChatUiState.Message.Thinking -> {
                ThinkingMessage(
                    message = message,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                )
            }

            is ChatUiState.Message.Tool -> {
                ToolsMessage(
                    message = message,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                )
            }
        }
    }
}

@Composable
fun UserMessage(
    message: ChatUiState.Message.User,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                color = if (isSystemInDarkTheme()) darkChatBubbleColor else lightChatBubbleColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(vertical = 8.dp, horizontal = 16.dp),
        text = message.message,
        style = McpTheme.textStyle.copy(fontSize = 14.sp)
    )
}


@Composable
fun AgentMessage(
    message: ChatUiState.Message.Agent,
    modifier: Modifier = Modifier
) {
    //TODO m.c.shin : styles
    MarkdownText(
        text = message.message,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
    /*
    Text(
        text = message.message,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        style = McpTheme.textStyle.copy(fontSize = 14.sp)
    )*/
}

//TODO
@Composable
fun SystemMessage(
    message: ChatUiState.Message.System,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                color = Color.Red, //if (isSystemInDarkTheme()) darkChatBubbleColor else lightChatBubbleColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(vertical = 4.dp, horizontal = 16.dp),
        text = message.message,
        style = McpTheme.textStyle.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textColor = Color.White
        )
    )
}


//TODO
@Composable
fun ThinkingMessage(
    message: ChatUiState.Message.Thinking,
    modifier: Modifier = Modifier
) {
    Text(
        text = message.message,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        style = McpTheme.textStyle.copy(
            fontSize = 12.sp,
            textColor = if (isSystemInDarkTheme()) darkSubTextColor else lightSubTextColor
        )
    )
}

//TODO
@Composable
fun ToolsMessage(
    message: ChatUiState.Message.Tool,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                color = if (isSystemInDarkTheme()) darkChatSubBubbleColor else lightChatSubBubbleColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(shape = RoundedCornerShape(12.dp))
            .clickable {
                message.onClickExpand()
            }
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(if (message.isExpand) R.drawable.icon_upward else R.drawable.icon_dropdown),
                modifier = Modifier
                    .size(14.dp),
                colorFilter = ColorFilter.tint(color = if (isSystemInDarkTheme()) darkSubTextColor else lightSubTextColor),
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Tool Calls..",
                modifier = modifier
                    .fillMaxWidth(),
                style = McpTheme.textStyle.copy(
                    fontSize = 12.sp,
                    textColor = if (isSystemInDarkTheme()) darkSubTextColor else lightSubTextColor
                )
            )
        }
        if (message.isExpand) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message.message,
                modifier = modifier
                    .fillMaxWidth(),
                style = McpTheme.textStyle.copy(
                    fontSize = 12.sp,
                    textColor = if (isSystemInDarkTheme()) darkSubTextColor else lightSubTextColor
                )
            )
        }
    }
}

@Preview(name = "LightMode", showBackground = true, backgroundColor = 0xFFFFFF)
@Preview(name = "DarkMode", showBackground = true, backgroundColor = 0x000000, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MessagePreview() {
    MCPSampleTheme {
        Column(modifier = Modifier.fillMaxWidth()) {
            Message(
                modifier = Modifier.fillMaxWidth(),
                message = ChatUiState.Message.User(id = "1", "오늘 날씨 어때?")
            )
            Message(
                modifier = Modifier.fillMaxWidth(),
                message = ChatUiState.Message.Thinking(id = "2", "(질문 파악 중)")
            )
            Message(
                modifier = Modifier.fillMaxWidth(),
                message = ChatUiState.Message.Agent(id = "3", "날씨 알아볼게요!")
            )
            Message(
                modifier = Modifier.fillMaxWidth(),
                message = ChatUiState.Message.Tool(id = "4", "toolCall: get_weather()")
            )
            Message(
                modifier = Modifier.fillMaxWidth(),
                message = ChatUiState.Message.Tool(id = "4", "toolCall: get_weather()", isExpand = true)
            )
            Message(
                modifier = Modifier.fillMaxWidth(),
                message = ChatUiState.Message.System(id = "5", "NetworkException")
            )
        }
    }
}
