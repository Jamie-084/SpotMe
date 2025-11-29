package com.example.spotme

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spotme.databinding.FragmentChatbotBinding
import com.google.android.material.chip.Chip
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.launch


class ChatbotFragment : Fragment() {
    // To pass arguments to fragment, use this pattern
//    companion object {
//        fun newInstance() = ChatbotFragment()
//    }
    //private val viewModel: PresageViewModel by viewModels()
    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = _binding!!

    private lateinit var model : GenerativeModel
    private lateinit var chat : Chat

    private lateinit var adapter: ChatbotAdapter
    private val chatHistory = mutableListOf<ChatMessage>()
    private val questions = mutableListOf<ChatMessage>()
    val instructionText: Content? = content { """
        You are an assistant that helps user prepare for online job interviews.
        Analyse the current chat history and provide relevant and helpful responses.
        Use a friendly and professional tone.
    """.trimIndent() }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatbotBinding.inflate(inflater, container, false)

        adapter = ChatbotAdapter(chatHistory)
        binding.recyclerMessages.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerMessages.adapter = adapter

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        model = Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(
                modelName = "gemini-2.5-flash",
                systemInstruction = instructionText
            )

        setupQuestions()
        setupSendButton()
        addMessage(questions[0])
        questions.removeAt(0)
    }

    private fun setupSendButton() {
        binding.buttonSend.setOnClickListener {
            val textInTextbox = binding.editMessage.text.toString().trim()
            if (textInTextbox.isEmpty()) return@setOnClickListener

            // Send user message, clear input box
            addMessage(ChatMessage(text = textInTextbox, isUser = true))
            binding.editMessage.setText("")


            if (questions.isEmpty()) {
                sendMessages()
                return@setOnClickListener
            }

            addMessage(questions[0])
            questions.removeAt(0)
        }
    }

    private fun setupQuestions() {
        val questionList = listOf(
            "Enter a job description to get started.",
            "What are your strengths",
            "... and your weaknesses?",
            "Whats your experience with online interviews?",
            "What would you like to practice today?",
            "Last question: Any thing else?"
        )

        val optionsList = listOf(
            listOf("Software Engineer", "Data Scientist", "Product Manager"),
            listOf("Communication", "Problem-solving", "Teamwork"),
            listOf("Very experienced", "Some experience", "New to it"),
            listOf("Behavioral questions", "Technical questions", "Both"),
            listOf("No, I'm ready", "Yes, tell me more")
        )

        questionList.forEach { q ->
            questions += ChatMessage(
                text = q,
                isUser = false,
                options = optionsList.getOrNull(questions.size)
            )
        }
    }

    private fun sendMessages () {
        lifecycleScope.launch {

            val contentList = mutableListOf<Content>()
            chatHistory.forEach { c -> contentList.add (c.content) }

            chat = model.startChat( history = contentList as List<Content> )

            val response = chat.sendMessage("Give me a short summary of my progress so far.")
            addMessage(ChatMessage(text = response.text ?: "No response", isUser = false))
        }
    }

    private fun addMessage(message: ChatMessage) {
        adapter.addMessage(message)
        binding.recyclerMessages.scrollToPosition(adapter.itemCount - 1)
    }

    private fun showBotOptions(options: List<String>) {
        binding.optionButtonsContainer.visibility = View.VISIBLE
        binding.optionButtonsContainer.removeAllViews()

        options.forEach { label ->
            val chip = Chip(requireContext()).apply {
                text = label
                isCheckable = false
                setOnClickListener {
                    hideBotOptions()
                    addMessage(ChatMessage(text = label, isUser = true))
                    addMessage(ChatMessage(text = "Received: $label", isUser = false))
                }
            }
            binding.optionButtonsContainer.addView(chip)
        }

        binding.editMessage.isEnabled = false
        binding.buttonSend.isEnabled = false
    }

    private fun hideBotOptions() {
        binding.optionButtonsContainer.visibility = View.GONE
        binding.optionButtonsContainer.removeAllViews()

        binding.editMessage.isEnabled = true
        binding.buttonSend.isEnabled = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

//    private fun simulateBotResponse(userMessage: ChatMessage) {
//        // Example decision logic: first bot sends text, second time requests options
//        if (chatHistory.count { !it.isUser } == 0) {
//            addMessage(ChatMessage(
//                text = "Choose one of the following options:",
//                isUser = false,
//                options = listOf("Option A", "Option B", "Option C")
//            ))
//            showBotOptions(chatHistory.last().options!!)
//        } else {
//            addMessage(ChatMessage(text = "Previous message: " + userMessage.text, isUser = false))
//            hideBotOptions()
//        }
//    }

}