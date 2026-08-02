package com.example.nirapod.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import com.example.nirapod.data.model.SosAlert
import com.example.nirapod.databinding.FragmentHomeBinding
import com.example.nirapod.ui.auth.AuthViewModel
import com.example.nirapod.ui.authority.SosViewModel
import com.example.nirapod.ui.common.NirapodViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val app by lazy { requireActivity().application as NirapodApplication }
    private val authViewModel: AuthViewModel by viewModels { NirapodViewModelFactory(app) }
    private val sosViewModel: SosViewModel by viewModels { NirapodViewModelFactory(app) }
    private var currentUser: AppUser? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) sendSosNow()
        else Snackbar.make(binding.root, "Location permission is needed for the SOS demo", Snackbar.LENGTH_LONG).show()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnMap.setOnClickListener { findNavController().navigate(R.id.action_home_to_map) }
        binding.btnReport.setOnClickListener { findNavController().navigate(R.id.action_home_to_report) }
        binding.btnMyReports.setOnClickListener { findNavController().navigate(R.id.action_home_to_my_reports) }
        binding.btnAuthority.setOnClickListener { findNavController().navigate(R.id.action_home_to_authority) }
        binding.btnLogout.setOnClickListener {
            authViewModel.logout()
            findNavController().navigate(R.id.action_home_to_login)
        }
        binding.btnSos.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("SOS prototype")
                .setMessage("This is not connected to real emergency services. Long-press the button to send a demo alert to the in-app authority dashboard.")
                .setPositiveButton("OK", null)
                .show()
        }
        binding.btnSos.setOnLongClickListener {
            ensureLocationThenSos()
            true
        }

        viewLifecycleOwner.lifecycleScope.launch {
            currentUser = authViewModel.currentUser()
            val user = currentUser
            if (user == null) {
                findNavController().navigate(R.id.action_home_to_login)
                return@launch
            }
            binding.tvWelcome.text = "Welcome, ${user.name.ifBlank { "User" }}"
            binding.tvRole.text = "${user.roleLabel()} • ${AppConfig.cloudModeLabel}"
            binding.btnAuthority.isVisible = user.canAccessAuthorityDashboard()
            registerFcmTokenIfAvailable()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sosViewModel.state.collect { state ->
                    when (state) {
                        is UiResult.Loading -> binding.btnSos.isEnabled = false
                        is UiResult.Success -> {
                            binding.btnSos.isEnabled = true
                            Snackbar.make(binding.root, "Prototype SOS received by the in-app control room", Snackbar.LENGTH_LONG).show()
                            sosViewModel.clearState()
                        }
                        is UiResult.Error -> {
                            binding.btnSos.isEnabled = true
                            Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                            sosViewModel.clearState()
                        }
                        else -> binding.btnSos.isEnabled = true
                    }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun ensureLocationThenSos() {
        val fine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            sendSosNow()
        } else {
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    private fun sendSosNow() {
        val user = currentUser ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val location = runCatching { app.container.locationClient.getCurrentLocation() }.getOrNull()
            if (location == null) {
                Snackbar.make(binding.root, "Could not obtain current location", Snackbar.LENGTH_LONG).show()
                return@launch
            }
            sosViewModel.send(
                SosAlert(
                    userId = user.uid,
                    userName = user.name,
                    latitude = location.latitude,
                    longitude = location.longitude
                )
            )
        }
    }

    private fun registerFcmTokenIfAvailable() {
        if (!AppConfig.firebaseConfigured) return
        viewLifecycleOwner.lifecycleScope.launch {
            val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull() ?: return@launch
            runCatching { app.container.authRepository.saveFcmToken(token) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
