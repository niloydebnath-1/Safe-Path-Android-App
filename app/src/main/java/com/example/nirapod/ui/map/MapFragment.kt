package com.example.nirapod.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.nirapod.NirapodApplication
import com.example.nirapod.R
import com.example.nirapod.data.model.HazardReport
import com.example.nirapod.databinding.FragmentMapBinding
import com.example.nirapod.ui.common.NirapodViewModelFactory
import com.example.nirapod.ui.report.ReportViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import kotlin.math.abs

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null

    private val binding: FragmentMapBinding
        get() = _binding!!

    private val app by lazy {
        requireActivity().application as NirapodApplication
    }

    private val viewModel: ReportViewModel by viewModels {
        NirapodViewModelFactory(app)
    }

    private var map: MapLibreMap? = null

    private var mapStyleReady = false

    private var cameraMovedToReports = false

    private var latestReports: List<HazardReport> =
        emptyList()

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->

            val granted =
                result.values.any { it }

            if (granted) {
                moveToCurrentLocation()
            } else {
                _binding?.let {
                    Snackbar.make(
                        it.root,
                        "Location permission was not granted",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentMapBinding.inflate(
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
        super.onViewCreated(
            view,
            savedInstanceState
        )

        binding.tvMapStatus.text =
            "Loading OpenFreeMap and hazard reports…"

        binding.mapView.onCreate(
            savedInstanceState
        )

        binding.mapView
            .addOnDidFailLoadingMapListener { message ->

                val currentBinding =
                    _binding
                        ?: return@addOnDidFailLoadingMapListener

                currentBinding.tvMapStatus.text =
                    "Unable to load OpenFreeMap"

                Snackbar.make(
                    currentBinding.root,
                    "Map error: $message",
                    Snackbar.LENGTH_LONG
                ).show()
            }

        binding.mapView.getMapAsync { mapLibreMap ->

            if (_binding == null) {
                return@getMapAsync
            }

            map = mapLibreMap

            mapLibreMap.cameraPosition =
                CameraPosition.Builder()
                    .target(
                        LatLng(
                            DEFAULT_LATITUDE,
                            DEFAULT_LONGITUDE
                        )
                    )
                    .zoom(DEFAULT_ZOOM)
                    .build()

            mapLibreMap.setStyle(
                Style.Builder()
                    .fromUri(MAP_STYLE_URI)
            ) {
                mapStyleReady = true
                renderMarkers()
            }

            mapLibreMap
                .setOnInfoWindowClickListener { marker ->

                    val report =
                        findReportAtPosition(
                            marker.position
                        )

                    if (
                        report != null &&
                        findNavController()
                            .currentDestination
                            ?.id == R.id.mapFragment
                    ) {
                        findNavController().navigate(
                            R.id.action_map_to_detail,
                            bundleOf(
                                "reportId" to report.id
                            )
                        )

                        true
                    } else {
                        false
                    }
                }
        }

        binding.fabReport.setOnClickListener {

            if (
                findNavController()
                    .currentDestination
                    ?.id == R.id.mapFragment
            ) {
                findNavController().navigate(
                    R.id.action_map_to_report
                )
            }
        }

        binding.fabLocation.setOnClickListener {
            ensurePermissionAndMove()
        }

        observeReports()
    }

    private fun observeReports() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                viewModel.reports.collect { reports ->

                    latestReports = reports

                    renderMarkers()
                }
            }
        }
    }

    private fun renderMarkers() {

        val mapLibreMap =
            map ?: return

        val currentBinding =
            _binding ?: return

        if (!mapStyleReady) {

            currentBinding.tvMapStatus.text =
                "Loading OpenFreeMap… " +
                    "${latestReports.size} report(s) received"

            return
        }

        mapLibreMap.clear()

        val validReports =
            latestReports.filter { report ->
                hasValidCoordinates(report)
            }

        validReports.forEach { report ->

            val category =
                report.aiCategory
                    .ifBlank {
                        report.category
                    }
                    .ifBlank {
                        "Other Hazard"
                    }

            val severity =
                report.aiSeverity
                    .ifBlank {
                        report.severity
                    }
                    .ifBlank {
                        "MEDIUM"
                    }
                    .uppercase()

            val status =
                report.status.ifBlank {
                    "SUBMITTED"
                }

            mapLibreMap.addMarker(
                MarkerOptions()
                    .position(
                        LatLng(
                            report.latitude,
                            report.longitude
                        )
                    )
                    .title(
                        "$category • $severity"
                    )
                    .snippet(
                        buildString {
                            append(status)
                            append(" • ")
                            append(report.confirmations)
                            append(" confirmation(s)")
                            append("\nTap this box for details")
                        }
                    )
            )
        }

        updateMapStatus(
            validReports
        )

        if (
            !cameraMovedToReports &&
            validReports.isNotEmpty()
        ) {
            cameraMovedToReports = true

            val firstReport =
                validReports.first()

            mapLibreMap.animateCamera(
                CameraUpdateFactory
                    .newLatLngZoom(
                        LatLng(
                            firstReport.latitude,
                            firstReport.longitude
                        ),
                        REPORT_ZOOM
                    )
            )
        }
    }

    private fun updateMapStatus(
        validReports: List<HazardReport>
    ) {
        val currentBinding =
            _binding ?: return

        val invalidCount =
            latestReports.size -
                validReports.size

        currentBinding.tvMapStatus.text =
            when {

                latestReports.isEmpty() ->

                    "OpenFreeMap • No community reports yet"

                validReports.isEmpty() ->

                    "OpenFreeMap • Reports found, " +
                        "but no valid GPS coordinates"

                invalidCount > 0 ->

                    "OpenFreeMap • " +
                        "${validReports.size} live marker(s) • " +
                        "$invalidCount without valid GPS"

                else ->

                    "OpenFreeMap • " +
                        "${validReports.size} live hazard marker(s) • " +
                        "tap a marker for details"
            }
    }

    private fun findReportAtPosition(
        position: LatLng
    ): HazardReport? {

        return latestReports.firstOrNull { report ->

            abs(
                report.latitude -
                    position.latitude
            ) < COORDINATE_TOLERANCE &&
                abs(
                    report.longitude -
                        position.longitude
                ) < COORDINATE_TOLERANCE
        }
    }

    private fun hasValidCoordinates(
        report: HazardReport
    ): Boolean {

        val validLatitude =
            report.latitude in -90.0..90.0

        val validLongitude =
            report.longitude in -180.0..180.0

        val notZero =
            report.latitude != 0.0 ||
                report.longitude != 0.0

        return validLatitude &&
            validLongitude &&
            notZero
    }

    private fun ensurePermissionAndMove() {

        val finePermission =
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )

        val coarsePermission =
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

        val granted =
            finePermission ==
                PackageManager.PERMISSION_GRANTED ||
                coarsePermission ==
                PackageManager.PERMISSION_GRANTED

        if (granted) {
            moveToCurrentLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun moveToCurrentLocation() {

        viewLifecycleOwner.lifecycleScope.launch {

            val currentBinding =
                _binding
                    ?: return@launch

            val location =
                runCatching {
                    app.container
                        .locationClient
                        .getCurrentLocation()
                }.getOrNull()

            if (location == null) {

                Snackbar.make(
                    currentBinding.root,
                    "Current location unavailable. " +
                        "Turn on GPS and try again.",
                    Snackbar.LENGTH_LONG
                ).show()

                return@launch
            }

            map?.animateCamera(
                CameraUpdateFactory
                    .newLatLngZoom(
                        LatLng(
                            location.latitude,
                            location.longitude
                        ),
                        CURRENT_LOCATION_ZOOM
                    )
            )

            showNearbyDangerWarning(
                location
            )
        }
    }

    private fun showNearbyDangerWarning(
        currentLocation: Location
    ) {
        val currentBinding =
            _binding ?: return

        val nearbyDanger =
            latestReports
                .filter {
                    hasValidCoordinates(it)
                }
                .mapNotNull { report ->

                    val severity =
                        report.aiSeverity
                            .ifBlank {
                                report.severity
                            }
                            .uppercase()

                    val dangerous =
                        severity == "HIGH" ||
                            severity == "CRITICAL"

                    if (!dangerous) {
                        return@mapNotNull null
                    }

                    val distanceResult =
                        FloatArray(1)

                    Location.distanceBetween(
                        currentLocation.latitude,
                        currentLocation.longitude,
                        report.latitude,
                        report.longitude,
                        distanceResult
                    )

                    if (
                        distanceResult[0] <=
                        NEARBY_DISTANCE_METRES
                    ) {
                        report to distanceResult[0]
                    } else {
                        null
                    }
                }
                .minByOrNull {
                    it.second
                }

        if (nearbyDanger != null) {

            val report =
                nearbyDanger.first

            val distance =
                nearbyDanger.second

            val category =
                report.aiCategory
                    .ifBlank {
                        report.category
                    }
                    .ifBlank {
                        "Hazard"
                    }

            Snackbar.make(
                currentBinding.root,
                "Caution: $category reported " +
                    "${distance.toInt()} metres away",
                Snackbar.LENGTH_LONG
            ).show()

        } else {

            Snackbar.make(
                currentBinding.root,
                "Current location found. " +
                    "No high-risk hazard within 300 metres.",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    override fun onStart() {
        super.onStart()
        _binding?.mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        _binding?.mapView?.onResume()
    }

    override fun onPause() {
        _binding?.mapView?.onPause()
        super.onPause()
    }

    override fun onStop() {
        _binding?.mapView?.onStop()
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        _binding?.mapView?.onLowMemory()
    }

    override fun onSaveInstanceState(
        outState: Bundle
    ) {
        super.onSaveInstanceState(
            outState
        )

        _binding
            ?.mapView
            ?.onSaveInstanceState(
                outState
            )
    }

    override fun onDestroyView() {

        map?.clear()

        map = null
        mapStyleReady = false
        cameraMovedToReports = false

        binding.mapView.onDestroy()

        _binding = null

        super.onDestroyView()
    }

    companion object {

        private const val MAP_STYLE_URI =
            "https://tiles.openfreemap.org/styles/liberty"

        private const val DEFAULT_LATITUDE =
            23.8103

        private const val DEFAULT_LONGITUDE =
            90.4125

        private const val DEFAULT_ZOOM =
            11.5

        private const val REPORT_ZOOM =
            13.5

        private const val CURRENT_LOCATION_ZOOM =
            15.0

        private const val COORDINATE_TOLERANCE =
            0.00001

        private const val NEARBY_DISTANCE_METRES =
            300f
    }
}
