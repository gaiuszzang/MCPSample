package io.groovin.mcpsample

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.groovin.mcpsample.ui.theme.MCPSampleTheme

class MainActivity : ComponentActivity() {
    private val sharedPreference by lazy {
        getSharedPreferences("MCPSample", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel = remember { MainViewModel(sharedPreference) }
            MCPSampleTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeContentPadding()
                ) { innerPadding ->
                    MCPHome(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}


@Composable
private fun MCPHome(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            Button(
                onClick = viewModel::startServer
            ) {
                Text("Server On")
            }
            Button(
                onClick = viewModel::connectClient
            ) {
                Text("Client On")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "API-KEY")
            TextField(
                modifier = Modifier.weight(1f),
                value = uiState.apiKey,
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                singleLine = true,
                onValueChange = {
                    viewModel.updateApiKey(it)
                }
            )
        }
        val scrollState = rememberLazyListState()
        LaunchedEffect(uiState.messageList) {
            // Auto-scroll to the bottom of the message list when new messages are added
            scrollState.animateScrollToItem(uiState.messageList.size)
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            state = scrollState
        ) {
            items(uiState.messageList) { message ->
                Text(
                    text = message,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            var text by remember { mutableStateOf("") }
            TextField(
                modifier = Modifier.weight(1f),
                value = text,
                onValueChange = {
                    text = it
                }
            )
            Column {
                Button(
                    onClick = {
                        viewModel.chatRequest(text)
                        text = ""
                    }
                ) {
                    Text("SEND")
                }
                Button(
                    onClick = {
                        viewModel.promptRequest(text)
                        text = ""
                    }
                ) {
                    Text("PMT")
                }
                Button(
                    onClick = {
                        viewModel.withResourceRequest(text)
                        text = ""
                    }
                ) {
                    Text("RSC")
                }
            }
        }
    }
}
