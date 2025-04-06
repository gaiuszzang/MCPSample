package io.groovin.mcpsample.ui

import android.content.Intent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.groovin.mcpsample.ui.component.SquareButton
import io.groovin.mcpsample.ui.component.Text
import io.groovin.mcpsample.ui.dialog.DialogPopup
import io.groovin.mcpsample.ui.theme.McpTheme
import io.groovin.mcpsample.ui.theme.darkSecondaryButtonColor
import io.groovin.mcpsample.ui.theme.lightSecondaryButtonColor

@Composable
fun CustomPermissionPopup(
    message: String,
    actionIntent: Intent?,
    onDismissRequest: () -> Unit
) {

    DialogPopup(
        onDismissRequest = onDismissRequest
    ) {
        val context = LocalContext.current
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
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
                        if (actionIntent != null) {
                            context.startActivity(actionIntent)
                        }
                        onDismissRequest()
                    }
                )
                if (actionIntent != null) {
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
}
