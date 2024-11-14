package com.mstf.jumptonextitem

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
fun <T, B> JumpToNextItemList(
    modifier: Modifier = Modifier,
    lazyListState: LazyListState,
    itemList: List<T>,
    nextItem: B?,
    onJumpToNextItem: (B) -> Unit,
    content: LazyListScope.() -> Unit,
    nextItemContent: @Composable (contentSize: Dp, swipedEnough: Boolean) -> Unit,
    nextItemLabel: String,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val minListYOffset = remember { (-100).dp }
    val maxNextItemLayoutWidth = remember { 50.dp }
    val listYOffset = remember { Animatable(0f) }
    val nextItemLayoutHeight = remember { Animatable(0f) }
    val nextItemLayoutWidth = remember { Animatable(0f) }
    val nextItemLayoutBottomMargin = remember { Animatable(with(density) { 8.dp.toPx() }) }
    val nextItemLayoutPadding = remember { Animatable(with(density) { 6.dp.toPx() }) }

    var listHeight by remember { mutableIntStateOf(0) }

    var isAnimating by remember { mutableStateOf(false) }

    fun swipedEnoughToShowNextChatIcon(): Boolean {
        return if (nextItem != null)
            listYOffset.value < with(density) { (minListYOffset / 3 * 2).toPx() }
        else listYOffset.value == with(density) { minListYOffset.toPx() }
    }

    val nextItemIconSize by animateDpAsState(
        targetValue =
        if (swipedEnoughToShowNextChatIcon()) 50.dp
        else 0.dp,
        label = "next_item_size"
    )

    val arrowUpMargin by animateDpAsState(
        targetValue =
        if (swipedEnoughToShowNextChatIcon()) 6.dp
        else 0.dp,
        label = "arrow_up_margin"
    )

    var skipDragEventCounter by remember { mutableIntStateOf(0) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dragAmount = available.y

                return Offset(
                    x = 0f,
                    y =
                    if (listYOffset.value == 0f) 0f
                    else dragAmount,
                )
            }
        }
    }
    Box(
        modifier = modifier
            .nestedScroll(nestedScrollConnection)
            .pointerInput(key1 = itemList) {
                coroutineScope {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            when (event.type) {

                                PointerEventType.Press -> {
                                }

                                PointerEventType.Release -> {
                                    skipDragEventCounter = 0

                                    if (
                                        listYOffset.value > minListYOffset.toPx()
                                        || (listYOffset.value <= minListYOffset.toPx() && nextItem == null)
                                    ) {
                                        scope.launch { listYOffset.animateTo(0f) }
                                        scope.launch { nextItemLayoutWidth.animateTo(0f) }
                                        scope.launch { nextItemLayoutHeight.animateTo(0f) }
                                        scope.launch {
                                            nextItemLayoutBottomMargin.animateTo(
                                                with(density) { 8.dp.toPx() }
                                            )
                                        }
                                        scope.launch {
                                            nextItemLayoutPadding.animateTo(
                                                with(density) { 6.dp.toPx() },
                                            )
                                        }
                                    } else if (listYOffset.value <= minListYOffset.toPx()) {
                                        scope.launch {
                                            listYOffset.snapTo(0f)
                                            nextItemLayoutWidth.snapTo(0f)
                                            nextItemLayoutHeight.snapTo(0f)
                                            nextItemLayoutPadding.snapTo(with(density) { 6.dp.toPx() })
                                            nextItemLayoutBottomMargin.animateTo(with(density) { 8.dp.toPx() })
                                        }

                                        nextItem?.let { onJumpToNextItem(it) }
                                    }
                                }

                                PointerEventType.Move -> {
                                    if (skipDragEventCounter < 5) skipDragEventCounter++
                                    else {
                                        val dragAmount =
                                            event.changes[0].let { it.position.y - it.previousPosition.y } / 2

                                        // Log.d(TAG, "drag amount: $dragAmount")
                                        // Log.d(TAG, "canScrollForward: ${lazyListState.canScrollForward}")
                                        // Log.d(TAG, "nextItemLayoutHeight: $listYOffset")
                                        // Log.d(TAG, "=== isAnimating? = $isAnimating")

                                        if (!isAnimating) {
                                            if (!lazyListState.canScrollBackward) {
                                                scope.launch {
                                                    listYOffset.snapTo(
                                                        (listYOffset.value + dragAmount)
                                                            .coerceIn(minListYOffset.toPx(), 0f)
                                                    )
                                                }
                                                scope.launch {
                                                    nextItemLayoutHeight.snapTo(
                                                        (nextItemLayoutHeight.value + -dragAmount)
                                                            .coerceIn(
                                                                0f,
                                                                -minListYOffset.toPx()
                                                            )
                                                    )
                                                }
                                                scope.launch {
                                                    nextItemLayoutWidth.snapTo(
                                                        (nextItemLayoutWidth.value + -dragAmount / 2)
                                                            .coerceIn(
                                                                0f,
                                                                maxNextItemLayoutWidth.toPx(),
                                                            )
                                                    )
                                                }
                                            }

                                            if (dragAmount < 0
                                                && listYOffset.value == with(density) { minListYOffset.toPx() }
                                            ) {
                                                isAnimating = true
                                                scope.launch {
                                                    nextItemLayoutHeight.animateTo(
                                                        with(density) { maxNextItemLayoutWidth.toPx() },
                                                        animationSpec = spring(
                                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                                            stiffness = Spring.StiffnessMedium
                                                        ),
                                                    )
                                                }
                                                scope.launch {
                                                    nextItemLayoutBottomMargin.animateTo(
                                                        0f,
                                                        animationSpec = tween(200),
                                                    )
                                                }
                                                scope.launch {
                                                    nextItemLayoutPadding.animateTo(
                                                        0f,
                                                        animationSpec = tween(200),
                                                    )
                                                }
                                                scope.launch {
                                                    delay(200)
                                                    isAnimating = false
                                                }
                                            }


                                            if (dragAmount > 0
                                                && listYOffset.value <= with(density) { (minListYOffset + 1.dp).toPx() }
                                            ) {
                                                isAnimating = true
                                                scope.launch {
                                                    nextItemLayoutHeight.animateTo(
                                                        -listYOffset.value,
                                                        animationSpec = tween(100),
                                                    )
                                                }
                                                scope.launch {
                                                    nextItemLayoutBottomMargin.animateTo(
                                                        with(density) { 8.dp.toPx() },
                                                        animationSpec = tween(100),
                                                    )
                                                }
                                                scope.launch {
                                                    nextItemLayoutPadding.animateTo(
                                                        with(density) { 6.dp.toPx() },
                                                        animationSpec = tween(100),
                                                    )
                                                }
                                                scope.launch {
                                                    delay(100)
                                                    isAnimating = false
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
    ) {
        val swipedEnough = listYOffset.value == with(density) { minListYOffset.toPx() }
        AnimatedContent(targetState = itemList, label = "chat_messages") { _ ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned {
                        listHeight = it.size.height
                    }
                    .offset {
                        IntOffset(
                            x = 0,
                            y = listYOffset.value.toInt(),
                        )
                    },
                state = lazyListState,
                reverseLayout = true,
                content = content,
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .height(-minListYOffset)
                .offset {
                    IntOffset(
                        x = 0,
                        y = listHeight + listYOffset.value.toInt(),
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier
                    .height(with(density) { nextItemLayoutHeight.value.toDp() })
                    .width(with(density) { nextItemLayoutWidth.value.toDp() })
                    .padding(bottom = with(density) { nextItemLayoutBottomMargin.value.toDp() })
                    .background(Color.LightGray, shape = CircleShape)
                    .padding(horizontal = with(density) { nextItemLayoutPadding.value.toDp() }),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement =
                if (swipedEnoughToShowNextChatIcon()) Arrangement.SpaceAround
                else Arrangement.Center,
            ) {
                AnimatedVisibility(!swipedEnough) {
                    Icon(
                        painterResource(R.drawable.ic_round_arrow_upward),
                        null,
                        tint = Color.White,
                        modifier = Modifier.padding(top = arrowUpMargin)
                    )
                }
                nextItemContent(nextItemIconSize, swipedEnough)
            }
            AnimatedVisibility(
                visible = swipedEnough,
                enter = slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                ),
                exit = slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 100)
                ),
                label = "next_item_label",
            ) {
                Text(
                    text = nextItemLabel,
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .background(Color.LightGray, shape = CircleShape)
                        .padding(horizontal = 8.dp),
                )
            }
        }
    }
}