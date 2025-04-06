package io.groovin.mcpsample.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

@Composable
fun ToggleButton(
    text: String,
    isChecked: Boolean,
    modifier: Modifier = Modifier,
    checkedTextColor: Color = Color.White,
    unCheckedTextColor: Color = Color.White,
    fontSize: TextUnit = TextUnit.Unspecified,
    checkedButtonColor: Color = Color(0xFF0050CC),
    unCheckedButtonColor: Color = Color(0xFF505050),
    onClick: () -> Unit = {}
) {
    Text(
        modifier = modifier
            .background(
                color = if (isChecked) checkedButtonColor else unCheckedButtonColor,
                shape = RoundedCornerShape(50)
            )
            .clip(RoundedCornerShape(50))
            .clickable {
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        text = text,
        textAlign = TextAlign.Center,
        fontSize = fontSize,
        color = if (isChecked) checkedTextColor else unCheckedTextColor
    )
}

@Preview
@Composable
private fun ToggleButtonPreview() {
    Column {
        ToggleButton(
            text = "On",
            isChecked = true,
            onClick = {}
        )
        ToggleButton(
            text = "Off",
            isChecked = false,
            onClick = {}
        )
    }
}