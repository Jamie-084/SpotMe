package com.example.spotme

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
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
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import kotlinx.coroutines.launch
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive


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
    private var evaluationMessage = "Provide a small summary of what has been mentioned so far, ask me if ready for interview"

    val instructionText: Content? = content { """
        You are an assistant that helps users prepare for online job interviews.
        Provide summaries of current chat history when requested.
        Use a friendly and professional tone.
        Responses should use simple text formatting.
    """.trimIndent() }
    val toolDeclarations: List<FunctionDeclaration>
    = listOf(
        FunctionDeclaration(
            name = "StartMockInterview",
            description = "Starts a mock interview session.",
            parameters = mapOf("question" to Schema.string("A single interview question to ask the user. Auto generate this if not already provided"), "length" to Schema.integer("Length of interview in seconds. Default to 20") )
        ),
        FunctionDeclaration(
            name = "SendMessage",
            description = "Responds to the user message based on chat history.",
            parameters = mapOf("response" to Schema.string("Response message to send to the user."))
        ),
        FunctionDeclaration(
            name = "StartPostInterviewFeedback",
            description = "Starts the post-interview feedback session.",
            parameters = mapOf()
        )
    )

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
//            questions.clear()
//            evaluationMessage = ""
            val pulses = viewModel.pulsesLD.value
            if (pulses.isNullOrEmpty() || pulses[0] == 0) return@observe
            messageToScreenAndHistory(ChatMessage( text = "Metric data: \n" + viewModel.getAverages(20), isUser = true ))
            messageToScreenAndHistory(ChatMessage(text = "Interview speech: \n" + viewModel.speechTextLD.value, isUser = true))

            sendMessages( "Give a short summary on how I did, based off my metric data and interview chat. " +
                    "Relate back to the interview question" +
                    "Ask if I want to start the post interview feedback session" )
        }

        model = Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(
                modelName = "gemini-2.5-flash",
                systemInstruction = instructionText,
                tools = listOf(Tool.functionDeclarations(toolDeclarations))
            )

        messageToScreenAndHistory(ChatMessage(text = "Hello! I'm your interview preparation assistant. Let's get started!", isUser = false))
        setupInitialQuestions()
        setupSendButton()
        nextQuestion()
    }

    private fun nextQuestion() : Boolean {
        if (questions.isEmpty()) {
            if (evaluationMessage.isNotEmpty()) {
                adapter.addMessage(ChatMessage(text = evaluationMessage, isUser = true)) /// remove later
                sendMessages(evaluationMessage)
                evaluationMessage = ""
                return true
            }
            return false
        }
        val question = questions.removeAt(0)
        messageToScreenAndHistory(question)
        if (!question.options.isNullOrEmpty()) { showBotOptions(question.options) }
        return true
    }

    private fun setupSendButton() {
        binding.buttonSend.setOnClickListener {
            val textInTextbox = binding.editMessage.text.toString().trim()
            if (textInTextbox.isEmpty()) return@setOnClickListener

            // hide keyboard
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.editMessage.windowToken, 0)
            binding.editMessage.clearFocus()

            val userMessage = ChatMessage(text = textInTextbox, isUser = true)
            messageToScreenAndHistory(userMessage)
            binding.editMessage.setText("")

            if (questions.isEmpty()) {
                if (evaluationMessage.isNotEmpty()) {
                    nextQuestion()
                } else {
                    sendMessages(textInTextbox)
                }
            } else {
                nextQuestion()
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

                    if (questions.isEmpty()) {
                        if (evaluationMessage.isNotEmpty()) {
                            nextQuestion()
                        } else {
                            sendMessages(label)
                        }
                    } else {
                        nextQuestion()
                    }
                }
            }
            binding.optionButtonsContainer.addView(chip)
        }

//        binding.editMessage.isEnabled = false
//        binding.buttonSend.isEnabled = false
    }



    private fun sendMessages ( prompt : String) {
        lifecycleScope.launch {

            hideBotOptions()

            val contentList = mutableListOf<Content>()
            chatHistory.forEach { c -> contentList.add (c.content) }

            chat = model.startChat( history = contentList as List<Content> )

            val response = chat.sendMessage(prompt)
            var functionCall = response.functionCalls.find { it.name == "StartMockInterview" }
            if (functionCall != null) {
                val question = functionCall.args["question"]!!.jsonPrimitive.content
                val interviewLength = functionCall.args["length"]?.jsonPrimitive?.doubleOrNull ?: 20

//                main.presageFragment.updateMeasurementDuration(interviewLength as Double)
                viewModel.updateQuestion(question)
                main.switchFragment(main.preinterviewFragment)
                return@launch
            }
            functionCall = response.functionCalls.find { it.name == "StartPostInterviewFeedback" }
            if (functionCall != null) {
                evaluationMessage = "Consider the previous chat history. Provide answers to questions asked. Then ask what they want to cover next"
                setupFinalQuestions()
                setupSendButton()
                nextQuestion()
                return@launch
            }
            functionCall = response.functionCalls.find { it.name == "SendMessage" }
            if (functionCall != null) {
                val responseText = functionCall.args["response"]!!.jsonPrimitive.content
                messageToScreenAndHistory(ChatMessage(text = responseText, isUser = false))
                return@launch
            }
            messageToScreenAndHistory(ChatMessage(text = response.text ?: "No response", isUser = false))
        }
    }

    private fun setupInitialQuestions() {
        questions.clear()
        val questionList = listOf(
            "Enter a job description or select a predefined role",
            "Tell me about yourself: What are your interests and career goals?",
            "What are your strengths",
            "... and your weaknesses?",
            "Whats your experience with online interviews?",
            "Would you like to focus on a specific set of skills?"
        )

        val optionsList = listOf(
            listOf("Software Engineer", "Data Scientist", "Product Manager", "Retail"),
            listOf("Skip"),
            listOf("Communication", "Problem-solving", "Teamwork", "Leadership", "Adaptability", "Creativity"),
            listOf("Communication", "Problem-solving", "Teamwork", "Leadership", "Adaptability", "Creativity"),
            listOf("Very experienced", "Some experience", "New to it"),
            listOf("Behavioral questions", "Technical questions", "Confidence")
        )
        questionList.forEach { q ->
            questions += ChatMessage(
                text = q,
                isUser = false,
                options = optionsList.getOrNull(questions.size)
            )
        }
    }

    private fun setupFinalQuestions() {
        questions.clear()
        val questionList = listOf(
            "How do you feel about your interview readiness now?",
            "What areas would you like to improve further?",
            "Would you like tips on handling difficult questions?"
        )

        val optionsList = listOf(
            listOf("Very confident", "Somewhat ready", "Need more practice"),
            listOf("Technical skills", "Behavioral skills", "Confidence"),
            listOf("Yes, please", "No, thank you")
        )
        questionList.forEach { q ->
            questions += ChatMessage(
                text = q,
                isUser = false,
                options = optionsList.getOrNull(questions.size)
            )
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