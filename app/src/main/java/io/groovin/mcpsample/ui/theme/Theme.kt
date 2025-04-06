package io.groovin.mcpsample.ui.theme

import android.os.Build
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
import androidx.compose.ui.graphics.Color

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
    val iconButtonStyle: IconButtonStyle
) {
    data class IconButtonStyle(
        val tintColor: Color,
        val highlightTintColor: Color,
        val backgroundColor: Color,
        val highlightBackgroundColor: Color,
        val borderColor: Color
    )
}

private val lightMcpTheme: McpThemeScheme = McpThemeScheme(
    primaryColor = primaryColor,
    surfaceColor = lightSurfaceColor,
    iconButtonStyle = McpThemeScheme.IconButtonStyle(
        tintColor = Color.Black,
        highlightTintColor = Color.White,
        backgroundColor = Color.Transparent,
        highlightBackgroundColor = primaryColor,
        borderColor = Color(0x20000000)
    )
)

private val darkMcpTheme: McpThemeScheme = McpThemeScheme(
    primaryColor = primaryColor,
    surfaceColor = darkSurfaceColor,
    iconButtonStyle = McpThemeScheme.IconButtonStyle(
        tintColor = Color.LightGray,
        highlightTintColor = Color.White,
        backgroundColor = Color.Transparent,
        highlightBackgroundColor = primaryColor,
        borderColor = Color(0x20FFFFFF)
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
