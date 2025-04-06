package io.groovin.mcpsample.ui.component

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import io.groovin.mcpsample.ui.theme.McpTheme
import io.groovin.mcpsample.ui.theme.McpThemeScheme

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    style: McpThemeScheme.TextStyle = McpTheme.textStyle,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
) {

    BasicText(
        text = text,
        modifier = modifier,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = TextStyle.Default.copy(
            color = style.textColor,
            fontSize = style.fontSize,
            fontWeight = style.fontWeight,
            textAlign = style.textAlign
        )
    )
}