package io.groovin.mcpsample.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.groovin.mcpsample.McpUiState
import io.groovin.mcpsample.ui.component.InputTextField
import io.groovin.mcpsample.ui.component.SquareButton
import io.groovin.mcpsample.ui.component.Text
import io.groovin.mcpsample.ui.dialog.DialogPopup
import io.groovin.mcpsample.ui.theme.McpTheme
import io.groovin.mcpsample.ui.theme.darkSecondaryButtonColor
import io.groovin.mcpsample.ui.theme.lightSecondaryButtonColor
import io.groovin.mcpsample.util.showToast
import androidx.core.net.toUri

@Composable
fun PermRationalPopup(
    deniedList: List<String>,
    onDismissRequest: () -> Unit
) {

    DialogPopup(
        onDismissRequest = onDismissRequest
    ) {
        val context = LocalContext.current
        val message = remember { getPermissionRequestRationaleMessage(deniedList) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "Required Permission",
                style = McpTheme.textStyle.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                SquareButton(
                    modifier = Modifier.weight(1f),
                    text = "Okay",
                    onClick = {
                        context.startApplicationDetailSettingsActivity()
                        onDismissRequest()
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                SquareButton(
                    modifier = Modifier.weight(1f),
                    buttonStyle = McpTheme.buttonStyle.copy(
                        buttonColor = if (isSystemInDarkTheme()) darkSecondaryButtonColor else lightSecondaryButtonColor
                    ),
                    text = "Cancel",
                    onClick = onDismissRequest
                )
            }
        }

    }
}


private fun getPermissionRequestRationaleMessage(deniedList: List<String>): String {
    val stringBuilder = StringBuilder()
    stringBuilder.append("Following Permissions are denied.\n\n")
    deniedList.forEach {
        stringBuilder.append("$it\n")
    }
    stringBuilder.append("\nPlease enable Permission from Settings.")
    return stringBuilder.toString()
}

private fun Context.startApplicationDetailSettingsActivity() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${packageName}".toUri())
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    startActivity(intent)
}
