package io.groovin.mcpsample.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.groovin.mcpsample.R
import io.groovin.mcpsample.ui.theme.LocalMcpThemeScheme
import io.groovin.mcpsample.ui.theme.MCPSampleTheme
import io.groovin.mcpsample.util.conditional

@Composable
fun IconButton(
    resourceId: Int,
    modifier: Modifier = Modifier,
    style: IconButtonStyle = IconButtonStyle.Basic,
    onClick: () -> Unit = {},
) {
    val iconButtonStyle = LocalMcpThemeScheme.current.iconButtonStyle
    Box(
        modifier = Modifier
            .conditional(style.useBorder) {
                border(0.3.dp, iconButtonStyle.borderColor, CircleShape)
            }
            .background(color = if (style.isHighlight) iconButtonStyle.highlightBackgroundColor else iconButtonStyle.backgroundColor, shape = CircleShape)
            .clip(CircleShape)
            .clickable {
                onClick()
            }
            .padding(style.iconPaddingSize)
            .then(modifier)
    ) {
        Image(
            painter = painterResource(resourceId),
            modifier = Modifier
                .size(style.iconSize),
            colorFilter = ColorFilter.tint(color = if (style.isHighlight) iconButtonStyle.highlightTintColor else iconButtonStyle.tintColor),
            contentDescription = null
        )
    }
}

enum class IconButtonStyle(
    val useBorder: Boolean = false,
    val isHighlight: Boolean = false,
    val iconSize: Dp = 24.dp,
    val iconPaddingSize: Dp = 6.dp
) {
    Basic,
    BasicBorder(true),
    BasicHighlight(isHighlight = true),
    Small(iconSize= 20.dp),
    SmallBorder(iconSize = 20.dp, useBorder = true),
    SmallHighlight(iconSize = 20.dp, isHighlight = true),
    XSmall(iconSize = 16.dp, iconPaddingSize = 4.dp),
    XSmallBorder(iconSize = 16.dp, iconPaddingSize = 4.dp, useBorder = true),
    XSmallHighlight(iconSize = 16.dp, iconPaddingSize = 4.dp, isHighlight = true),
    Large(iconSize = 24.dp),
    LargeBorder(iconSize = 24.dp, useBorder = true),
    LargeHighlight(iconSize = 24.dp, isHighlight = true)
}

@Preview(name = "LightMode", showBackground = true, backgroundColor = 0xFFFFFF)
@Preview(name = "DarkMode", showBackground = true, backgroundColor = 0x000000, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewIconButton() {
    MCPSampleTheme {
        Column {
            Row {
                IconButton(
                    style = IconButtonStyle.Basic,
                    resourceId = R.drawable.icon_settings
                )
                IconButton(
                    style = IconButtonStyle.BasicBorder,
                    resourceId = R.drawable.icon_edit
                )
                IconButton(
                    style = IconButtonStyle.BasicHighlight,
                    resourceId = R.drawable.icon_edit
                )
            }

            Row {
                IconButton(
                    style = IconButtonStyle.Small,
                    resourceId = R.drawable.icon_settings
                )
                IconButton(
                    style = IconButtonStyle.SmallBorder,
                    resourceId = R.drawable.icon_edit
                )
                IconButton(
                    style = IconButtonStyle.SmallHighlight,
                    resourceId = R.drawable.icon_edit
                )
            }
        }
    }
}
