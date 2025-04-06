package io.groovin.mcpsample.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.groovin.mcpsample.ui.theme.darkRecordingProgressBackgroundColor
import io.groovin.mcpsample.ui.theme.darkRecordingProgressBarColor
import io.groovin.mcpsample.ui.theme.lightRecordingProgressBackgroundColor
import io.groovin.mcpsample.ui.theme.lightRecordingProgressBarColor
import kotlinx.collections.immutable.ImmutableList

@Composable
fun RecordingProgress(
    uiState: RecordingProgressUiState,
    modifier: Modifier = Modifier,
    style: RecordingProgressStyle = defaultRecordingProgressStyle()
) {
    var rowHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    Row(
        modifier = modifier
            .background(color = style.backgroundColor, shape = RoundedCornerShape(50))
            .padding(style.contentPadding)
            .onGloballyPositioned { layout ->
                rowHeight = with(density) { layout.size.height.toDp() }
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        uiState.rmsDbList.asReversed().forEach { rmsDb ->
            val barHeight = (style.minBarHeight + (rowHeight - style.minBarHeight) * (rmsDb / 100f)).coerceAtLeast(style.minBarHeight)
            Box(
                modifier = Modifier
                    .width(style.barWidth)
                    .height(barHeight)
                    .background(style.barColor, shape = RoundedCornerShape(100))
            )
        }
    }
}

@Immutable
data class RecordingProgressUiState(
    val rmsDbList: ImmutableList<Int>
)

@Immutable
data class RecordingProgressStyle(
    val barWidth: Dp = 4.dp,
    val minBarHeight: Dp = 4.dp,
    val contentPadding: Dp = 4.dp,
    val barColor: Color = Color.Blue,
    val backgroundColor: Color = Color.Transparent
)

@Composable
private fun defaultRecordingProgressStyle(): RecordingProgressStyle {
    if (isSystemInDarkTheme()) {
        return RecordingProgressStyle(
            barWidth = 4.dp,
            minBarHeight = 6.dp,
            contentPadding = 4.dp,
            barColor = darkRecordingProgressBarColor,
            backgroundColor = darkRecordingProgressBackgroundColor
        )
    } else {
        return RecordingProgressStyle(
            barWidth = 4.dp,
            minBarHeight = 6.dp,
            contentPadding = 4.dp,
            barColor = lightRecordingProgressBarColor,
            backgroundColor = lightRecordingProgressBackgroundColor
        )
    }
}