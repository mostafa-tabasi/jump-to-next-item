package com.mstf.jumptonextitem

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Blue
import androidx.compose.ui.graphics.Color.Companion.Cyan
import androidx.compose.ui.graphics.Color.Companion.DarkGray
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.Magenta
import androidx.compose.ui.graphics.Color.Companion.Unspecified
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.Color.Companion.Yellow
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class MainViewModel : ViewModel() {

    private var _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = uiState.value.copy(
            chats = listOf(
                makeChat("Mr. White", White),
                makeChat("Mr. Black", Black, imageTint = DarkGray),
                makeChat("Mr. Blue", Blue),
                makeChat("Mr. Magenta", Magenta),
                makeChat("Mr. Gray", Gray),
                makeChat("Mr. Yellow", Yellow),
                makeChat("Mr. Cyan", Cyan),
                makeChat("Mr. Green", Green),
            ),
        )
    }

    private fun makeChat(
        title: String,
        tint: Color,
        imageTint: Color = Unspecified,
    ): MainUiState.Chat {
        val messagesCount = (5..30).random()
        val unread = Random.nextBoolean()

        return MainUiState.Chat(
            title = title,
            image = R.drawable.ic_account,
            imageTint = imageTint,
            backgroundTint = tint,
            messages = (3..messagesCount).map {
                var text = ""
                repeat((15..100).random()) {
                    /*
                    text += "A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y, z,  "
                        .split(", ").random()
                    */
                    text += " "
                }
                // text = "$it. $text"
                repeat((0..2).random()) { text += "\n" }

                MainUiState.Chat.Message(body = text, isReceived = Random.nextBoolean())
            },
            unread = unread,
            firstUnreadIndex = if (unread) (1..messagesCount).random() else null,
        )
    }

    fun onChatSelect(chat: MainUiState.Chat) {
        val chatIndex = uiState.value.chats.indexOf(chat)
        val updatedChat = chat.copy(unread = false, firstUnreadIndex = null)

        _uiState.value = uiState.value.copy(
            chats = uiState.value.chats.toMutableList().also { it[chatIndex] = updatedChat },
            selectedChat = updatedChat,
        )
    }
}

data class MainUiState(
    val chats: List<Chat> = arrayListOf(),
    val selectedChat: Chat? = null,
) {
    data class Chat(
        val title: String,
        @DrawableRes val image: Int,
        val imageTint: Color = Unspecified,
        val backgroundTint: Color,
        val messages: List<Message> = arrayListOf(),
        val unread: Boolean = false,
        val firstUnreadIndex: Int? = null,
    ) {
        data class Message(
            val body: String,
            val isReceived: Boolean,
        )
    }
}