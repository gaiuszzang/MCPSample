package io.groovin.mcpsample.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.groovin.mcpsample.McpUiState
import io.groovin.mcpsample.R
import io.groovin.mcpsample.ui.bottomsheet.BottomSheetPopup
import io.groovin.mcpsample.ui.component.IconButton
import io.groovin.mcpsample.ui.component.IconButtonStyle
import io.groovin.mcpsample.ui.component.Text
import io.groovin.mcpsample.ui.component.ToggleSwitch
import io.groovin.mcpsample.ui.theme.McpTheme
import io.groovin.mcpsample.ui.theme.darkSubTextColor
import io.groovin.mcpsample.ui.theme.lightSubTextColor


@Composable
fun RemoteMcpSettingPopup(
    uiState: McpUiState,
    onDismissRequest: () -> Unit
) {
    var addPopup by remember { mutableStateOf(false) }
    var editPopup by remember { mutableStateOf<McpUiState.McpInfo?>(null) }
    BottomSheetPopup(
        onDismissRequest = onDismissRequest
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            val heightIn = this.maxHeight * 0.8f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
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
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = mcpItem.connectionName,
                                    style = McpTheme.textStyle.copy(
                                        fontSize = 16.sp
                                    )
                                )
                                mcpItem.serverName?.let { serverName ->
                                    Text(
                                        text = "Server: $serverName",
                                        style = McpTheme.textStyle.copy(
                                            textColor = if (isSystemInDarkTheme()) darkSubTextColor else lightSubTextColor,
                                            fontSize = 12.sp,
                                        )
                                    )
                                }
                                mcpItem.version?.let { version ->
                                    Text(
                                        text = "Version: $version",
                                        style = McpTheme.textStyle.copy(
                                            textColor = if (isSystemInDarkTheme()) darkSubTextColor else lightSubTextColor,
                                            fontSize = 12.sp,
                                        )
                                    )
                                }
                                mcpItem.supportFunctionDesc?.let { functionDesc ->
                                    Text(
                                        text = "Functions: $functionDesc",
                                        style = McpTheme.textStyle.copy(
                                            textColor = if (isSystemInDarkTheme()) darkSubTextColor else lightSubTextColor,
                                            fontSize = 12.sp,
                                        )
                                    )
                                }
                                mcpItem.url?.let { url ->
                                    Text(
                                        text = "URL: $url",
                                        style = McpTheme.textStyle.copy(
                                            textColor = if (isSystemInDarkTheme()) darkSubTextColor else lightSubTextColor,
                                            fontSize = 12.sp,
                                        )
                                    )
                                }
                            }
                            if (mcpItem.isRemovable && !mcpItem.isOn) {
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    resourceId = R.drawable.icon_edit,
                                    style = IconButtonStyle.SmallBorder,
                                    onClick = {
                                        editPopup = mcpItem
                                    }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    resourceId = R.drawable.icon_trash,
                                    style = IconButtonStyle.SmallBorder,
                                    onClick = {
                                        mcpItem.onRemoveClick()
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            ToggleSwitch(
                                isOn = mcpItem.isOn,
                                onChanged = {
                                    mcpItem.onToggleClick()
                                }
                            )
                        }
                    }
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                modifier = Modifier.align(Alignment.Center),
                                resourceId = R.drawable.icon_plus,
                                style = IconButtonStyle.BasicBorder,
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
            onConfirm = { item ->
                uiState.onAddMcpItem(item)
            },
            onDismissRequest = {
                addPopup = false
            }
        )
    }
    editPopup?.let { editPopupItem ->
        RemoteMcpEditPopup(
            prevItem = editPopupItem,
            onConfirm = { item ->
                uiState.onEditMcpItem(editPopupItem, item)
            },
            onDismissRequest = {
                editPopup = null
            }
        )
    }
}
