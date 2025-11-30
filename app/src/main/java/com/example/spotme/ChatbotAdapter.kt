package com.example.spotme

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.spotme.databinding.ItemMessageBotBinding
import com.example.spotme.databinding.ItemMessageUserBinding




class ChatbotAdapter(
    private val items: MutableList<ChatItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_USER = 0
        private const val TYPE_BOT = 1
        private const val TYPE_GRAPH = 2

    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = items[position]) {
            is ChatItem.Message -> if (item.chatMessage.isUser) TYPE_USER else TYPE_BOT
            is ChatItem.Graph -> TYPE_GRAPH
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_USER -> UserViewHolder(ItemMessageUserBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            TYPE_BOT -> BotViewHolder(ItemMessageBotBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            TYPE_GRAPH -> GraphViewHolder(FrameLayout(parent.context))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ChatItem.Message -> {
                if (holder is UserViewHolder) holder.bind(item.chatMessage)
                if (holder is BotViewHolder) holder.bind(item.chatMessage)
            }
            is ChatItem.Graph -> {
                if (holder is GraphViewHolder) holder.bind(item.graphView)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun addMessage(message: ChatMessage) {
        items.add(ChatItem.Message(message))
        notifyItemInserted(items.size - 1)
    }

    fun addGraph(graphView: android.view.View) {
        items.add(ChatItem.Graph(graphView))
        notifyItemInserted(items.size - 1)
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

    class GraphViewHolder(private val frame: FrameLayout) : RecyclerView.ViewHolder(frame) {
        fun bind(graphView: android.view.View) {
            frame.removeAllViews()
            frame.addView(graphView)
        }
    }

}
