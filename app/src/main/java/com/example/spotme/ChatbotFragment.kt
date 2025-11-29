package com.example.spotme

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spotme.databinding.FragmentChatbotBinding


import com.google.android.material.chip.Chip
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import kotlinx.coroutines.launch


class ChatbotFragment : Fragment() {
    // To pass arguments to fragment, use this pattern
//    companion object {
//        fun newInstance() = ChatbotFragment()
//    }
    private val viewModel: PresageViewModel by activityViewModels()
    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = _binding!!

    private lateinit var model : GenerativeModel
    private lateinit var chat : Chat
    private lateinit var main : MainActivity


    private lateinit var adapter: ChatbotAdapter
    private val chatHistory = mutableListOf<ChatMessage>()
    private val questions = mutableListOf<ChatMessage>()
    val instructionText: Content? = content { """
        You are an assistant that helps user prepare for online job interviews.
        Analyse the current chat history and provide relevant and helpful responses.
        Use a friendly and professional tone.
        Responses should use simple text formatting, avoid markdown.
        If user seems ready for interview, call the StartMockInterview tool.
    """.trimIndent() }
    val toolDeclarations: List<FunctionDeclaration>
    = listOf(
        FunctionDeclaration(
            name = "StartMockInterview",
            description = "Starts a mock interview session.",
            parameters = mapOf()
        )
    )

    //    val interviewTool = com.google.ai.client.generativeai.common.client.Tool(toolDeclarations)



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatbotBinding.inflate(inflater, container, false)

        val chatItems : MutableList<ChatItem> = mutableListOf()

        adapter = ChatbotAdapter(chatItems)
        binding.recyclerMessages.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerMessages.adapter = adapter

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        main = requireActivity() as MainActivity

        viewModel.metricsDataLD.observe(viewLifecycleOwner) { metrics ->

            val pulses = viewModel.pulsesLD.value
            if (pulses.isNullOrEmpty() || pulses[0].value == 0) return@observe

            messageToScreenAndHistory(ChatMessage(text = "New metrics data received.", isUser = false))
            messageToScreenAndHistory(ChatMessage( viewModel.getAverages(20), isUser = false ))
//            chatHistory.add( ChatMessage( viewModel.getAverages(), isUser = false ) )
//            sendMessages( "Based on my health metrics during the mock interview, how do you think I did?" )

//            val chartContainer = LinearLayout(requireContext())
//            PresageGraphs.handleMetricsBuffer(metrics, chartContainer, requireContext())
//
//            val views = (0 until chartContainer.childCount).map { chartContainer.getChildAt(it) }
//            for (graphView in views) {
//                val params = ViewGroup.LayoutParams(
//                    ViewGroup.LayoutParams.MATCH_PARENT,
//                    400
//                )
//                graphView.layoutParams = params
//                (graphView.parent as? ViewGroup)?.removeView(graphView)
//                adapter.addGraph(graphView)
//            }

        }

        model = Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(
                modelName = "gemini-2.5-flash",
                systemInstruction = instructionText,
                tools = listOf(Tool.functionDeclarations(toolDeclarations))
            )

        setupQuestions()
        setupSendButton()
        nextQuestion()
    }

    private fun nextQuestion() : Boolean {
        if (questions.isEmpty()) {
            return false
        }
        val question = questions[0]
        questions.removeAt(0)
        messageToScreenAndHistory(question)
        if (!question.options.isNullOrEmpty()) { showBotOptions(question.options) }
        return true
    }

    private fun setupSendButton() {
        binding.buttonSend.setOnClickListener {
            val textInTextbox = binding.editMessage.text.toString().trim()
            if (textInTextbox.isEmpty()) return@setOnClickListener

            messageToScreenAndHistory(ChatMessage(text = textInTextbox, isUser = true))
            binding.editMessage.setText("")

            if (!nextQuestion()){
                sendMessages("Evaluate current chat history, provide a small summary, ask me if ready for interview")
            }
        }
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
                    messageToScreenAndHistory(ChatMessage(text = label, isUser = true))

                    nextQuestion()
                }
            }
            binding.optionButtonsContainer.addView(chip)
        }

//        binding.editMessage.isEnabled = false
//        binding.buttonSend.isEnabled = false
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

    private fun sendMessages ( prompt : String) {
        lifecycleScope.launch {

            val contentList = mutableListOf<Content>()
            chatHistory.forEach { c -> contentList.add (c.content) }

            chat = model.startChat( history = contentList as List<Content> )

            val response = chat.sendMessage(prompt)
            val functionCall: FunctionCallPart? = response.functionCalls.firstOrNull()
            if (functionCall != null) {
                val functionName = functionCall.name

                if (functionName == "StartMockInterview") {
                   main.switchFragment(main.presageFragment)
                } else {
                    println("Unknown function requested: $functionName")
                }

            } else {
                messageToScreenAndHistory(ChatMessage(text = response.text ?: "No response", isUser = false))
            }
        }
    }

    private fun messageToScreenAndHistory(message: ChatMessage) {
        adapter.addMessage(message)
        chatHistory.add(message)
        binding.recyclerMessages.scrollToPosition(adapter.itemCount - 1)
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