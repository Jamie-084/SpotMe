package com.example.spotme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.spotme.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    //private val viewModel: PresageViewModel by viewModels()
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val main = requireActivity() as MainActivity

        binding.textView2.text = """
            
            This app incorporates AI and physiological sensing to help you prepare for job interviews.
            
            Ready to get started?
        """.trimIndent()

        binding.btnBegin.setOnClickListener {
            main.switchFragment(main.chatbotFragment)
        }

    }


}