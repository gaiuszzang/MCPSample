package io.groovin.mcpsample.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.groovin.mcpsample.ui.theme.McpTheme

@Composable
fun DialogPopup(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(
        dismissOnBackPress = true,
        dismissOnClickOutside = true
    ),
    content: @Composable () -> Unit
) {
    Dialog(
        properties = properties,
        onDismissRequest = onDismissRequest
    ) {
        DialogPopupContainer(content = content)
    }
}

@Composable
private fun DialogPopupContainer(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .background(color = McpTheme.surfaceColor, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
    ) {
        content()
    }
}