package com.mstf.jumptonextitem

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Blue
import androidx.compose.ui.graphics.Color.Companion.Cyan
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.Magenta
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
                makeChat("Mr. White", White, unread = true, firstUnreadIndex = 3),
                makeChat("Mr. Black", Black),
                makeChat("Mr. Green", Green, unread = true, firstUnreadIndex = 110),
                makeChat("Mr. Blue", Blue),
                makeChat("Mr. Gray", Gray),
                makeChat("Mr. Yellow", Yellow, unread = true, firstUnreadIndex = 75),
                makeChat("Mr. Cyan", Cyan),
                makeChat("Mr. Magenta", Magenta, unread = true, firstUnreadIndex = 9),
            ),
        )
    }

    private fun makeChat(
        title: String,
        tint: Color,
        unread: Boolean = false,
        firstUnreadIndex: Int = 0,
    ) = MainUiState.Chat(
        title = title,
        image = R.drawable.ic_account,
        tint = tint,
        messages = (1..(150..200).random()).map {
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
        firstUnreadIndex = firstUnreadIndex,
    )

    fun onChatSelect(chat: MainUiState.Chat) {
        _uiState.value = uiState.value.copy(
            selectedChat = chat,
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
        val tint: Color,
        val messages: List<Message> = arrayListOf(),
        val unread: Boolean = false,
        val firstUnreadIndex: Int = 0,
    ) {
        data class Message(
            val body: String,
            val isReceived: Boolean,
        )
    }
}