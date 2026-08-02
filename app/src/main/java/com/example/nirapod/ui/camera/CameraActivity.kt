package com.example.nirapod.ui.camera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.nirapod.databinding.ActivityCameraBinding
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    private lateinit var cameraExecutor: ExecutorService

    private var imageCapture: ImageCapture? = null

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                startCamera()
            } else {
                finishWithError("Camera permission denied")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.btnCapture.setOnClickListener {
            capturePhoto()
        }

        val permissionGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

        if (permissionGranted) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(
            {
                try {
                    val cameraProvider =
                        cameraProviderFuture.get()

                    val preview =
                        Preview.Builder()
                            .build()
                            .also { cameraPreview ->

                                cameraPreview.setSurfaceProvider(
                                    binding.previewView.surfaceProvider
                                )
                            }

                    // Local non-null ImageCapture instance
                    val captureUseCase =
                        ImageCapture.Builder()
                            .setCaptureMode(
                                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                            )
                            .build()

                    imageCapture = captureUseCase

                    cameraProvider.unbindAll()

                    cameraProvider.bindToLifecycle(
                        this@CameraActivity,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        captureUseCase
                    )

                } catch (exception: Exception) {

                    finishWithError(
                        exception.message
                            ?: "Camera could not start"
                    )
                }
            },
            ContextCompat.getMainExecutor(this)
        )
    }

    private fun capturePhoto() {

        val captureUseCase = imageCapture

        if (captureUseCase == null) {
            Snackbar.make(
                binding.root,
                "Camera is not ready yet",
                Snackbar.LENGTH_SHORT
            ).show()

            return
        }

        binding.progress.isVisible = true
        binding.btnCapture.isEnabled = false

        val photoFile = File(
            cacheDir,
            "hazard-${System.currentTimeMillis()}.jpg"
        )

        val outputOptions =
            ImageCapture.OutputFileOptions
                .Builder(photoFile)
                .build()

        captureUseCase.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(
                    outputFileResults:
                    ImageCapture.OutputFileResults
                ) {
                    runOnUiThread {

                        val imageUri =
                            Uri.fromFile(photoFile).toString()

                        val resultIntent =
                            Intent().putExtra(
                                EXTRA_IMAGE_URI,
                                imageUri
                            )

                        setResult(
                            Activity.RESULT_OK,
                            resultIntent
                        )

                        finish()
                    }
                }

                override fun onError(
                    exception: ImageCaptureException
                ) {
                    runOnUiThread {

                        binding.progress.isVisible = false
                        binding.btnCapture.isEnabled = true

                        Snackbar.make(
                            binding.root,
                            exception.message
                                ?: "Photo capture failed",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }

    private fun finishWithError(message: String) {

        val resultIntent =
            Intent().putExtra(
                EXTRA_ERROR,
                message
            )

        setResult(
            Activity.RESULT_CANCELED,
            resultIntent
        )

        finish()
    }

    override fun onDestroy() {

        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }

        super.onDestroy()
    }

    companion object {
        const val EXTRA_IMAGE_URI = "imageUri"
        const val EXTRA_ERROR = "cameraError"
    }
}