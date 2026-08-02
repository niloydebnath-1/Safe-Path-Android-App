package com.example.nirapod.ui.authority

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nirapod.NirapodApplication
import com.example.nirapod.R
import com.example.nirapod.data.model.AppUser
import com.example.nirapod.data.model.ApprovalStatus
import com.example.nirapod.data.model.AuthorityType
import com.example.nirapod.data.model.HazardReport
import com.example.nirapod.data.model.ReportStatus
import com.example.nirapod.data.model.SosAlert
import com.example.nirapod.data.model.UserRole
import com.example.nirapod.databinding.FragmentAuthorityBinding
import com.example.nirapod.ui.auth.AuthViewModel
import com.example.nirapod.ui.common.NirapodViewModelFactory
import com.example.nirapod.ui.report.ReportViewModel
import com.example.nirapod.ui.reports.ReportAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class AuthorityFragment : Fragment() {

    private var _binding:
            FragmentAuthorityBinding? = null

    private val binding
        get() = _binding!!

    private val app by lazy {
        requireActivity().application
                as NirapodApplication
    }

    private val authViewModel:
            AuthViewModel by viewModels {
        NirapodViewModelFactory(app)
    }

    private val reportsViewModel:
            ReportViewModel by viewModels {
        NirapodViewModelFactory(app)
    }

    private val sosViewModel:
            SosViewModel by viewModels {
        NirapodViewModelFactory(app)
    }

    private val adapter =
        ReportAdapter { report ->

            findNavController().navigate(
                R.id.action_authority_to_detail,
                bundleOf(
                    "reportId" to report.id
                )
            )
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentAuthorityBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        binding.recyclerReports
            .layoutManager =
            LinearLayoutManager(
                requireContext()
            )

        binding.recyclerReports.adapter =
            adapter

        binding.btnLogout
            .setOnClickListener {
                authViewModel.logout()

                findNavController().navigate(
                    R.id.action_authority_to_login
                )
            }

        verifyUserAndLoadPanel()
    }

    private fun verifyUserAndLoadPanel() {

        viewLifecycleOwner.lifecycleScope.launch {

            val user =
                runCatching {
                    authViewModel.currentUser()
                }.getOrNull()

            val approved =
                user?.approvalStatus ==
                        ApprovalStatus.APPROVED.name

            val validRole =
                user?.userRole() ==
                        UserRole.AUTHORITY ||
                        user?.userRole() ==
                        UserRole.ADMIN

            if (
                user == null ||
                !approved ||
                !validRole
            ) {
                authViewModel.logout()

                findNavController().navigate(
                    R.id.action_authority_to_login
                )

                return@launch
            }

            binding.tvTitle.text =
                if (
                    user.userRole() ==
                    UserRole.ADMIN
                ) {
                    "Authority Control Room • Admin"
                } else {
                    "Authority Control Room"
                }

            binding.tvAuthorityType.text =
                user.roleLabel()

            observeAuthorityData(user)
        }
    }

    private suspend fun observeAuthorityData(
        user: AppUser
    ) {
        viewLifecycleOwner.repeatOnLifecycle(
            Lifecycle.State.STARTED
        ) {
            launch {
                reportsViewModel
                    .reports
                    .collect { allReports ->

                        val visibleReports =
                            filterReportsForUser(
                                allReports,
                                user
                            )

                        adapter.submitList(
                            visibleReports
                        )

                        val open =
                            visibleReports.count {
                                it.status !=
                                        ReportStatus
                                            .RESOLVED.name &&
                                        it.status !=
                                        ReportStatus
                                            .REJECTED.name
                            }

                        val serious =
                            visibleReports.count {
                                it.severity ==
                                        "HIGH" ||
                                        it.severity ==
                                        "CRITICAL"
                            }

                        binding.tvSummary.text =
                            "${visibleReports.size} total • " +
                                    "$open open • " +
                                    "$serious high/critical"
                    }
            }

            launch {
                sosViewModel
                    .alerts
                    .collect { alerts ->
                        renderSos(alerts)
                    }
            }
        }
    }

    private fun filterReportsForUser(
        reports: List<HazardReport>,
        user: AppUser
    ): List<HazardReport> {

        if (
            user.userRole() ==
            UserRole.ADMIN
        ) {
            return reports
        }

        return reports.filter { report ->

            val category =
                report.aiCategory
                    .ifBlank {
                        report.category
                    }
                    .uppercase()

            when (user.authorityType) {

                AuthorityType
                    .CITY_CORPORATION.name ->

                    listOf(
                        "MANHOLE",
                        "ROAD",
                        "DRAIN",
                        "ELECTRICAL",
                        "OTHER"
                    ).any {
                        category.contains(it)
                    }

                AuthorityType
                    .DISASTER_MANAGEMENT_BOARD
                    .name ->

                    listOf(
                        "WATER",
                        "FLOOD",
                        "FIRE",
                        "DISASTER"
                    ).any {
                        category.contains(it)
                    }

                AuthorityType.POLICE.name ->

                    listOf(
                        "CRIME",
                        "SNATCH",
                        "ROBBERY",
                        "THEFT",
                        "ASSAULT"
                    ).any {
                        category.contains(it)
                    }

                else -> false
            }
        }
    }

    private fun renderSos(
        alerts: List<SosAlert>
    ) {
        binding.sosContainer
            .removeAllViews()

        if (alerts.isEmpty()) {
            binding.sosContainer.addView(
                TextView(
                    requireContext()
                ).apply {
                    text =
                        "No active prototype SOS alerts"
                }
            )

            return
        }

        alerts.forEach { alert ->

            val card =
                MaterialCardView(
                    requireContext()
                ).apply {

                    setContentPadding(
                        14,
                        12,
                        14,
                        12
                    )

                    val content =
                        LinearLayout(
                            context
                        ).apply {
                            orientation =
                                LinearLayout.VERTICAL

                            addView(
                                TextView(context).apply {
                                    text =
                                        "SOS: ${
                                            alert.userName
                                                .ifBlank {
                                                    alert.userId
                                                }
                                        }"

                                    textSize = 17f

                                    setTypeface(
                                        typeface,
                                        android.graphics
                                            .Typeface.BOLD
                                    )
                                }
                            )

                            addView(
                                TextView(context).apply {
                                    text =
                                        "%.6f, %.6f • %s"
                                            .format(
                                                alert.latitude,
                                                alert.longitude,
                                                DateFormat
                                                    .getDateTimeInstance()
                                                    .format(
                                                        Date(
                                                            alert.createdAt
                                                        )
                                                    )
                                            )
                                }
                            )

                            addView(
                                MaterialButton(context)
                                    .apply {
                                        text =
                                            "Mark resolved"

                                        setOnClickListener {
                                            sosViewModel
                                                .resolve(
                                                    alert.id
                                                )
                                        }
                                    }
                            )
                        }

                    addView(content)
                }

            binding.sosContainer
                .addView(card)
        }
    }

    override fun onDestroyView() {
        binding.recyclerReports.adapter =
            null

        _binding = null
        super.onDestroyView()
    }
}