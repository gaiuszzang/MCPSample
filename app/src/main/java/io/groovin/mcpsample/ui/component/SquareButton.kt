package io.groovin.mcpsample.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.groovin.mcpsample.ui.theme.McpTheme
import io.groovin.mcpsample.ui.theme.McpThemeScheme

@Composable
fun SquareButton(
    text: String,
    modifier: Modifier = Modifier,
    buttonStyle: McpThemeScheme.ButtonStyle = McpTheme.buttonStyle,
    onClick: () -> Unit = {}
) {
    BasicText(
        modifier = modifier
            .background(
                color = buttonStyle.buttonColor
            )
            .clickable {
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        text = text,
        style = TextStyle.Default.copy(
            textAlign = TextAlign.Center,
            fontSize = buttonStyle.fontSize,
            color = buttonStyle.textColor
        )
    )
}

@Preview
@Composable
private fun SquareButtonPreview() {
    Column {
        SquareButton(
            text = "On",
            onClick = {}
        )
    }
}