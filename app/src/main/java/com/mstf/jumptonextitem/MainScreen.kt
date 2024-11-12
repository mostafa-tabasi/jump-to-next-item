package com.mstf.jumptonextitem

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun MainScreen(paddingValues: PaddingValues, viewModel: MainViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            ChatList(
                chats = state.chats,
                selectedChat = state.selectedChat,
                modifier = Modifier
                    .wrapContentWidth()
                    .animateContentSize()
                    .background(color = Color.LightGray),
                onChatSelect = viewModel::onChatSelect,
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                state.selectedChat?.let { chat ->
                    ChatMessages(chat)
                } ?: run {
                    Text(
                        "Select a chat",
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

}

@Composable
fun ChatList(
    chats: List<MainUiState.Chat>,
    selectedChat: MainUiState.Chat?,
    modifier: Modifier = Modifier,
    onChatSelect: (MainUiState.Chat) -> Unit,
) {
    LazyColumn(modifier) {
        items(chats) { chat ->
            Row(
                modifier = Modifier
                    .clickable { onChatSelect(chat) }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(chat.tint),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(chat.image),
                        contentDescription = null,
                        tint = Color.DarkGray,
                    )
                }
                AnimatedVisibility(
                    visible = selectedChat == null,
                    exit = fadeOut() + shrinkHorizontally(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessHigh,
                        )
                    ),
                ) {
                    Text(
                        chat.title,
                        modifier = Modifier
                            .width(100.dp)
                            .padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessages(chat: MainUiState.Chat, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val lazyListState = rememberLazyListState()
    val minListYOffset = remember { (-100).dp }
    val maxNextItemLayoutWidth = remember { 50.dp }
    val listYOffset = remember { Animatable(0f) }
    val nextItemLayoutHeight = remember { Animatable(0f) }
    val nextItemLayoutWidth = remember { Animatable(0f) }
    val nextItemLayoutBottomMargin = remember { Animatable(with(density) { 8.dp.toPx() }) }
    val nextItemLayoutPadding = remember { Animatable(with(density) { 4.dp.toPx() }) }

    var listHeight by remember { mutableIntStateOf(0) }

    var isAnimating by remember { mutableStateOf(false) }

    val nextItemIconSize by animateDpAsState(
        targetValue =
        if (listYOffset.value < with(density) { (minListYOffset / 3 * 2).toPx() }) 50.dp
        else 0.dp,
        label = "next_item_size"
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
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .pointerInput(Unit) {
                coroutineScope {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            when (event.type) {

                                PointerEventType.Press -> {
                                    Log.d(TAG, "onPress")
                                }

                                PointerEventType.Release -> {
                                    skipDragEventCounter = 0
                                    if (listYOffset.value > minListYOffset.toPx()) {
                                        Log.d(TAG, "onRelease, reset everything")
                                        scope.launch { listYOffset.animateTo(0f) }
                                        scope.launch { nextItemLayoutWidth.animateTo(0f) }
                                        scope.launch { nextItemLayoutHeight.animateTo(0f) }
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
                                                        with(density) { 4.dp.toPx() },
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
        val openedEnough = listYOffset.value == with(density) { minListYOffset.toPx() }
        val isHalfOpened = listYOffset.value < with(density) { (minListYOffset / 3 * 2).toPx() }
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
        ) {
            items(chat.messages) { item ->
                Text(
                    item,
                    modifier = Modifier
                        .padding(8.dp)
                        .background(Color.LightGray)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
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
                    .height(with(density) { nextItemLayoutHeight.value.toDp() })
                    .width(with(density) { nextItemLayoutWidth.value.toDp() })
                    .padding(bottom = with(density) { nextItemLayoutBottomMargin.value.toDp() })
                    .clip(RoundedCornerShape(50.dp))
                    .background(Color.LightGray)
                    .padding(with(density) { nextItemLayoutPadding.value.toDp() }),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (isHalfOpened) Arrangement.SpaceAround else Arrangement.Center,
            ) {
                AnimatedVisibility(!openedEnough) {
                    Icon(
                        painterResource(R.drawable.ic_round_arrow_upward),
                        null,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(nextItemIconSize)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color.Red),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Home,
                        null,
                        modifier = Modifier.padding(4.dp),
                    )
                }
            }
            AnimatedVisibility(
                visible = openedEnough,
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
                label = "",
            ) {
                Text("Item Name", modifier = Modifier.padding(top = 24.dp))
            }
        }
    }

    LaunchedEffect(chat) {
        scope.launch {
            lazyListState.animateScrollToItem(chat.firstUnreadIndex)
        }
    }
}