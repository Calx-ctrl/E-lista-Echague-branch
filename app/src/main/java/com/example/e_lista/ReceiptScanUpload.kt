package com.example.e_lista

import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_11)

        // Camera capture button
        findViewById<FloatingActionButton>(R.id.fabCapture).setOnClickListener {
            dispatchTakePictureIntent()
        }

        //  Upload from gallery
        /*
        findViewById<Button>(R.id.btnUpload).setOnClickListener {
          val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
          startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }
        */
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
