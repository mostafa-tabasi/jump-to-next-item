package com.mstf.jumptonextitem

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.clipToBounds
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

@Composable
fun <E, T> JumpToNextItemList(
    modifier: Modifier = Modifier,
    lazyListState: LazyListState,
    itemList: List<E>,
    nextItem: T?,
    onJumpToNextItem: (T) -> Unit,
    listItemContent: @Composable (index: Int, item: E) -> Unit,
    nextItemContent: @Composable (contentSize: Dp, swipedEnough: Boolean) -> Unit,
    nextItemLabel: String,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    // max height of the next item layout that user needs to swipe for jumping to the next item
    val minListYOffset = remember { (-100).dp }
    val maxNextItemLayoutWidth = remember { 50.dp }
    // current amount of swiping
    val listYOffset = remember { Animatable(0f) }
    val nextItemLayoutHeight = remember { Animatable(0f) }
    val nextItemLayoutWidth = remember { Animatable(0f) }
    val nextItemLayoutBottomMargin = remember { Animatable(density.dpToPx(8.dp)) }
    val nextItemLayoutPadding = remember { Animatable(density.dpToPx(6.dp)) }

    var listHeight by remember { mutableIntStateOf(0) }

    var isAnimating by remember { mutableStateOf(false) }

    fun swipedEnoughToJumpToNextItem(): Boolean {
        return listYOffset.value == density.dpToPx(minListYOffset)
    }

    fun swipedEnoughToShowNextItemIcon(): Boolean {
        return if (nextItem != null)
            listYOffset.value < density.dpToPx(minListYOffset / 3 * 2)
        else swipedEnoughToJumpToNextItem()
    }

    val nextItemIconSize by animateDpAsState(
        targetValue =
        if (swipedEnoughToShowNextItemIcon()) 50.dp
        else 0.dp,
        label = "next_item_size"
    )

    val arrowUpMargin by animateDpAsState(
        targetValue =
        if (swipedEnoughToShowNextItemIcon()) 6.dp
        else 0.dp,
        label = "arrow_up_margin"
    )

    val swipeBackgroundColor by animateColorAsState(
        targetValue =
        if (swipedEnoughToJumpToNextItem() && nextItem != null) Color.White
        else Color.LightGray,
        label = "swipe_background_color"
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
            .clipToBounds()
            .nestedScroll(nestedScrollConnection)
            .pointerInput(key1 = itemList, key2 = nextItem) {
                coroutineScope {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            when (event.type) {

                                PointerEventType.Press -> {
                                }

                                PointerEventType.Release -> {
                                    skipDragEventCounter = 0

                                    // reset everything to their default values
                                    listYOffset.tweenAnimateTo(scope, 0f, 200)
                                    nextItemLayoutWidth
                                        .tweenAnimateTo(scope, 0f, 200)
                                    nextItemLayoutHeight
                                        .tweenAnimateTo(scope, 0f, 200)
                                    nextItemLayoutBottomMargin
                                        .tweenAnimateTo(scope, density.dpToPx(8.dp), 200)
                                    nextItemLayoutPadding
                                        .tweenAnimateTo(scope, density.dpToPx(6.dp), 200)

                                    // user swiped enough for jumping to the next item
                                    if (swipedEnoughToJumpToNextItem() && nextItem != null) {
                                        scope.launch {
                                            delay(100)
                                            onJumpToNextItem(nextItem)
                                        }
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

                                        // ignore dragging event while an animation is running
                                        if (!isAnimating) {
                                            // start swiping for jumping to the next item, only when
                                            // user is at the end of the list ( =at the beginning of the list, since list is reversed)
                                            if (!lazyListState.canScrollBackward) {
                                                listYOffset.snapTo(
                                                    scope,
                                                    (listYOffset.value + dragAmount)
                                                        .coerceIn(minListYOffset.toPx(), 0f)
                                                )
                                                nextItemLayoutHeight.snapTo(
                                                    scope,
                                                    (nextItemLayoutHeight.value + -dragAmount)
                                                        .coerceIn(0f, -minListYOffset.toPx())
                                                )
                                                nextItemLayoutWidth.snapTo(
                                                    scope,
                                                    (nextItemLayoutWidth.value + -dragAmount / 2)
                                                        .coerceIn(0f, maxNextItemLayoutWidth.toPx())
                                                )
                                            }

                                            // user swiped enough for jumping to the next item,
                                            // but still hasn't released the touch event
                                            // so just pop the next item content and label
                                            if (dragAmount < 0 && swipedEnoughToJumpToNextItem()) {
                                                isAnimating = true

                                                nextItemLayoutHeight.springAnimateTo(
                                                    scope,
                                                    density.dpToPx(maxNextItemLayoutWidth),
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessMediumLow,
                                                )
                                                nextItemLayoutBottomMargin
                                                    .tweenAnimateTo(scope, 0f, 200)
                                                nextItemLayoutPadding
                                                    .tweenAnimateTo(scope, 0f, 200)

                                                scope.launch {
                                                    delay(200)
                                                    isAnimating = false
                                                }
                                            }

                                            // user came back from fully swiped to partially swiped state
                                            // so just dismiss the next item content and label
                                            if (dragAmount > 0 &&
                                                listYOffset.value <= density.dpToPx(minListYOffset + 1.dp)
                                            ) {
                                                isAnimating = true
                                                nextItemLayoutHeight
                                                    .tweenAnimateTo(scope, -listYOffset.value, 100)
                                                nextItemLayoutHeight
                                                    .tweenAnimateTo(scope, -listYOffset.value, 100)
                                                nextItemLayoutBottomMargin
                                                    .tweenAnimateTo(
                                                        scope,
                                                        density.dpToPx(8.dp),
                                                        100
                                                    )
                                                nextItemLayoutPadding
                                                    .tweenAnimateTo(
                                                        scope,
                                                        density.dpToPx(6.dp),
                                                        100
                                                    )

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
        AnimatedContent(
            targetState = itemList,
            transitionSpec = {
                slideInVertically { height -> -height / 2 } + fadeIn(tween(500)) togetherWith
                        slideOutVertically { height -> height } + fadeOut(tween(100))
            },
            label = "items_list",
        ) { items ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned {
                        listHeight = it.size.height
                    }
                    .offset {
                        IntOffset(
                            x = 0,
                            y = listYOffset.value.toInt()
                        )
                    },
                state = lazyListState,
                reverseLayout = true,
            ) {
                itemsIndexed(items = items) { index, item -> listItemContent(index, item) }
            }
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
                    .height(density.pxToDp(nextItemLayoutHeight.value))
                    .width(density.pxToDp(nextItemLayoutWidth.value))
                    .padding(bottom = density.pxToDp(nextItemLayoutBottomMargin.value))
                    .background(swipeBackgroundColor, shape = CircleShape)
                    .padding(horizontal = density.pxToDp(nextItemLayoutPadding.value)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement =
                if (swipedEnoughToShowNextItemIcon()) Arrangement.SpaceAround
                else Arrangement.Center,
            ) {
                AnimatedVisibility(!swipedEnoughToJumpToNextItem()) {
                    Icon(
                        painterResource(R.drawable.ic_round_arrow_upward),
                        null,
                        tint = Color.White,
                        modifier = Modifier.padding(top = arrowUpMargin)
                    )
                }
                nextItemContent(nextItemIconSize, swipedEnoughToJumpToNextItem())
            }
            AnimatedVisibility(
                visible = swipedEnoughToJumpToNextItem(),
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