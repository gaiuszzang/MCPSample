package io.groovin.mcpsample.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.groovin.mcpsample.ui.component.InputTextField
import io.groovin.mcpsample.ui.component.SquareButton
import io.groovin.mcpsample.ui.dialog.DialogPopup
import io.groovin.mcpsample.util.showToast

@Composable
fun RemoteMcpAddPopup(
    onConfirm: (String, String) -> Unit,
    onDismissRequest: () -> Unit
) {
    DialogPopup(
        onDismissRequest = onDismissRequest
    ) {
        var connectionName by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("") }
        val context = LocalContext.current

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "Add Remote MCP Server",
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Connection Name")
            InputTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color(0xFFF0F0F0), shape = RoundedCornerShape(8.dp))
                    .padding(8.dp),
                singleLine = true,
                value = connectionName,
                onValueChange = { connectionName = it }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Url")
            InputTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color(0xFFF0F0F0), shape = RoundedCornerShape(8.dp))
                    .padding(8.dp),
                singleLine = true,
                value = url,
                onValueChange = { url = it }
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                SquareButton(
                    modifier = Modifier.weight(1f),
                    text = "Add",
                    onClick = {
                        if (connectionName.isNotEmpty() && url.isNotEmpty()) {
                            onConfirm(connectionName, url)
                            onDismissRequest()
                        } else {
                            context.showToast("Please enter valid values.")
                        }
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                SquareButton(
                    modifier = Modifier.weight(1f),
                    buttonColor = Color(0xFF808080),
                    text = "Cancel",
                    onClick = onDismissRequest
                )
            }
        }

    }
}
