package com.mstf.jumptonextitem

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.lifecycle.viewmodel.compose.viewModel


@Composable
fun MainScreen(paddingValues: PaddingValues, viewModel: MainViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    val rootTopPadding by animateDpAsState(
        targetValue = if (state.selectedChat == null) paddingValues.calculateTopPadding() else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "root_top_padding",
    )

    val selectedChatBackgroundColor =
        if (state.selectedChat != null) Color(android.graphics.Color.parseColor(state.selectedChat!!.backgroundTintHex))
        else Color.Transparent

    val animatedImageColor by animateColorAsState(
        targetValue = selectedChatBackgroundColor,
        animationSpec = spring(),
        label = "background_color",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Gray)
            .padding(
                bottom = paddingValues.calculateBottomPadding(),
                top = rootTopPadding,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
        ) {
            AnimatedVisibility(
                visible = state.selectedChat == null,
                exit = shrinkHorizontally(spring(stiffness = Spring.StiffnessHigh)) +
                        fadeOut(spring(stiffness = Spring.StiffnessHigh)),
            ) {
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
            }
            Box(
                Modifier
                    .weight(1f)
                    .background(Color.White)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                state.selectedChat?.let { chat ->
                    ChatMessages(
                        chat = chat,
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
        AnimatedVisibility(
            visible = state.selectedChat != null,
            enter = slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { fullHeight -> -fullHeight },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(animatedImageColor)
                    .padding(bottom = 8.dp, top = paddingValues.calculateTopPadding() + 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Chat(
                    chat = state.selectedChat!!.copy(backgroundTintHex = "#00000000"),
                    showTitle = true,
                    titleStyle = TextStyle(
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    showUnreadBadge = false,
                )
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
        horizontalAlignment = Alignment.Start,
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
                isChatSelected = isChatSelected,
            )
        }
    }
}

@Composable
private fun Chat(
    modifier: Modifier = Modifier,
    chat: MainUiState.Chat,
    showTitle: Boolean,
    titleStyle: TextStyle = TextStyle(
        color = Color.Black,
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
    ),
    showUnreadBadge: Boolean = true,
    unreadBadgeColor: Color = Color(android.graphics.Color.parseColor("#B71C1C")),
    unreadBadgeBorderColor: Color = Color.LightGray,
    isChatSelected: Boolean = false,
) {
    val imageMargin by animateDpAsState(
        targetValue = if (isChatSelected) 0.dp else 6.dp,
        label = "next_item_size"
    )

    Row(
        modifier = modifier.sizeIn(50.dp),
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
            val chatColor = Color(android.graphics.Color.parseColor(chat.backgroundTintHex))
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(imageMargin)
                    .background(chatColor, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(chat.image),
                    contentDescription = null,
                    tint = chat.imageTint,
                    modifier = Modifier.padding(2.dp)
                )
            }
            AnimatedVisibility(
                visible = chat.unread && showUnreadBadge,
                modifier = Modifier.layoutId("unread_badge"),
                enter = scaleIn(animationSpec = spring()),
                exit = scaleOut(animationSpec = spring()),
                label = "unread_badge"
            ) {
                UnreadBadge(
                    unreadBadgeColor,
                    unreadBadgeBorderColor,
                    chat.firstUnreadIndex,
                )
            }
        }
        if (showTitle) {
            AnimatedContent(
                targetState = chat.title,
                transitionSpec = {
                    slideInVertically { height -> -height } + fadeIn(tween(500)) togetherWith
                            slideOutVertically { height -> height } + fadeOut(tween(100))
                },
                label = "chat_title"
            ) { title ->
                Text(
                    title,
                    style = titleStyle,
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(start = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun UnreadBadge(
    unreadBadgeColor: Color,
    unreadBadgeBorderColor: Color,
    firstUnreadIndex: Int?,
) {
    Box(
        modifier = Modifier
            .sizeIn(minWidth = 20.dp)
            .background(unreadBadgeColor, shape = CircleShape)
            .border(width = 2.dp, color = unreadBadgeBorderColor, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        val unreadCount =
            firstUnreadIndex.let {
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
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
    }
}

@Composable
private fun ChatMessages(
    modifier: Modifier = Modifier,
    chat: MainUiState.Chat,
    nextUnreadChat: MainUiState.Chat?,
    onJumpToNextChat: (MainUiState.Chat) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val messages = remember(chat) { chat.messages }

    JumpToNextItemList(
        modifier = modifier
            .fillMaxSize(),
        lazyListState = lazyListState,
        itemList = messages,
        nextItem = nextUnreadChat,
        onJumpToNextItem = onJumpToNextChat,
        listItemContent = { index, item ->
            if (index >= messages.size) return@JumpToNextItemList

            val previousMessageHasSameSender = when {
                index == 0 -> false
                item.isReceived && messages[index - 1].isReceived -> true
                !item.isReceived && !messages[index - 1].isReceived -> true
                else -> false
            }
            val nextMessageHasSameSender = when {
                index == messages.indices.last -> false
                item.isReceived && messages[index + 1].isReceived -> true
                !item.isReceived && !messages[index + 1].isReceived -> true
                else -> false
            }
            Message(item, index, previousMessageHasSameSender, nextMessageHasSameSender)

            if (index == chat.firstUnreadIndex) UnreadMessagesDivider()
        },
        nextItemContent = { contentSize, swipedEnough ->
            Chat(
                modifier = Modifier
                    .size(contentSize)
                    .aspectRatio(1f),
                chat = nextUnreadChat ?: MainUiState.Chat(
                    "done",
                    R.drawable.ic_check,
                    imageTint = Color.White,
                    backgroundTintHex = "#00000000",
                ),
                showTitle = false,
                showUnreadBadge = swipedEnough,
                unreadBadgeColor = Color.LightGray,
                unreadBadgeBorderColor = Color.White,
                isChatSelected = true,
            )
        },
        nextItemLabel = nextUnreadChat?.title ?: "You have no unread chat",
    )

    LaunchedEffect(chat) {
        lazyListState.scrollToItem(chat.firstUnreadIndex ?: 0)
    }
}

@Composable
private fun UnreadMessagesDivider() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(2.dp)
                .background(Color.Red.copy(alpha = 0.5f)),
        )
        Text(
            text = "Unread Messages",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Box(
            Modifier
                .weight(1f)
                .height(2.dp)
                .background(Color.Red.copy(alpha = 0.5f)),
        )
    }
}

@Composable
private fun Message(
    item: MainUiState.Chat.Message,
    position: Int,
    previousMessageHasSameSender: Boolean,
    nextMessageHasSameSender: Boolean,
) {
    val paddingFromPreviousMessage = when {
        position == 0 -> 8.dp
        item.isReceived && previousMessageHasSameSender -> 2.dp
        !item.isReceived && previousMessageHasSameSender -> 2.dp
        else -> 12.dp
    }

    val clippedShape =
        if (item.isReceived)
            RoundedCornerShape(
                topStart = if (nextMessageHasSameSender) 0.dp else 16.dp,
                bottomStart = if (previousMessageHasSameSender) 0.dp else 16.dp,
                topEnd = 16.dp,
                bottomEnd = 16.dp,
            )
        else RoundedCornerShape(
            topStart = 16.dp,
            bottomStart = 16.dp,
            topEnd = if (nextMessageHasSameSender) 0.dp else 16.dp,
            bottomEnd = if (previousMessageHasSameSender) 0.dp else 16.dp,
        )

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
