package com.example.nirapod.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.nirapod.NirapodApplication
import com.example.nirapod.R
import com.example.nirapod.core.AppConfig
import com.example.nirapod.core.UiResult
import com.example.nirapod.data.model.AppUser
import com.example.nirapod.data.model.ApprovalStatus
import com.example.nirapod.data.model.UserRole
import com.example.nirapod.databinding.FragmentLoginBinding
import com.example.nirapod.ui.common.NirapodViewModelFactory
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding:
            FragmentLoginBinding? = null

    private val binding
        get() = _binding!!

    private val viewModel:
            AuthViewModel by viewModels {
        NirapodViewModelFactory(
            requireActivity().application
                    as NirapodApplication
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentLoginBinding.inflate(
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
        binding.tvMode.text =
            AppConfig.cloudModeLabel

        binding.demoButtons.isVisible =
            !AppConfig.firebaseConfigured

        binding.tvDemoLabel.isVisible =
            !AppConfig.firebaseConfigured

        binding.btnLogin.setOnClickListener {
            viewModel.login(
                binding.etEmail.text
                    ?.toString()
                    .orEmpty(),
                binding.etPassword.text
                    ?.toString()
                    .orEmpty()
            )
        }

        binding.btnRegister.setOnClickListener {
            findNavController().navigate(
                R.id.action_login_to_register
            )
        }

        binding.btnDemoCitizen
            .setOnClickListener {
                viewModel.demoLogin(
                    UserRole.CITIZEN
                )
            }

        binding.btnDemoAuthority
            .setOnClickListener {
                viewModel.demoLogin(
                    UserRole.AUTHORITY
                )
            }

        checkExistingSession()
        observeLoginState()
    }

    private fun checkExistingSession() {
        viewLifecycleOwner.lifecycleScope.launch {

            val existingUser =
                runCatching {
                    viewModel.currentUser()
                }.getOrNull()

            if (
                existingUser != null &&
                findNavController()
                    .currentDestination
                    ?.id == R.id.loginFragment
            ) {
                openCorrectPanel(existingUser)
            }
        }
    }

    private fun observeLoginState() {
        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                viewModel.state.collect { state ->

                    binding.progress.isVisible =
                        state is UiResult.Loading

                    binding.btnLogin.isEnabled =
                        state !is UiResult.Loading

                    when (state) {
                        is UiResult.Success -> {
                            openCorrectPanel(
                                state.data
                            )
                            viewModel.clearState()
                        }

                        is UiResult.Error -> {
                            Snackbar.make(
                                binding.root,
                                state.message,
                                Snackbar.LENGTH_LONG
                            ).show()

                            viewModel.clearState()
                        }

                        else -> Unit
                    }
                }
            }
        }
    }

    private fun openCorrectPanel(
        user: AppUser
    ) {
        if (
            findNavController()
                .currentDestination
                ?.id != R.id.loginFragment
        ) {
            return
        }

        val role = user.userRole()

        if (
            role != UserRole.CITIZEN &&
            user.approvalStatus !=
            ApprovalStatus.APPROVED.name
        ) {
            viewModel.logout()

            val message =
                when (
                    user.approvalStatus
                ) {
                    ApprovalStatus.REJECTED.name ->
                        "Your account was rejected by an administrator."

                    else ->
                        "Your account is pending administrator approval."
                }

            Snackbar.make(
                binding.root,
                message,
                Snackbar.LENGTH_LONG
            ).show()

            return
        }

        val actionId =
            when (role) {
                UserRole.CITIZEN ->
                    R.id.action_login_to_home

                UserRole.AUTHORITY ->
                    R.id.action_login_to_authority

                UserRole.ADMIN ->
                    R.id.action_login_to_admin
            }

        findNavController().navigate(actionId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}