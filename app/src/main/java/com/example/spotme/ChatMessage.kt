package com.example.spotme

import com.google.firebase.ai.type.content

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val options: List<String>? = null    // null = free-text, non-null = show option buttons
) {

    var content = content(role = if (isUser) "user" else "model") {
        text(text)
    }

}
