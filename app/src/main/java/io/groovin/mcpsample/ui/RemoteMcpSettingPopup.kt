package io.groovin.mcpsample.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.groovin.mcpsample.McpUiState
import io.groovin.mcpsample.ui.bottomsheet.BottomSheetPopup
import io.groovin.mcpsample.ui.component.RoundButton
import io.groovin.mcpsample.ui.component.ToggleButton


@Composable
fun RemoteMcpSettingPopup(
    uiState: McpUiState,
    onDismissRequest: () -> Unit
) {
    var addPopup by remember { mutableStateOf(false) }
    BottomSheetPopup(
        onDismissRequest = onDismissRequest
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            val heightIn = maxHeight * 0.8f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .requiredHeightIn(max = heightIn)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                ) {
                    items(uiState.mcpInfoList) { mcpItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = mcpItem.connectionName,
                                    fontSize = 16.sp
                                )
                                mcpItem.serverName?.let { serverName ->
                                    Text(
                                        text = "Server: $serverName",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                                mcpItem.version?.let { version ->
                                    Text(
                                        text = "Version: $version",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                                mcpItem.supportFunctionDesc?.let { functionDesc ->
                                    Text(
                                        text = "Functions: $functionDesc",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                                mcpItem.url?.let { url ->
                                    Text(
                                        text = "URL: $url",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            if (mcpItem.isRemovable && !mcpItem.isOn) {
                                RoundButton(
                                    text = "Remove",
                                    onClick = {
                                        mcpItem.onRemoveClick()
                                    }
                                )
                            }
                            ToggleButton(
                                modifier = Modifier.width(80.dp),
                                text = if (mcpItem.isOn) "On" else "Off",
                                isChecked = mcpItem.isOn,
                                onClick = {
                                    mcpItem.onToggleClick()
                                }
                            )
                        }
                    }
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            RoundButton(
                                text = "Add",
                                onClick = {
                                    addPopup = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    if (addPopup) {
        RemoteMcpAddPopup(
            onConfirm = { connectionName, url ->
                uiState.onAddRequest(connectionName, url)
            },
            onDismissRequest = {
                addPopup = false
            }
        )
    }
}