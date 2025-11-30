package com.example.spotme

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.example.spotme.databinding.FragmentChatbotBinding
import com.example.spotme.databinding.FragmentPreinterviewBinding
import com.example.spotme.databinding.FragmentPresageBinding
import kotlin.getValue

class PreinterviewFragment : Fragment() {

    //private val viewModel: PresageViewModel by viewModels()
    private var _binding: FragmentPreinterviewBinding? = null
    private val binding get() = _binding!!
    private lateinit var main : MainActivity

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPreinterviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val continueBtn = binding.btnContinue
        main = requireActivity() as MainActivity

        binding.introBullets.text = """
            • The next page will record your response to an interview question
            • When you have read the question, press 'CHECKUP' to start
            • You will have 20 seconds to answer the question
            • Make sure you memorise the question before starting
        """.trimIndent()


        continueBtn.setOnClickListener {
            main.switchFragment(main.presageFragment)
        }
    }

}