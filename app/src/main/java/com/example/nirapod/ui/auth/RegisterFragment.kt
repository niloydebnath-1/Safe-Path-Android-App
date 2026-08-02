package com.example.nirapod.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
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
import com.example.nirapod.data.model.AuthorityType
import com.example.nirapod.data.model.UserRole
import com.example.nirapod.databinding.FragmentRegisterBinding
import com.example.nirapod.ui.common.NirapodViewModelFactory
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private var _binding:
            FragmentRegisterBinding? = null

    private val binding
        get() = _binding!!

    private val viewModel:
            AuthViewModel by viewModels {
        NirapodViewModelFactory(
            requireActivity().application
                    as NirapodApplication
        )
    }

    private val roleOptions =
        listOf(
            "Citizen" to UserRole.CITIZEN,
            "Authority" to UserRole.AUTHORITY,
            "Admin" to UserRole.ADMIN
        )

    private val authorityOptions =
        listOf(
            "City Corporation" to
                    AuthorityType.CITY_CORPORATION,

            "Disaster Management Board" to
                    AuthorityType
                        .DISASTER_MANAGEMENT_BOARD,

            "Police" to
                    AuthorityType.POLICE
        )

    private var selectedRole =
        UserRole.CITIZEN

    private var selectedAuthorityType =
        AuthorityType.NONE

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentRegisterBinding.inflate(
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
        setupRoleDropdown()
        setupAuthorityDropdown()

        binding.btnRegister.setOnClickListener {

            if (
                selectedRole ==
                UserRole.AUTHORITY &&
                selectedAuthorityType ==
                AuthorityType.NONE
            ) {
                Snackbar.make(
                    binding.root,
                    "Please select an authority type",
                    Snackbar.LENGTH_LONG
                ).show()

                return@setOnClickListener
            }

            viewModel.register(
                name = binding.etName.text
                    ?.toString()
                    .orEmpty(),

                email = binding.etEmail.text
                    ?.toString()
                    .orEmpty(),

                password =
                    binding.etPassword.text
                        ?.toString()
                        .orEmpty(),

                role = selectedRole.name,

                authorityType =
                    selectedAuthorityType.name
            )
        }

        observeRegistrationState()
    }

    private fun observeRegistrationState() {
        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                viewModel.state.collect { state ->

                    binding.progress.isVisible =
                        state is UiResult.Loading

                    binding.btnRegister.isEnabled =
                        state !is UiResult.Loading

                    when (state) {
                        is UiResult.Success -> {
                            val user = state.data

                            if (
                                user.userRole() ==
                                UserRole.CITIZEN
                            ) {
                                findNavController()
                                    .navigate(
                                        R.id
                                            .action_register_to_home
                                    )
                            } else {
                                viewModel.logout()

                                Toast.makeText(
                                    requireContext(),
                                    "Account created. " +
                                            "Wait for administrator approval.",
                                    Toast.LENGTH_LONG
                                ).show()

                                val returned =
                                    findNavController()
                                        .popBackStack(
                                            R.id.loginFragment,
                                            false
                                        )

                                if (!returned) {
                                    findNavController()
                                        .navigate(
                                            R.id.loginFragment
                                        )
                                }
                            }

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

    private fun setupRoleDropdown() {

        val labels =
            roleOptions.map {
                it.first
            }

        binding.roleDropdown.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout
                    .simple_dropdown_item_1line,
                labels
            )
        )

        binding.roleDropdown.setText(
            "Citizen",
            false
        )

        binding.roleDropdown
            .setOnItemClickListener {
                    _,
                    _,
                    position,
                    _ ->

                selectedRole =
                    roleOptions[position].second

                val showAuthorityType =
                    selectedRole ==
                            UserRole.AUTHORITY

                binding
                    .authorityTypeLayout
                    .isVisible =
                    showAuthorityType

                if (!showAuthorityType) {
                    selectedAuthorityType =
                        AuthorityType.NONE

                    binding
                        .authorityTypeDropdown
                        .setText(
                            "",
                            false
                        )
                }
            }
    }

    private fun setupAuthorityDropdown() {

        val labels =
            authorityOptions.map {
                it.first
            }

        binding
            .authorityTypeDropdown
            .setAdapter(
                ArrayAdapter(
                    requireContext(),
                    android.R.layout
                        .simple_dropdown_item_1line,
                    labels
                )
            )

        binding
            .authorityTypeDropdown
            .setOnItemClickListener {
                    _,
                    _,
                    position,
                    _ ->

                selectedAuthorityType =
                    authorityOptions[position].second
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}