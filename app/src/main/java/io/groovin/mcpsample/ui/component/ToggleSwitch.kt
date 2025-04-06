package io.groovin.mcpsample.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.groovin.mcpsample.ui.theme.McpTheme
import io.groovin.mcpsample.ui.theme.McpThemeScheme

@Composable
fun ToggleSwitch(
    isOn: Boolean,
    modifier: Modifier = Modifier,
    style: McpThemeScheme.ToggleSwitchStyle = McpTheme.toggleSwitchStyle,
    onChanged: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val thumbAlign by animateAlignmentAsState(if (isOn) Alignment.CenterEnd else Alignment.CenterStart)
    val trackColor by animateColorAsState(if (isOn) style.onTrackColor else style.offTrackColor)
    Box(
        modifier = Modifier
            .background(color = trackColor, shape = CircleShape)
            .width(style.trackWidth)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    onChanged()
                }
            )
            .padding(style.thumbPaddingSize)
            .then(modifier)
    ) {
        Spacer(
            modifier = Modifier
                .background(color = style.thumbColor, shape = CircleShape)
                .size(style.thumbSize)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(
                        bounded = false,
                        radius = style.thumbSize * 0.8f,
                        color = Color(0xFF000000)
                    ),
                    onClick = {
                        onChanged()
                    }
                )
                .align(thumbAlign)
        )
    }
}

@Composable
fun animateAlignmentAsState(
    targetAlignment: Alignment,
): State<Alignment> {
    val biased = targetAlignment as BiasAlignment
    val horizontal by animateFloatAsState(biased.horizontalBias)
    val vertical by animateFloatAsState(biased.verticalBias)
    return remember { derivedStateOf { BiasAlignment(horizontal, vertical) } }
}

@Preview
@Composable
private fun PreviewIconButton() {
    Column {
        var s1 by remember { mutableStateOf(true) }
        var s2 by remember { mutableStateOf(false) }

        ToggleSwitch(isOn = s1, onChanged = { s1 = !s1 })
        ToggleSwitch(isOn = s2, onChanged = { s2 = !s2 })
    }
}


