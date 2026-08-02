package com.example.nirapod.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.nirapod.NirapodApplication
import com.example.nirapod.R
import com.example.nirapod.core.UiResult
import com.example.nirapod.data.model.AppUser
import com.example.nirapod.data.model.ApprovalStatus
import com.example.nirapod.data.model.UserRole
import com.example.nirapod.databinding.FragmentAdminBinding
import com.example.nirapod.ui.auth.AuthViewModel
import com.example.nirapod.ui.common.NirapodViewModelFactory
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class AdminFragment : Fragment() {

    private var _binding:
            FragmentAdminBinding? = null

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentAdminBinding.inflate(
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
        binding.btnOpenAuthorityRoom
            .setOnClickListener {
                findNavController().navigate(
                    R.id.action_admin_to_authority
                )
            }

        binding.btnLogout
            .setOnClickListener {
                authViewModel.logout()

                findNavController().navigate(
                    R.id.action_admin_to_login
                )
            }

        verifyAdminAndObserve()
    }

    private fun verifyAdminAndObserve() {
        viewLifecycleOwner.lifecycleScope.launch {

            val user = runCatching {
                authViewModel.currentUser()
            }.getOrNull()

            val isApprovedAdmin =
                user?.userRole() ==
                        UserRole.ADMIN &&
                        user.approvalStatus ==
                        ApprovalStatus.APPROVED.name

            if (!isApprovedAdmin) {
                authViewModel.logout()

                findNavController().navigate(
                    R.id.action_admin_to_login
                )

                return@launch
            }

            binding.tvAdminIdentity.text =
                "${user.name} • ${user.email}"

            observeAdminData()
        }
    }

    private suspend fun observeAdminData() {

        viewLifecycleOwner.repeatOnLifecycle(
            Lifecycle.State.STARTED
        ) {
            launch {
                authViewModel
                    .pendingUsers
                    .collect { users ->
                        renderPendingUsers(users)
                    }
            }

            launch {
                authViewModel
                    .adminActionState
                    .collect { state ->

                        binding.progress.isVisible =
                            state is UiResult.Loading

                        when (state) {
                            is UiResult.Success -> {
                                Snackbar.make(
                                    binding.root,
                                    "Account status updated",
                                    Snackbar.LENGTH_SHORT
                                ).show()

                                authViewModel
                                    .clearAdminActionState()
                            }

                            is UiResult.Error -> {
                                Snackbar.make(
                                    binding.root,
                                    state.message,
                                    Snackbar.LENGTH_LONG
                                ).show()

                                authViewModel
                                    .clearAdminActionState()
                            }

                            else -> Unit
                        }
                    }
            }
        }
    }

    private fun renderPendingUsers(
        users: List<AppUser>
    ) {
        binding.pendingUsersContainer
            .removeAllViews()

        binding.tvEmpty.isVisible =
            users.isEmpty()

        users.forEach { user ->

            val card =
                MaterialCardView(
                    requireContext()
                ).apply {

                    layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout
                                .LayoutParams
                                .MATCH_PARENT,

                            LinearLayout
                                .LayoutParams
                                .WRAP_CONTENT
                        ).apply {
                            topMargin = 12.dp()
                        }

                    setContentPadding(
                        16.dp(),
                        14.dp(),
                        16.dp(),
                        14.dp()
                    )
                }

            val content =
                LinearLayout(
                    requireContext()
                ).apply {
                    orientation =
                        LinearLayout.VERTICAL
                }

            val title =
                TextView(
                    requireContext()
                ).apply {
                    text =
                        user.name.ifBlank {
                            "Unnamed account"
                        }

                    textSize = 18f

                    setTypeface(
                        typeface,
                        android.graphics
                            .Typeface.BOLD
                    )
                }

            val details =
                TextView(
                    requireContext()
                ).apply {
                    text =
                        buildString {
                            appendLine(user.email)
                            append(user.roleLabel())
                        }
                }

            val buttons =
                LinearLayout(
                    requireContext()
                ).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                }

            val approveButton =
                MaterialButton(
                    requireContext()
                ).apply {
                    text = "Approve"

                    layoutParams =
                        LinearLayout.LayoutParams(
                            0,
                            LinearLayout
                                .LayoutParams
                                .WRAP_CONTENT,
                            1f
                        )

                    setOnClickListener {
                        authViewModel
                            .updateApproval(
                                user.uid,
                                ApprovalStatus
                                    .APPROVED.name
                            )
                    }
                }

            val rejectButton =
                MaterialButton(
                    requireContext()
                ).apply {
                    text = "Reject"

                    layoutParams =
                        LinearLayout.LayoutParams(
                            0,
                            LinearLayout
                                .LayoutParams
                                .WRAP_CONTENT,
                            1f
                        ).apply {
                            marginStart = 8.dp()
                        }

                    setOnClickListener {
                        authViewModel
                            .updateApproval(
                                user.uid,
                                ApprovalStatus
                                    .REJECTED.name
                            )
                    }
                }

            buttons.addView(approveButton)
            buttons.addView(rejectButton)

            content.addView(title)
            content.addView(details)
            content.addView(buttons)

            card.addView(content)

            binding.pendingUsersContainer
                .addView(card)
        }
    }

    private fun Int.dp(): Int {
        return (
                this *
                        resources
                            .displayMetrics
                            .density
                ).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}