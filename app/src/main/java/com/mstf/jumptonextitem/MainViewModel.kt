package com.mstf.jumptonextitem

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Blue
import androidx.compose.ui.graphics.Color.Companion.Cyan
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.Magenta
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.Color.Companion.Yellow
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {

    private var _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = uiState.value.copy(
            chats = listOf(
                makeChat("Mr. White", White),
                makeChat("Mr. Black", Black),
                makeChat("Mr. Green", Green),
                makeChat("Mr. Red", Red),
                makeChat("Mr. Blue", Blue),
                makeChat("Mr. Gray", Gray),
                makeChat("Mr. Yellow", Yellow),
                makeChat("Mr. Cyan", Cyan),
                makeChat("Mr. Magenta", Magenta),
            ),
        )
    }

    private fun makeChat(title: String, tint: Color, unread: Boolean = false): MainUiState.Chat =
        MainUiState.Chat(
            title = title,
            image = R.drawable.ic_account,
            tint = tint,
            messages = (1..10).map {
                var text = ""
                repeat((30..250).random()) { text += "a" }
                text
            },
            unread = unread,
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
        val messages: List<String> = arrayListOf(),
        val unread: Boolean = false,
    )
}