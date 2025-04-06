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
fun SquareButton(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    fontSize: TextUnit = TextUnit.Unspecified,
    buttonColor: Color = Color(0xFF0050CC),
    onClick: () -> Unit = {}
) {
    Text(
        modifier = modifier
            .background(
                color = buttonColor
            )
            .clickable {
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        text = text,
        textAlign = TextAlign.Center,
        fontSize = fontSize,
        color = textColor
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