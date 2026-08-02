package com.example.nirapod.ui.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.nirapod.NirapodApplication
import com.example.nirapod.R
import com.example.nirapod.core.UiResult
import com.example.nirapod.data.model.AppUser
import com.example.nirapod.data.model.HazardReport
import com.example.nirapod.data.model.ReportStatus
import com.example.nirapod.data.model.UserRole
import com.example.nirapod.databinding.FragmentReportDetailBinding
import com.example.nirapod.ui.auth.AuthViewModel
import com.example.nirapod.ui.common.NirapodViewModelFactory
import com.example.nirapod.ui.report.ReportViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class ReportDetailFragment : Fragment() {
    private var _binding: FragmentReportDetailBinding? = null
    private val binding get() = _binding!!
    private val app by lazy { requireActivity().application as NirapodApplication }
    private val reportsViewModel: ReportViewModel by viewModels { NirapodViewModelFactory(app) }
    private val authViewModel: AuthViewModel by viewModels { NirapodViewModelFactory(app) }
    private val reportId by lazy { requireArguments().getString("reportId").orEmpty() }
    private var user: AppUser? = null
    private var currentReport: HazardReport? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReportDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val statuses = ReportStatus.entries.map { it.name }
        binding.statusDropdown.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, statuses))
        binding.btnConfirm.setOnClickListener {
            val uid = user?.uid ?: return@setOnClickListener
            reportsViewModel.confirm(reportId, uid)
        }
        binding.btnUpdateStatus.setOnClickListener {
            val authority = user ?: return@setOnClickListener
            val status = binding.statusDropdown.text.toString().ifBlank { ReportStatus.RECEIVED.name }
            reportsViewModel.updateStatus(reportId, status, authority.uid)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            user = authViewModel.currentUser()
            val role = user?.userRole()
            binding.authorityControls.isVisible = role == UserRole.AUTHORITY || role == UserRole.ADMIN
            binding.btnConfirm.isVisible = role == UserRole.CITIZEN
        }
        reportsViewModel.loadReport(reportId)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    reportsViewModel.detail.collect { state ->
                        binding.progress.isVisible = state is UiResult.Loading
                        when (state) {
                            is UiResult.Success -> bindReport(state.data)
                            is UiResult.Error -> Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                            else -> Unit
                        }
                    }
                }
                launch {
                    reportsViewModel.actionState.collect { state ->
                        when (state) {
                            is UiResult.Loading -> binding.progress.isVisible = true
                            is UiResult.Success -> {
                                binding.progress.isVisible = false
                                Snackbar.make(binding.root, "Update saved", Snackbar.LENGTH_SHORT).show()
                                reportsViewModel.clearActionState()
                            }
                            is UiResult.Error -> {
                                binding.progress.isVisible = false
                                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                                reportsViewModel.clearActionState()
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun bindReport(report: HazardReport) {
        currentReport = report
        binding.tvCategory.text = "${report.category} • ${report.severity}"
        binding.tvStatus.text = report.status.replace('_', ' ')
        binding.tvDescription.text = report.description
        binding.tvLocation.text = "Location: %.6f, %.6f".format(report.latitude, report.longitude)
        binding.tvMeta.text = "Reported by ${report.reporterName.ifBlank { "Community user" }} • ${report.confirmations} confirmations • ${DateFormat.getDateTimeInstance().format(Date(report.createdAt))}"
        binding.tvAi.text = buildString {
            appendLine("Suggested category: ${report.aiCategory.ifBlank { "Not available" }}")
            appendLine("Suggested severity: ${report.aiSeverity.ifBlank { "Not available" }}")
            appendLine("Suggested authority: ${report.assignedAuthority.ifBlank { "Not assigned" }}")
            append("Summary: ${report.aiSummary.ifBlank { "No AI summary" }}")
        }
        binding.aiCard.isVisible = report.aiSummary.isNotBlank()
        binding.statusDropdown.setText(report.status, false)
        if (report.imageUrl.isNotBlank()) {
            Glide.with(this).load(report.imageUrl).placeholder(R.drawable.ic_app_logo).error(R.drawable.ic_app_logo).into(binding.ivReport)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
