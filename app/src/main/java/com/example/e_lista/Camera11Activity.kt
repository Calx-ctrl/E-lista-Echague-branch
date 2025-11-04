package com.example.e_lista

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.e_lista.databinding.ActivityCamera11Binding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class Camera11Activity : AppCompatActivity() {

    private lateinit var binding: ActivityCamera11Binding
    private var imageCapture: ImageCapture? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) startCamera()
            else Toast.makeText(
                this,
                "Camera permission denied. Please enable it in Settings.",
                Toast.LENGTH_LONG
            ).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCamera11Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        // Capture button
        binding.fabCapture.setOnClickListener {
            takePhotoWithLoading()
        }

        // Back button
        binding.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                Log.d("Camera11Activity", "✅ Camera started")
            } catch (exc: Exception) {
                Log.e("Camera11Activity", "❌ Camera start failed: ${exc.message}")
                Toast.makeText(this, "Camera start failed.", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhotoWithLoading() {
        val imageCapture = imageCapture ?: return

        // Create photo file
        val photoFile = File(
            externalMediaDirs.firstOrNull(),
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault()).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Show loading dialog
        val loadingDialog = android.app.Dialog(this)
        val loadingView = layoutInflater.inflate(R.layout.dialog_loading, null)
        loadingDialog.setContentView(loadingView)
        loadingDialog.setCancelable(false)
        loadingDialog.show()

        // Handler for 30-second timeout
        val handler = Handler(Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (loadingDialog.isShowing) {
                loadingDialog.dismiss()
                Toast.makeText(this, "Processing took too long.", Toast.LENGTH_SHORT).show()
            }
        }
        handler.postDelayed(timeoutRunnable, 30_000L)

        // Capture photo
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("Camera11Activity", "❌ Capture failed: ${exc.message}")
                    if (loadingDialog.isShowing) loadingDialog.dismiss()
                    handler.removeCallbacks(timeoutRunnable)
                    Toast.makeText(this@Camera11Activity, "Capture failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d("Camera11Activity", "✅ Photo saved: ${photoFile.absolutePath}")
                    if (loadingDialog.isShowing) loadingDialog.dismiss()
                    handler.removeCallbacks(timeoutRunnable)
                    Toast.makeText(this@Camera11Activity, "✅ Photo saved: ${photoFile.absolutePath}", Toast.LENGTH_SHORT).show()

                    // Optional: simulate 30-second scan for receipt
                    simulateReceiptScan()
                }
            }
        )
    }

    private fun simulateReceiptScan() {
        val scanningDialog = android.app.Dialog(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_scanning, null)
        scanningDialog.setContentView(dialogView)
        scanningDialog.setCancelable(false)
        scanningDialog.show()

        val handler = Handler(Looper.getMainLooper())

        // Finish scanning after 30 seconds
        handler.postDelayed({
            if (scanningDialog.isShowing) {
                scanningDialog.dismiss()
                android.app.AlertDialog.Builder(this)
                    .setTitle("Scan Result")
                    .setMessage("Receipt scan complete!")
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        }, 30_000L)
    }
}
