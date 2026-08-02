package com.example.nirapod.ui.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nirapod.NirapodApplication
import com.example.nirapod.R
import com.example.nirapod.databinding.FragmentReportListBinding
import com.example.nirapod.ui.auth.AuthViewModel
import com.example.nirapod.ui.common.NirapodViewModelFactory
import com.example.nirapod.ui.report.ReportViewModel
import kotlinx.coroutines.launch

class MyReportsFragment : Fragment() {
    private var _binding: FragmentReportListBinding? = null
    private val binding get() = _binding!!
    private val app by lazy { requireActivity().application as NirapodApplication }
    private val reportsViewModel: ReportViewModel by viewModels { NirapodViewModelFactory(app) }
    private val authViewModel: AuthViewModel by viewModels { NirapodViewModelFactory(app) }
    private val adapter = ReportAdapter { report ->
        findNavController().navigate(R.id.action_my_reports_to_detail, bundleOf("reportId" to report.id))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReportListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.tvTitle.text = "My Reports"
        binding.recyclerReports.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerReports.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.currentUser()?.let { reportsViewModel.observeMyReports(it.uid) }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                reportsViewModel.myReports.collect { list ->
                    adapter.submitList(list)
                    binding.tvEmpty.isVisible = list.isEmpty()
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.recyclerReports.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
