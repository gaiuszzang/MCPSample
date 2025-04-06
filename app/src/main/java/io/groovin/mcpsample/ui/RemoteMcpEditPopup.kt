package io.groovin.mcpsample.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.groovin.mcpsample.McpServerItem.McpRemoteServerItem
import io.groovin.mcpsample.McpUiState
import io.groovin.mcpsample.R
import io.groovin.mcpsample.ui.component.IconButton
import io.groovin.mcpsample.ui.component.IconButtonStyle
import io.groovin.mcpsample.ui.component.InputTextField
import io.groovin.mcpsample.ui.component.SquareButton
import io.groovin.mcpsample.ui.component.Text
import io.groovin.mcpsample.ui.dialog.DialogPopup
import io.groovin.mcpsample.ui.theme.McpTheme
import io.groovin.mcpsample.ui.theme.darkSecondaryButtonColor
import io.groovin.mcpsample.ui.theme.lightSecondaryButtonColor
import io.groovin.mcpsample.util.showToast

@Composable
fun RemoteMcpEditPopup(
    prevItem: McpUiState.McpInfo,
    onConfirm: (McpRemoteServerItem) -> Unit,
    onDismissRequest: () -> Unit
) {

    DialogPopup(
        onDismissRequest = onDismissRequest
    ) {
        var connectionName by remember { mutableStateOf(prevItem.connectionName) }
        var url by remember { mutableStateOf(prevItem.url ?: "") }
        val headers = remember {
            mutableStateListOf<Pair<String, String>>(*(prevItem.headers?.toList()?.toTypedArray() ?: arrayOf()))
        }
        val context = LocalContext.current

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "Edit Remote MCP Server",
                style = McpTheme.textStyle.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Connection Name")
            InputTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = McpTheme.inputTextFieldBackgroundColor, shape = RoundedCornerShape(8.dp))
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
                    .background(color = McpTheme.inputTextFieldBackgroundColor, shape = RoundedCornerShape(8.dp))
                    .padding(8.dp),
                singleLine = true,
                value = url,
                onValueChange = { url = it }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Headers")
            for((index, header) in headers.withIndex()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InputTextField(
                        modifier = Modifier
                            .width(80.dp)
                            .background(color = McpTheme.inputTextFieldBackgroundColor, shape = RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        singleLine = true,
                        value = header.first,
                        onValueChange = { newKey ->
                            headers[index] = header.copy(first = newKey)
                        }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    InputTextField(
                        modifier = Modifier
                            .weight(1f)
                            .background(color = McpTheme.inputTextFieldBackgroundColor, shape = RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        singleLine = true,
                        value = header.second,
                        onValueChange = { newValue ->
                            headers[index] = header.copy(second = newValue)
                        }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        resourceId = R.drawable.icon_trash,
                        style = IconButtonStyle.XSmallBorder,
                        onClick = {
                            headers.removeAt(index)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    modifier = Modifier.align(Alignment.Center),
                    resourceId = R.drawable.icon_plus,
                    style = IconButtonStyle.XSmallBorder,
                    onClick = {
                        headers.add(Pair<String, String>("", ""))
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                SquareButton(
                    modifier = Modifier.weight(1f),
                    text = "Edit",
                    onClick = {
                        if (connectionName.isNotEmpty() && url.isNotEmpty()) {
                            val headerMap = headers.filter { it.first.isNotBlank() && it.second.isNotBlank() }.toMap()
                            val item = McpRemoteServerItem(connectionName, url, headerMap)
                            onConfirm(item)
                            onDismissRequest()
                        } else {
                            context.showToast("Please enter valid values.")
                        }
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
