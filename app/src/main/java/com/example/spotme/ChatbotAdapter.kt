package com.example.spotme

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.spotme.databinding.ItemMessageBotBinding
import com.example.spotme.databinding.ItemMessageUserBinding
import com.example.spotme.ChatMessage

class ChatbotAdapter(
    private val messages: MutableList<ChatMessage>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_USER = 0
        private const val TYPE_BOT = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) TYPE_USER else TYPE_BOT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_USER) {
            val binding = ItemMessageUserBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            UserViewHolder(binding)
        } else {
            val binding = ItemMessageBotBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            BotViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        if (holder is UserViewHolder) holder.bind(msg)
        if (holder is BotViewHolder) holder.bind(msg)
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    class UserViewHolder(private val b: ItemMessageUserBinding)
        : RecyclerView.ViewHolder(b.root) {

        fun bind(msg: ChatMessage) {
            b.textMessageUser.text = msg.text
            b.textMessageTimeUser.visibility = View.GONE
        }
    }

    class BotViewHolder(private val b: ItemMessageBotBinding)
        : RecyclerView.ViewHolder(b.root) {

        fun bind(msg: ChatMessage) {
            b.textMessageBot.text = msg.text
            b.textMessageTimeBot.visibility = View.GONE
        }
    }
}
