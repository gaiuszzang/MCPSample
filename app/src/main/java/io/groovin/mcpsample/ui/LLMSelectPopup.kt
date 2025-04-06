package io.groovin.mcpsample.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.groovin.mcpsample.LLMSelectUiState
import io.groovin.mcpsample.R
import io.groovin.mcpsample.ui.component.Text
import io.groovin.mcpsample.ui.dialog.DialogPopup
import io.groovin.mcpsample.ui.theme.LocalMcpThemeScheme
import io.groovin.mcpsample.ui.theme.primaryColor

@Composable
fun LLMSelectPopup(
    uiState: LLMSelectUiState,
    onDismissRequest: () -> Unit
) {
    DialogPopup(
        onDismissRequest = onDismissRequest
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp)
        ) {
            items(uiState.llmItemList) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            uiState.onItemSelectClick(item)
                            onDismissRequest()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.llmType.llmName
                    )
                    Spacer(modifier = Modifier.weight(1f))

                    if (item.isSelected) {
                        Image(
                            painter = painterResource(R.drawable.icon_check),
                            modifier = Modifier
                                .size(24.dp),
                            colorFilter = ColorFilter.tint(color = primaryColor),
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}
