package com.example.e_lista.scanner

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.e_lista.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.e_lista.scanner.ReceiptAdapter
class MainActivity : AppCompatActivity() {

  private lateinit var recyclerView: RecyclerView
  private lateinit var adapter: ReceiptAdapter
  private lateinit var tvVendor: TextView
  private lateinit var tvDate: TextView
  private lateinit var tvTotal: TextView
  private lateinit var tvReceiptId: TextView

  // Request codes
  private val PICK_IMAGE_REQUEST = 100
  private val REQUEST_IMAGE_CAPTURE = 101

  private var currentPhotoPath: String? = null

  @SuppressLint("MissingInflatedId")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_confirm_11_1)
    setContentView(R.layout.activity_camera_11)

    // Initialize RecyclerView and Adapter
    recyclerView = findViewById(R.id.products)
    adapter = ReceiptAdapter(emptyList())
    recyclerView.layoutManager = LinearLayoutManager(this)
    recyclerView.adapter = adapter

    // Initialize TextViews
    tvVendor = findViewById(R.id.storeName)
    tvDate = findViewById(R.id.dateLabel)
    tvTotal = findViewById(R.id.totalLabel)
    tvReceiptId = findViewById(R.id.receipt_id)

    // Default values
    tvVendor.text = "Vendor: N/A"
    tvDate.text = "Date: N/A"
    tvTotal.text = "Total: ₱0.00"
    tvReceiptId.text = "Receipt ID: N/A"

    // Upload button logic
    /*findViewById<Button>(R.id.btnUpload).setOnClickListener {
      val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
      startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }*/

    // Camera capture button logic
    findViewById<Button>(R.id.fabCapture).setOnClickListener {
      dispatchTakePictureIntent()
    }
  }

  //pano to wtf (copy and paste sa google cloud instructions)
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
    // Show quick feedback
    Toast.makeText(this, "Uploading receipt...", Toast.LENGTH_SHORT).show()

    // Call ApiClient to analyze the receipt
    ApiClient.uploadReceiptFile(file) { analysis ->
      runOnUiThread {
        if (analysis != null) {
          updateUI(analysis)
          Toast.makeText(this, "Receipt scanned successfully!", Toast.LENGTH_SHORT).show()
        } else {
          Toast.makeText(this, "Failed to analyze receipt.", Toast.LENGTH_LONG).show()
        }
      }
    }
  }



  private fun updateUI(receipt: ReceiptAnalysis) {
    tvVendor.text = "Vendor: ${receipt.vendor}"
    tvDate.text = "Date: ${receipt.date}"
    tvReceiptId.text = "Receipt ID: ${receipt.receiptID}"

    // receipt.total is a string; try to format as numeric, fallback to raw
    tvTotal.text = try {
      "Total: ₱${String.format("%.2f", receipt.total.toDouble())}"
    } catch (e: Exception) {
      "Total: ₱${receipt.total}"
    }

    adapter.updateItems(receipt.items)
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

  //pano to :skull:
  @Throws(IOException::class)
  private fun createImageFile(): File {
    // Create an image file name
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile(
      "JPEG_${timeStamp}_", /* prefix */
      ".jpg", /* suffix */
      storageDir /* directory */
    ).apply {
      // Save a file: path for use with ACTION_VIEW intents
      currentPhotoPath = absolutePath
    }
  }

  private fun dispatchTakePictureIntent() {
    Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
      // Ensure that there's a camera activity to handle the intent
      takePictureIntent.resolveActivity(packageManager)?.also {
        // Create the File where the photo should go
        val photoFile: File? = try {
          createImageFile()
        } catch (ex: IOException) {
          // Error occurred while creating the File
          Toast.makeText(this, "Error creating image file.", Toast.LENGTH_SHORT).show()
          null
        }
        // Continue only if the File was successfully created
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