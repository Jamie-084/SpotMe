package com.example.spotme

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.example.spotme.databinding.FragmentChatbotBinding
import com.example.spotme.databinding.FragmentPresageBinding
import com.example.spotme.databinding.FragmentReportBinding
import kotlin.getValue

class ReportFragment : Fragment() {
    companion object {
        fun newInstance() = ChatbotFragment()
    }

    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportBinding.inflate(inflater, container, false)
        return binding.root
    }

}