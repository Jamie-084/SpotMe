package com.example.spotme

import com.google.firebase.ai.type.content

sealed class ChatItem {
    data class Message(val chatMessage: ChatMessage) : ChatItem()
    data class Graph(val graphView: android.view.View) : ChatItem()
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val options: List<String>? = null,
    val followUpPrompt : String? = null
// null = free-text, non-null = show option buttons
) {

    var content = content(role = if (isUser) "user" else "model") {
        text(text)
    }

}
