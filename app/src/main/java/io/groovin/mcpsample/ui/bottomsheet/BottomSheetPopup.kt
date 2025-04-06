package io.groovin.mcpsample.ui.bottomsheet

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BottomSheetPopup(
    showAnimationDuration: Int = DefaultBottomSheetPopupAnimationDuration,
    dismissAnimationDuration: Int = DefaultBottomSheetPopupAnimationDuration,
    draggable: Boolean = true,
    enableDismissByInteraction: Boolean = true,
    scrimColor: Color = Color(0x3A000000),
    onDismissRequest: () -> Unit,
    bottomSheet: @Composable BottomSheetPopupScope.() -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    val bottomSheetScope = remember {
        BottomSheetPopupScope(
            coroutineScope = coroutineScope,
            dismissAnimationDuration = dismissAnimationDuration,
            dismissRequest = onDismissRequest
        )
    }

    SideEffect {
        bottomSheetScope.enableDismissByInteraction = enableDismissByInteraction
    }
    BottomSheetContainerDialog(
        onDismissRequest = { bottomSheetScope.dismiss() }
    ) {
        BottomSheetBackScrim(
            modifier = Modifier
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {
                        if (bottomSheetScope.enableDismissByInteraction) bottomSheetScope.dismiss()
                    }
                ),
            color = scrimColor,
            bottomSheetScope = bottomSheetScope,
            showAnimationDuration = showAnimationDuration,
            dismissAnimationDuration = dismissAnimationDuration
        )
        Column(
            Modifier
                .safeDrawingPadding()
                .bottomSheetWidthIn()
                .fillMaxHeight()
        ) {
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            AnimatedVisibility(
                visibleState = bottomSheetScope.popupVisible,
                enter = expandVertically(
                    animationSpec = tween(showAnimationDuration),
                    expandFrom = Alignment.Top
                ) + fadeIn(animationSpec = tween(showAnimationDuration)),
                exit = shrinkVertically(
                    animationSpec = tween(dismissAnimationDuration),
                    shrinkTowards = Alignment.Top
                ) + fadeOut(animationSpec = tween(dismissAnimationDuration))
            ) {
                if (draggable) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                            Box(
                                modifier = with(bottomSheetScope) { Modifier.bottomSheetPopupDraggable(true) }
                            ) {
                                bottomSheetScope.bottomSheet()
                            }
                        }
                    } else {
                        Box(
                            modifier = with(bottomSheetScope) { Modifier.bottomSheetPopupDraggable(true) }
                        ) {
                            bottomSheetScope.bottomSheet()
                        }
                    }
                } else {
                    bottomSheetScope.bottomSheet()
                }
            }
        }
    }
}


class BottomSheetPopupScope(
    private val coroutineScope: CoroutineScope,
    private val dismissAnimationDuration: Int,
    private val dismissRequest: () -> Unit
) {
    internal val popupVisible = MutableTransitionState(false).apply { targetState = true }
    internal var enableDismissByInteraction: Boolean = true

    /**
     * dismiss() 호출 시 BottomSheetPopup의 dismissRequest()가 호출된다.
     *
     * @param withAnimation
     * true일 경우, dismiss animation과 함께 닫힌다.
     * default : true
     */
    fun dismiss(withAnimation: Boolean = true) {
        coroutineScope.launch {
            popupVisible.targetState = false
            if (withAnimation) {
                delay(dismissAnimationDuration + 50L)
            }
            dismissRequest()
        }
    }

    /**
     * dismissWithRequest() 호출 시 파라메터로 전달된 request가 호출된다.
     * BottomSheetPopup의 dismissRequest() 호출 없이 직접 dismiss 처리가 필요할 때 사용하는 것을 권장한다.
     *
     * @param withAnimation
     * true일 경우, dismiss animation과 함께 닫힌다.
     * default : true
     *
     * @param request
     * dismiss 요청을 핸들링 할 람다 함수를 전달하기 위한 파라메터이다.
     */
    fun dismissWithRequest(withAnimation: Boolean = true, request: () -> Unit) {
        coroutineScope.launch {
            popupVisible.targetState = false
            if (withAnimation) {
                delay(dismissAnimationDuration + 50L)
            }
            request()
        }
    }

    internal fun Modifier.bottomSheetPopupDraggable(
        nestedScrollEnabled: Boolean = true,
        animationDuration: Int = DefaultBottomSheetPopupAnimationDuration
    ) = swipeDownClosable(
        nestedScrollEnabled = nestedScrollEnabled,
        animationDuration = animationDuration,
        isEnableClose = {
            enableDismissByInteraction
        },
        closeAction = {
            dismiss(withAnimation = true)
        }
    )
}

@Composable
private fun BottomSheetBackScrim(
    bottomSheetScope: BottomSheetPopupScope,
    showAnimationDuration: Int,
    dismissAnimationDuration: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        modifier = modifier,
        visibleState = bottomSheetScope.popupVisible,
        enter = fadeIn(tween(showAnimationDuration)),
        exit = fadeOut(tween(dismissAnimationDuration))
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(color = color)
        )
    }
}

private const val DefaultBottomSheetPopupAnimationDuration: Int = 200



fun Modifier.swipeDownClosable(
    nestedScrollEnabled: Boolean = false,
    animationDuration: Int = 250,
    closeThreshold: Float = 0.2f,
    isEnableClose: () -> Boolean = { true },
    closeAction: () -> Unit = {}
) = composed {
    var maxHeight by remember { mutableFloatStateOf(0f) }
    var offset by remember { mutableFloatStateOf(0f) }

    fun performDrag(available: Float): Float {
        val consumed = if (available > 0) {
            available
        } else {
            if (offset > -available) available else -offset
        }
        offset = max(0f, offset + available)
        return consumed
    }

    suspend fun performFling(velocity: Float) {
        // 최고 높이의 closeThreshold(default = 20%) 이상 아래로 내려갔고, swipe down Fling이면 dismiss함
        val isCloseCondition = isEnableClose() && ((offset > maxHeight * closeThreshold) && (velocity > 0f))
        Animatable(offset).animateTo(
            targetValue = if (isCloseCondition) maxHeight else 0f,
            animationSpec = tween(animationDuration, easing = LinearOutSlowInEasing)
        ) {
            offset = this.value
        }
        if (isCloseCondition) closeAction()
    }

    val nestedScrollConnection = remember(nestedScrollEnabled) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                return if (nestedScrollEnabled && available.y < 0 && source == NestedScrollSource.Drag) {
                    Offset(0f, performDrag(available.y))
                } else {
                    super.onPreScroll(available, source)
                }
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                return if (nestedScrollEnabled && source == NestedScrollSource.Drag) {
                    Offset(0f, performDrag(available.y))
                } else {
                    super.onPostScroll(consumed, available, source)
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (offset <= 0f) return super.onPreFling(available)
                performFling(available.y)
                return available
            }
        }
    }

    return@composed this
        .clipToBounds()
        .onGloballyPositioned {
            maxHeight = it.size.height.toFloat()
        }
        .offset {
            IntOffset(
                0,
                offset
                    .toInt()
                    .coerceAtLeast(0)
            )
        }
        .nestedScroll(nestedScrollConnection)
        .draggable(
            state = rememberDraggableState(
                onDelta = {
                    performDrag(it)
                }
            ),
            orientation = Orientation.Vertical,
            onDragStopped = { velocity ->
                performFling(velocity)
            }
        )
}

//TODO nothing now
fun Modifier.bottomSheetWidthIn() = this

/*
    composed {
    val widthSizeClass = getWindowWidthSizeClass()
    this.widthIn(max = if (widthSizeClass != WindowWidthSizeClass.Compact) BottomSheetMaxWidth else Dp.Unspecified)
}*/