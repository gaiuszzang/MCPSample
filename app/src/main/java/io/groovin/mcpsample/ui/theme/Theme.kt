package io.groovin.mcpsample.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.halilibo.richtext.ui.CodeBlockStyle
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.TableStyle
import com.halilibo.richtext.ui.string.RichTextStringStyle

private val DarkColorScheme = darkColorScheme(
    primary = primaryColor
)

private val LightColorScheme = lightColorScheme(
    primary = primaryColor
)

@Immutable
data class McpThemeScheme(
    val primaryColor: Color,
    val surfaceColor: Color,
    val borderColor: Color,
    val dividerColor: Color,
    val inputTextFieldBackgroundColor: Color,
    val textStyle: TextStyle,
    val richTextStyle: RichTextStyle,
    val buttonStyle: ButtonStyle,
    val iconButtonStyle: IconButtonStyle,
    val toggleSwitchStyle: ToggleSwitchStyle
) {
    data class IconButtonStyle(
        val tintColor: Color,
        val highlightTintColor: Color,
        val backgroundColor: Color,
        val highlightBackgroundColor: Color,
        val borderColor: Color
    )
    data class TextStyle(
        val textColor: Color,
        val fontSize: TextUnit,
        val fontWeight: FontWeight,
        val textAlign: TextAlign,
        val brush: Brush? = null
    )
    data class ButtonStyle(
        val buttonColor: Color,
        val textColor: Color,
        val fontSize: TextUnit,
        val fontWeight: FontWeight
    )
    data class ToggleSwitchStyle(
        val onTrackColor: Color,
        val offTrackColor: Color,
        val trackWidth: Dp = 48.dp,
        val thumbColor: Color = Color.White,
        val thumbSize: Dp = 20.dp,
        val thumbPaddingSize: Dp = 3.dp,
    )
}

private val lightMcpTheme: McpThemeScheme = McpThemeScheme(
    primaryColor = primaryColor,
    surfaceColor = lightSurfaceColor,
    borderColor = lightBorderColor,
    dividerColor = lightDividerColor,
    inputTextFieldBackgroundColor = lightInputTextFieldBackgroundColor,
    textStyle = McpThemeScheme.TextStyle(
        textColor = lightTextColor,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        textAlign = TextAlign.Start
    ),
    //TODO m.c.shin richTextStyle
    richTextStyle = RichTextStyle(
        headingStyle = { level, textStyle ->
            when (level) {
                0 -> TextStyle.Default.copy(color = lightTextColor, fontWeight = FontWeight.Medium, fontSize = 30.sp)
                1 -> TextStyle.Default.copy(color = lightTextColor, fontWeight = FontWeight.Medium, fontSize = 28.sp)
                2 -> TextStyle.Default.copy(color = lightTextColor, fontWeight = FontWeight.Medium, fontSize = 24.sp)
                3 -> TextStyle.Default.copy(color = lightTextColor, fontWeight = FontWeight.Medium, fontSize = 22.sp)
                4 -> TextStyle.Default.copy(color = lightTextColor, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                5 -> TextStyle.Default.copy(color = lightTextColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                else -> TextStyle.Default.copy(color = lightTextColor, fontWeight = FontWeight.Normal, fontSize = 16.sp)
            }
        },
        tableStyle = TableStyle(
            borderColor = lightTableBorderColor
        ),
        stringStyle = RichTextStringStyle(
            linkStyle = TextLinkStyles()
        ),
        codeBlockStyle = CodeBlockStyle()
    ),
    buttonStyle = McpThemeScheme.ButtonStyle(
        buttonColor = lightButtonColor,
        textColor = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal
    ),
    iconButtonStyle = McpThemeScheme.IconButtonStyle(
        tintColor = Color.Black,
        highlightTintColor = Color.White,
        backgroundColor = Color.Transparent,
        highlightBackgroundColor = primaryColor,
        borderColor = lightBorderColor
    ),
    toggleSwitchStyle = McpThemeScheme.ToggleSwitchStyle(
        onTrackColor = lightToggleSwitchTrackOnColor,
        offTrackColor = lightToggleSwitchTrackOffColor,
        thumbColor = lightToggleSwitchThumbColor
    )
)

private val darkMcpTheme: McpThemeScheme = McpThemeScheme(
    primaryColor = primaryColor,
    surfaceColor = darkSurfaceColor,
    borderColor = darkBorderColor,
    dividerColor = darkDividerColor,
    inputTextFieldBackgroundColor = darkInputTextFieldBackgroundColor,
    textStyle = McpThemeScheme.TextStyle(
        textColor = darkTextColor,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        textAlign = TextAlign.Start
    ),
    //TODO m.c.shin richTextStyle
    richTextStyle = RichTextStyle(
        headingStyle = { level, textStyle ->
            when (level) {
                0 -> TextStyle.Default.copy(color = darkTextColor, fontWeight = FontWeight.Medium, fontSize = 30.sp)
                1 -> TextStyle.Default.copy(color = darkTextColor, fontWeight = FontWeight.Medium, fontSize = 28.sp)
                2 -> TextStyle.Default.copy(color = darkTextColor, fontWeight = FontWeight.Medium, fontSize = 24.sp)
                3 -> TextStyle.Default.copy(color = darkTextColor, fontWeight = FontWeight.Medium, fontSize = 22.sp)
                4 -> TextStyle.Default.copy(color = darkTextColor, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                5 -> TextStyle.Default.copy(color = darkTextColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                else -> TextStyle.Default.copy(color = darkTextColor, fontWeight = FontWeight.Normal, fontSize = 16.sp)
            }
        },
        tableStyle = TableStyle(
            borderColor = darkTableBorderColor
        ),
        stringStyle = RichTextStringStyle(
            linkStyle = TextLinkStyles()
        ),
        codeBlockStyle = CodeBlockStyle()
    ),
    buttonStyle = McpThemeScheme.ButtonStyle(
        buttonColor = darkButtonColor,
        textColor = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal
    ),
    iconButtonStyle = McpThemeScheme.IconButtonStyle(
        tintColor = Color.LightGray,
        highlightTintColor = Color.White,
        backgroundColor = Color.Transparent,
        highlightBackgroundColor = primaryColor,
        borderColor = darkBorderColor
    ),
    toggleSwitchStyle = McpThemeScheme.ToggleSwitchStyle(
        onTrackColor = darkToggleSwitchTrackOnColor,
        offTrackColor = darkToggleSwitchTrackOffColor,
        thumbColor = darkToggleSwitchThumbColor
    )
)


internal val LocalMcpThemeScheme = staticCompositionLocalOf { lightMcpTheme }

val McpTheme: McpThemeScheme
    @Composable
    get() = LocalMcpThemeScheme.current

@Composable
fun MCPSampleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val mcpThemeScheme: McpThemeScheme = if (darkTheme) darkMcpTheme else lightMcpTheme
    CompositionLocalProvider(
        LocalMcpThemeScheme provides mcpThemeScheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = McpTheme.surfaceColor)
                ) {
                    content()
                }
            }
        )
    }
}
