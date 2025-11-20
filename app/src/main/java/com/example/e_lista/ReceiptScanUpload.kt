package com.example.e_lista

import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.e_lista.scanner.ApiClient
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReceiptScanUpload : AppCompatActivity() {
    private val PICK_IMAGE_REQUEST = 100
    private val REQUEST_IMAGE_CAPTURE = 101
    private var currentPhotoPath: String? = null
    private val CAMERA_PERMISSION_CODE = 99

    private val appSettingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // When returning from settings, check permission again
            if (checkCameraPermission()) {
                dispatchTakePictureIntent()
            } else {
                Toast.makeText(this, "Camera permission is still required", Toast.LENGTH_SHORT).show()
                finish() // go back to previous activity
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Camera capture button

        if (savedInstanceState == null) {
            if (checkCameraPermission()) {
                dispatchTakePictureIntent()
            } else {
                requestCameraPermission()
            }
        }

        //  Upload from gallery
        /*
        findViewById<Button>(R.id.btnUpload).setOnClickListener {
          val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
          startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }
        */
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Request camera permission
    private fun requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.CAMERA)) {
            // User denied before, show rationale dialog and ask again
            AlertDialog.Builder(this)
                .setTitle("Camera Permission Needed")
                .setMessage("This app needs access to your camera to scan receipts.")
                .setPositiveButton("Allow") { _, _ ->
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(android.Manifest.permission.CAMERA),
                        CAMERA_PERMISSION_CODE
                    )
                }
                .setNegativeButton("Cancel") { _, _ ->
                    finish() // go back to previous activity
                }
                .show()
        } else {
            // First-time request OR permanently denied
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_CODE) {

            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                dispatchTakePictureIntent()
            } else {

                val permanentlyDenied =
                    !ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        android.Manifest.permission.CAMERA
                    )

                if (permanentlyDenied) {

                    // Show the settings dialog FIRST
                    AlertDialog.Builder(this)
                        .setTitle("Camera Permission Required")
                        .setMessage("The camera permission is denied. Please enable it in Settings.")
                        .setPositiveButton("Open Settings") { _, _ ->
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", packageName, null)
                            )
                            startActivity(intent)

                            // Close this activity so user returns to previous one after settings
                            finish()
                        }
                        .setNegativeButton("Cancel") { _, _ ->
                            // Close and go back to previous activity
                            finish()
                        }
                        .setCancelable(false)
                        .show()

                } else {
                    Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()

                    // Not permanently denied—just go back
                    finish()
                }
            }
        }
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    val imageUri = data?.data ?: return
                    val file = writeToFile(this, imageUri)
                    if (file != null) {
                        uploadFile(file)
                    } else {
                        Toast.makeText(this, "Failed to prepare image file.", Toast.LENGTH_SHORT).show()
                    }
                }

                REQUEST_IMAGE_CAPTURE -> {
                    val photoPath = currentPhotoPath
                    if (photoPath != null) {
                        val file = File(photoPath)
                        if (file.exists()) {
                            uploadFile(file)
                        } else {
                            Toast.makeText(this, "Failed to find captured image.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }else if (resultCode == Activity.RESULT_CANCELED) {
            // User pressed back in camera or gallery, finish this activity
            finish()
        }
    }

    private fun uploadFile(file: File) {
        Toast.makeText(this, "Uploading receipt...", Toast.LENGTH_SHORT).show()

        ApiClient.uploadReceiptFile(file) { analysis ->
            runOnUiThread {
                if (analysis != null) {
                    Toast.makeText(this, "Receipt scanned successfully!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, ScannedReceipt::class.java)
                    intent.putExtra("analysis", analysis)
                    startActivity(intent)

                } else {
                    Toast.makeText(this, "Failed to analyze receipt.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun writeToFile(context: Context, uri: Uri): File? {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        if (inputStream == null) return null

        val fileName = File(uri.path ?: "receipt_upload").name
        val tempFile = File(context.cacheDir, fileName)

        try {
            inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            return tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    Toast.makeText(this, "Error creating image file.", Toast.LENGTH_SHORT).show()
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.example.e_lista.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }
}
