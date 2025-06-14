package io.groovin.mcpsample

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.groovin.mcpsample.mcp.localserver.LocalToolPermissionHandler
import io.groovin.mcpsample.mcp.localserver.PermissionRequestHandler
import io.groovin.mcpsample.ui.CommonSettingPopup
import io.groovin.mcpsample.ui.CustomPermissionPopup
import io.groovin.mcpsample.ui.LLMSelectPopup
import io.groovin.mcpsample.ui.Message
import io.groovin.mcpsample.ui.PermRationalPopup
import io.groovin.mcpsample.ui.RemoteMcpSettingPopup
import io.groovin.mcpsample.ui.UserInputKeyboard
import io.groovin.mcpsample.ui.UserInputMicrophone
import io.groovin.mcpsample.ui.component.IconButton
import io.groovin.mcpsample.ui.component.IconButtonStyle
import io.groovin.mcpsample.ui.component.Text
import io.groovin.mcpsample.ui.theme.MCPSampleTheme
import io.groovin.mcpsample.ui.theme.darkChatBackgroundColor
import io.groovin.mcpsample.ui.theme.lightChatBackgroundColor
import io.groovin.mcpsample.util.TTS
import io.groovin.mcpsample.util.logd
import io.groovin.mcpsample.util.showToast
import io.groovin.permx.permX
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import org.koin.java.KoinJavaComponent.inject

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }
    private val permX by permX()
    private val permHandler: LocalToolPermissionHandler by inject()
    private val showPermRationalPopupStateFlow = MutableStateFlow<ImmutableList<String>>(persistentListOf())
    private val showCustomPermissionStateFlow = MutableStateFlow<Pair<String, Intent?>?>(null)
    private lateinit var tts: TTS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TTS(applicationContext)
        registerPermissionHandler()
        enableEdgeToEdge()
        setContent {
            val viewModel = koinViewModel<MainViewModel>()
            val showPermRationalPopupState by showPermRationalPopupStateFlow.collectAsStateWithLifecycle()
            val showCustomPermissionState by showCustomPermissionStateFlow.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                viewModel.ttsEvent.collect {
                    tts.speak(it)
                }
            }
            MCPSampleTheme {
                OnResumeEffect {
                    logd(TAG, "onResume")
                    viewModel.checkConnection()
                }
                MCPHome(
                    modifier = Modifier
                        .background(color = if (isSystemInDarkTheme()) darkChatBackgroundColor else lightChatBackgroundColor)
                        .safeContentPadding(),
                    viewModel = viewModel,
                    tts = tts
                )
                PermissionRationalPopup(showPermRationalPopupState) {
                    showPermRationalPopupStateFlow.update {
                        persistentListOf()
                    }
                }
                CustomPermissionRationalPopup(showCustomPermissionState) {
                    showCustomPermissionStateFlow.update { null }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        tts.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.shutdown()
        unregisterPermissionHandler()
    }

    private fun registerPermissionHandler() {
        permHandler.setHandler(object: PermissionRequestHandler {
            override suspend fun onRequestPermission(permissions: List<String>): Boolean {
                val permResult = permX.requestPermission(permissions.toTypedArray())
                return if (permResult.isAllGranted()) {
                    true
                } else if (permResult.shouldShowRequestPermissionRationale()) {
                    showPermRationalPopupStateFlow.update {
                        permResult.getAllDeniedPermissionList().toImmutableList()
                    }
                    false
                } else {
                    showToast("Permission Denied.")
                    false
                }
            }
            override fun onRequestCustomPermission(guideText: String, onActionIntent: Intent?) {
                showCustomPermissionStateFlow.update {
                    Pair(guideText, onActionIntent)
                }
            }
        })
    }

    private fun unregisterPermissionHandler() {
        permHandler.setHandler(null)
    }
}

@Composable
private fun MCPHome(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    tts: TTS
) {
    val llmSelectUiState by viewModel.llmSelectUiState.collectAsStateWithLifecycle()
    val chatUiState by viewModel.chatUiState.collectAsStateWithLifecycle()
    val settingUiState by viewModel.settingUiState.collectAsStateWithLifecycle()
    val mcpUiState by viewModel.mcpUiState.collectAsStateWithLifecycle()
    var showLLMSelectPopup by remember { mutableStateOf(false) }
    var showSettingPopup by remember { mutableStateOf(false) }
    var showMcpToolPopup by remember { mutableStateOf(false) }
    val hotWordAvailable by remember {
        derivedStateOf {
            !showLLMSelectPopup && !showSettingPopup && !showMcpToolPopup
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier)
    ) {
        // AppTopBar UI
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val selectedLLMItemName = llmSelectUiState.llmItemList.firstOrNull { it.isSelected }?.llmType?.llmName ?: "No Selected"
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(50))
                    .clickable {
                        showLLMSelectPopup = true
                    }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = selectedLLMItemName
                )
                IconButton(
                    resourceId = R.drawable.icon_dropdown,
                    style = IconButtonStyle.XSmall
                ) {
                    showLLMSelectPopup = true
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                resourceId = R.drawable.icon_settings,
                onClick = {
                    showSettingPopup = true
                }
            )
        }
        //Message List UI
        val scrollState = rememberLazyListState()
        LaunchedEffect(chatUiState.messageList.size) {
            // Auto-scroll to the bottom of the message list when new messages are added
            scrollState.animateScrollToItem(chatUiState.messageList.size)
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            state = scrollState
        ) {
            items(chatUiState.messageList) { message ->
                Message(
                    modifier = Modifier.fillMaxWidth(),
                    message = message
                )
            }
        }
        // Input UI
        var inputMode by remember { mutableStateOf(InputMode.Keyboard) }
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            val coroutineScope = rememberCoroutineScope()
            val permHandler = remember { inject<LocalToolPermissionHandler>(clazz = LocalToolPermissionHandler::class.java).value }
            UserInputKeyboard(
                modifier = Modifier
                    .fillMaxWidth(),
                visibility = inputMode == InputMode.Keyboard,
                isOnHandleChatting = chatUiState.isOnHandleChatting,
                useHotWord = settingUiState.hotWordEnabled && hotWordAvailable,
                hotWordText = settingUiState.hotWordText,
                onMcpToolClick = {
                    showMcpToolPopup = true
                },
                onChatRequestClick = { text ->
                    viewModel.chatRequest(text)
                },
                onChatStopClick = {
                    viewModel.cancelChatRequest()
                },
                onInputModeChangeClick = { mode ->
                    coroutineScope.launch {
                        when (mode) {
                            InputMode.Microphone -> {
                                val result = permHandler.request(listOf(Manifest.permission.RECORD_AUDIO))
                                if (result) {
                                    inputMode = mode
                                }
                                tts.stop()
                            }
                            InputMode.Keyboard -> inputMode = mode
                        }
                    }
                }
            )
            UserInputMicrophone(
                modifier = Modifier
                    .fillMaxWidth(),
                visibility = inputMode == InputMode.Microphone,
                autoResultMode = settingUiState.asrAutoResultModeEnabled,
                onCancelClick = {
                    inputMode = InputMode.Keyboard
                },
                onChatRequestClick = { text ->
                    inputMode = InputMode.Keyboard
                    viewModel.chatRequest(text)
                },
            )
        }

    }

    /* TODO
    if (chatUiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }*/

    if (showLLMSelectPopup) {
        LLMSelectPopup(
            uiState = llmSelectUiState,
            onDismissRequest = {
                showLLMSelectPopup = false
            }
        )
    }
    if (showSettingPopup) {
        CommonSettingPopup(
            uiState = settingUiState,
            onDismissRequest = {
                showSettingPopup = false
            }
        )
    }
    if (showMcpToolPopup) {
        RemoteMcpSettingPopup(
            uiState = mcpUiState,
            onDismissRequest = {
                showMcpToolPopup = false
            }
        )
    }
}

@Composable
private fun PermissionRationalPopup(
    showPermRationalPopupState: ImmutableList<String>,
    onDismissRequest: () -> Unit
) {
    if (showPermRationalPopupState.isNotEmpty()) {
        PermRationalPopup(
            deniedList = showPermRationalPopupState
        ) {
            onDismissRequest()
        }
    }
}

@Composable
private fun CustomPermissionRationalPopup(
    showCustomPermissionState: Pair<String, Intent?>?,
    onDismissRequest: () -> Unit
) {
    if (showCustomPermissionState != null) {
        CustomPermissionPopup(
            message = showCustomPermissionState.first,
            actionIntent = showCustomPermissionState.second
        ) {
            onDismissRequest()
        }
    }
}

@Composable
private fun OnResumeEffect(onResume: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onResume()
            }
        }

        val lifecycle = lifecycleOwner.lifecycle
        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}

enum class InputMode {
    Keyboard, Microphone
}
