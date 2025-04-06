package io.groovin.mcpsample.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.halilibo.richtext.commonmark.CommonMarkdownParseOptions
import com.halilibo.richtext.commonmark.CommonmarkAstNodeParser
import com.halilibo.richtext.markdown.BasicMarkdown
import com.halilibo.richtext.ui.BasicRichText
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.RichTextThemeProvider
import io.groovin.mcpsample.ui.theme.McpTheme
import io.groovin.mcpsample.ui.theme.McpThemeScheme

//TODO m.c.shin : Add styles
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    textStyle: McpThemeScheme.TextStyle = McpTheme.textStyle,
    richTextStyle: RichTextStyle = McpTheme.richTextStyle,
) {
    //TODO m.c.shin : RichTextStyle 추후 업뎃 필요
    RichTextThemeProvider(
        textStyleProvider = {
                TextStyle.Default.copy(
                color = textStyle.textColor,
                fontSize = textStyle.fontSize,
                fontWeight = textStyle.fontWeight,
                textAlign = textStyle.textAlign
            )
        }
    ) {
        BasicRichText(
            modifier = modifier,
            style = richTextStyle
        ) {
            // requires richtext-commonmark module.
            val parser = remember { CommonmarkAstNodeParser(CommonMarkdownParseOptions(true)) }
            val markDownNode = remember(parser, text) { parser.parse(text.trimIndent()) }
            BasicMarkdown(markDownNode)
        }
    }
}

