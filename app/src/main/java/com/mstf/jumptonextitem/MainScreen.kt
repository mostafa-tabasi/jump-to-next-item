package com.mstf.jumptonextitem

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
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
            .background(Color.Gray)
            .padding(paddingValues),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            ChatList(
                modifier = Modifier
                    .wrapContentWidth()
                    .fillMaxHeight()
                    .animateContentSize()
                    .background(color = Color.LightGray),
                chats = state.chats,
                selectedChat = state.selectedChat,
                onChatSelect = viewModel::onChatSelect,
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                state.selectedChat?.let { chat ->
                    ChatMessages(
                        messages = chat.messages,
                        firstUnreadMessageIndex = chat.firstUnreadIndex,
                        nextUnreadChat = state.chats.firstOrNull { it.unread && it != chat },
                        onJumpToNextChat = viewModel::onChatSelect,
                    )
                } ?: run {
                    Text(
                        "Select a chat",
                        color = Color.Gray,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatList(
    modifier: Modifier = Modifier,
    chats: List<MainUiState.Chat>,
    selectedChat: MainUiState.Chat?,
    onChatSelect: (MainUiState.Chat) -> Unit,
) {
    LazyColumn(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(chats) { chat ->
            val isChatSelected = chat == selectedChat
            Chat(
                modifier = Modifier
                    .conditional(!isChatSelected, { clickable { onChatSelect(chat) } })
                    .background(if (isChatSelected) Color.Gray.copy(alpha = 0.5f) else Color.LightGray)
                    .padding(8.dp),
                chat = chat,
                showTitle = selectedChat == null,
                showUnreadBadge = true,
            )
        }
    }
}

@Composable
private fun Chat(
    modifier: Modifier = Modifier,
    chat: MainUiState.Chat,
    showTitle: Boolean,
    showUnreadBadge: Boolean = true,
    unreadBadgeColor: Color = Color.Red,
    unreadBadgeBorderColor: Color = Color.LightGray,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val constraints = ConstraintSet {
            val unreadBadge = createRefFor("unread_badge")
            constrain(unreadBadge) {
                top.linkTo(parent.top, margin = (-4).dp)
                end.linkTo(parent.end, margin = (-4).dp)
            }
        }
        ConstraintLayout(
            constraints,
            modifier = Modifier.size(50.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(chat.backgroundTint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(chat.image),
                    contentDescription = null,
                    tint = chat.imageTint,
                )
            }
            AnimatedVisibility(
                visible = chat.unread && showUnreadBadge,
                modifier = Modifier
                    .layoutId("unread_badge"),
                enter = scaleIn(animationSpec = spring()),
                exit = scaleOut(animationSpec = spring()),
                label = "unread_badge"
            ) {
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 20.dp)
                        .background(unreadBadgeColor, shape = CircleShape)
                        .border(width = 2.dp, color = unreadBadgeBorderColor, shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    val unreadCount =
                        chat.firstUnreadIndex.let {
                            when {
                                it == null -> ""
                                it < 9 -> " ${it.plus(1)} "
                                it in 9..98 -> "${it.plus(1)}"
                                else -> "+99"
                            }
                        }
                    Text(
                        unreadCount,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = showTitle,
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

@Composable
private fun ChatMessages(
    modifier: Modifier = Modifier,
    messages: List<MainUiState.Chat.Message>,
    firstUnreadMessageIndex: Int?,
    nextUnreadChat: MainUiState.Chat?,
    onJumpToNextChat: (MainUiState.Chat) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val lazyListState = rememberLazyListState()
    val minListYOffset = remember { (-100).dp }
    val maxNextItemLayoutWidth = remember { 50.dp }
    val listYOffset = remember { Animatable(0f) }
    val nextItemLayoutHeight = remember { Animatable(0f) }
    val nextItemLayoutWidth = remember { Animatable(0f) }
    val nextItemLayoutBottomMargin = remember { Animatable(with(density) { 8.dp.toPx() }) }
    val nextItemLayoutPadding = remember { Animatable(with(density) { 6.dp.toPx() }) }

    var listHeight by remember { mutableIntStateOf(0) }

    var isAnimating by remember { mutableStateOf(false) }

    fun openedEnoughToShowNextChatIcon(): Boolean {
        return if (nextUnreadChat != null)
            listYOffset.value < with(density) { (minListYOffset / 3 * 2).toPx() }
        else listYOffset.value == with(density) { minListYOffset.toPx() }
    }

    val nextItemIconSize by animateDpAsState(
        targetValue =
        if (openedEnoughToShowNextChatIcon()) 50.dp
        else 0.dp,
        label = "next_item_size"
    )

    val arrowUpMargin by animateDpAsState(
        targetValue =
        if (openedEnoughToShowNextChatIcon()) 6.dp
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
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .pointerInput(key1 = messages) {
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
                                        || (listYOffset.value <= minListYOffset.toPx() && nextUnreadChat == null)
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

                                        nextUnreadChat?.let { onJumpToNextChat(it) }
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
        val openedEnough = listYOffset.value == with(density) { minListYOffset.toPx() }
        AnimatedContent(targetState = messages, label = "chat_messages") { messages ->
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
                itemsIndexed(messages) { index, item ->
                    val paddingValue = when {
                        index == 0 -> 12.dp
                        item.isReceived && messages[index - 1].isReceived -> 2.dp
                        !item.isReceived && !messages[index - 1].isReceived -> 2.dp
                        else -> 12.dp
                    }
                    Message(item, paddingValue)
                }
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
                    .background(Color.LightGray, shape = CircleShape)
                    .padding(horizontal = with(density) { nextItemLayoutPadding.value.toDp() }),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement =
                if (openedEnoughToShowNextChatIcon()) Arrangement.SpaceAround
                else Arrangement.Center,
            ) {
                AnimatedVisibility(!openedEnough) {
                    Icon(
                        painterResource(R.drawable.ic_round_arrow_upward),
                        null,
                        tint = Color.White,
                        modifier = Modifier.padding(top = arrowUpMargin/*, bottom = arrowUpMargin/2*/)
                    )
                }
                Chat(
                    modifier = Modifier
                        .size(nextItemIconSize)
                        .aspectRatio(1f),
                    chat = nextUnreadChat ?: MainUiState.Chat(
                        "done",
                        R.drawable.ic_check,
                        imageTint = Color.White,
                        backgroundTint = Color.Transparent,
                    ),
                    showTitle = false,
                    showUnreadBadge = openedEnough,
                    unreadBadgeColor = Color.LightGray,
                    unreadBadgeBorderColor = Color.White,
                )
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
                label = "next_item_title",
            ) {
                Text(
                    text = nextUnreadChat?.title ?: "You have no unread chat",
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

    LaunchedEffect(messages) {
        lazyListState.scrollToItem(firstUnreadMessageIndex ?: 0)
    }
}

@Composable
private fun Message(
    item: MainUiState.Chat.Message,
    paddingFromPreviousMessage: Dp,
) {
    val clippedShape =
        if (item.isReceived) RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
        else RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
        if (item.isReceived) Arrangement.Start
        else Arrangement.End,
    ) {
        if (!item.isReceived) Spacer(modifier = Modifier.width(75.dp))
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = if (item.isReceived) Alignment.CenterStart
            else Alignment.CenterEnd,
        ) {
            Text(
                item.body,
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = paddingFromPreviousMessage)
                    .clip(clippedShape)
                    .background(Color.LightGray)
                    .padding(vertical = 8.dp),
            )
        }
        if (item.isReceived) Spacer(modifier = Modifier.width(75.dp))
    }
}
