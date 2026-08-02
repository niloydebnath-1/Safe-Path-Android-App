package com.example.nirapod.ui.report

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.nirapod.NirapodApplication
import com.example.nirapod.core.UiResult
import com.example.nirapod.data.model.AiAnalysis
import com.example.nirapod.data.model.AppUser
import com.example.nirapod.data.model.HazardReport
import com.example.nirapod.data.model.ReportStatus
import com.example.nirapod.databinding.FragmentReportBinding
import com.example.nirapod.ui.auth.AuthViewModel
import com.example.nirapod.ui.camera.CameraActivity
import com.example.nirapod.ui.common.NirapodViewModelFactory
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ReportFragment : Fragment() {
    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!
    private val app by lazy { requireActivity().application as NirapodApplication }
    private val reportViewModel: ReportViewModel by viewModels { NirapodViewModelFactory(app) }
    private val authViewModel: AuthViewModel by viewModels { NirapodViewModelFactory(app) }

    private var user: AppUser? = null
    private var imageUri: Uri? = null
    private var latitude: Double? = null
    private var longitude: Double? = null
    private var aiAnalysis: AiAnalysis? = null

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val value = result.data?.getStringExtra(CameraActivity.EXTRA_IMAGE_URI)
            imageUri = value?.let(Uri::parse)
            imageUri?.let { Glide.with(this).load(it).into(binding.ivPhoto) }
            aiAnalysis = null
            binding.aiCard.isVisible = false
        } else {
            result.data?.getStringExtra(CameraActivity.EXTRA_ERROR)?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) captureLocation()
        else Snackbar.make(binding.root, "Location permission is required for a geotagged report", Snackbar.LENGTH_LONG).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val categories = listOf(
            "Open Manhole", "Damaged Road", "Broken Drain", "Waterlogging",
            "Electrical Hazard", "Crime Hotspot", "Fire Hazard", "Other Hazard"
        )
        binding.categoryDropdown.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, categories))
        binding.categoryDropdown.setText("Other Hazard", false)

        binding.btnCamera.setOnClickListener {
            cameraLauncher.launch(Intent(requireContext(), CameraActivity::class.java))
        }
        binding.btnLocation.setOnClickListener { ensureLocationPermission() }
        binding.btnAnalyze.setOnClickListener {
            val description = binding.etDescription.text?.toString().orEmpty()
            if (imageUri == null && description.isBlank()) {
                Snackbar.make(binding.root, "Take a photo or write a description first", Snackbar.LENGTH_LONG).show()
            } else {
                reportViewModel.analyze(imageUri, description)
            }
        }
        binding.btnSubmit.setOnClickListener { submitReport() }

        viewLifecycleOwner.lifecycleScope.launch { user = authViewModel.currentUser() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    reportViewModel.aiState.collect { state ->
                        val loading = state is UiResult.Loading
                        binding.progress.isVisible = loading || reportViewModel.submitState.value is UiResult.Loading
                        binding.btnAnalyze.isEnabled = !loading
                        when (state) {
                            is UiResult.Success -> {
                                aiAnalysis = state.data
                                binding.aiCard.isVisible = true
                                binding.tvAiResult.text = buildString {
                                    appendLine("Category: ${state.data.category}")
                                    appendLine("Severity: ${state.data.severity}")
                                    appendLine("Authority: ${state.data.suggestedAuthority}")
                                    appendLine("Risk: ${state.data.risk}")
                                    append("Summary: ${state.data.summary}")
                                }
                                binding.categoryDropdown.setText(state.data.category, false)
                            }
                            is UiResult.Error -> {
                                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                                reportViewModel.clearAiState()
                            }
                            else -> Unit
                        }
                    }
                }
                launch {
                    reportViewModel.submitState.collect { state ->
                        val loading = state is UiResult.Loading
                        binding.progress.isVisible = loading || reportViewModel.aiState.value is UiResult.Loading
                        binding.btnSubmit.isEnabled = !loading
                        when (state) {
                            is UiResult.Success -> {
                                Snackbar.make(binding.root, "Report submitted successfully", Snackbar.LENGTH_LONG).show()
                                reportViewModel.clearSubmitState()
                                findNavController().navigateUp()
                            }
                            is UiResult.Error -> {
                                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                                reportViewModel.clearSubmitState()
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun ensureLocationPermission() {
        val fine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            captureLocation()
        } else {
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    private fun captureLocation() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.btnLocation.isEnabled = false
            val location = runCatching { app.container.locationClient.getCurrentLocation() }.getOrNull()
            binding.btnLocation.isEnabled = true
            if (location == null) {
                Snackbar.make(binding.root, "Could not capture current location", Snackbar.LENGTH_LONG).show()
                return@launch
            }
            latitude = location.latitude
            longitude = location.longitude
            binding.tvLocation.text = "Location: %.6f, %.6f".format(location.latitude, location.longitude)
        }
    }

    private fun submitReport() {
        val currentUser = user
        val lat = latitude
        val lon = longitude
        val description = binding.etDescription.text?.toString().orEmpty().trim()
        val analysis = aiAnalysis
        when {
            currentUser == null -> Snackbar.make(binding.root, "User profile is still loading", Snackbar.LENGTH_LONG).show()
            imageUri == null -> Snackbar.make(binding.root, "Take a hazard photo first", Snackbar.LENGTH_LONG).show()
            lat == null || lon == null -> Snackbar.make(binding.root, "Capture the current location first", Snackbar.LENGTH_LONG).show()
            description.isBlank() -> Snackbar.make(binding.root, "Write a short description", Snackbar.LENGTH_LONG).show()
            analysis == null -> Snackbar.make(binding.root, "Run the mandatory AI analysis before submitting", Snackbar.LENGTH_LONG).show()
            else -> {
                reportViewModel.submit(
                    HazardReport(
                        reporterId = currentUser.uid,
                        reporterName = currentUser.name,
                        category = binding.categoryDropdown.text.toString().ifBlank { analysis.category },
                        description = description,
                        latitude = lat,
                        longitude = lon,
                        status = ReportStatus.SUBMITTED.name,
                        severity = analysis.severity,
                        aiCategory = analysis.category,
                        aiSeverity = analysis.severity,
                        aiSummary = analysis.summary,
                        assignedAuthority = analysis.suggestedAuthority
                    ),
                    imageUri
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
