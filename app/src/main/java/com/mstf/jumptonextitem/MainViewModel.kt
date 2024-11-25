package com.mstf.jumptonextitem

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Unspecified
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

class MainViewModel : ViewModel() {

    private var _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            uiState.value.copy(
                chats = listOf(
                    makeChat("Mr. Brown", "#795548"),
                    makeChat("Mr. Orange", "#FB8C00"),
                    makeChat("Mr. Green", "#558B2F"),
                    makeChat("Mr. Teal", "#26A69A"),
                    makeChat("Mr. Gray", "#455A64"),
                    makeChat("Mr. Blue", "#1565C0"),
                    makeChat("Mr. Purple", "#6A1B9A"),
                    makeChat("Mr. Red", "#F44336"),
                ),
            )
        }
    }

    private fun makeChat(
        title: String,
        tintHex: String,
        imageTint: Color = White,
    ): MainUiState.Chat {
        val messagesCount = (5..30).random()
        val unread = Random.nextBoolean()

        return MainUiState.Chat(
            title = title,
            image = R.drawable.ic_account,
            imageTint = imageTint,
            backgroundTintHex = tintHex,
            messages = (1..messagesCount).map {
                var text = ""
                repeat((15..75).random()) {
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
            firstUnreadIndex = if (unread) (0 until messagesCount / 3).random() else null,
        )
    }

    fun onChatSelect(chat: MainUiState.Chat) {
        val chatIndex = uiState.value.chats.indexOf(chat)
        val updatedChat = chat.copy(unread = false)

        _uiState.update {
            uiState.value.copy(
                chats = uiState.value.chats.toMutableList().also { it[chatIndex] = updatedChat },
                selectedChat = updatedChat,
            )
        }
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
        val backgroundTintHex: String,
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