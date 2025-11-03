package com.example.e_lista.scanner

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
object ApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val BACKEND_URL = "https://elista-406642774905.asia-southeast1.run.app/analyze"

    fun uploadReceiptFile(file: File, callback: (ReceiptAnalysis?) -> Unit) {
        try {
            val fileSize = file.length()
            Log.d("ApiClient", "Uploading file: ${file.name}, size: $fileSize bytes")

            if (fileSize == 0L) {
                Log.e("ApiClient", "File is empty or not saved properly.")
                callback(null)
                return
            }

            val mediaType = "image/jpeg".toMediaTypeOrNull()
            if (mediaType == null) {
                Log.e("ApiClient", "Invalid MIME type specified.")
                callback(null)
                return
            }

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "image",
                    file.name,
                    file.asRequestBody(mediaType)
                )
                .build()

            val request = Request.Builder()
                .url(BACKEND_URL)
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("ApiClient", "❌ Upload failed: ${e.message}", e)
                    callback(null)
                }

                override fun onResponse(call: Call, response: Response) {
                    val rawJsonResponse = response.body?.string()
                    try {
                        if (!response.isSuccessful) {
                            Log.e("ApiClient", "❌ Server error: ${response.code}. Message: ${rawJsonResponse}")
                            callback(null)
                            return
                        }

                        if (rawJsonResponse.isNullOrEmpty()) {
                            Log.e("ApiClient", "Empty response from backend")
                            callback(null)
                            return
                        }

                        Log.d("ApiClient", "Raw JSON response: $rawJsonResponse")

                        val root = JSONObject(rawJsonResponse)

                        val extractedDataJson: JSONObject? = root.optJSONObject("extracted_data")
                        if (extractedDataJson == null) {
                            Log.e("ApiClient", "❌ Backend response missing 'extracted_data' object. Full response: $rawJsonResponse")
                            callback(null)
                            return
                        }

                        val vendor = extractedDataJson.optString("store_name", "Unknown Store")
                        val date = extractedDataJson.optString("date_of_purchase", "Unknown Date")
                        val totalStr = extractedDataJson.optString("total_amount", "0.00")
                        val receiptID = extractedDataJson.optString("receipt_id", "Unknown ID")


                        val productNamesArray: JSONArray? = extractedDataJson.optJSONArray("product_names")
                        val productPricesArray: JSONArray? = extractedDataJson.optJSONArray("product_prices")

                        val items = mutableListOf<ReceiptItem>()

                        val namesList = mutableListOf<String>()
                        productNamesArray?.let {
                            for (i in 0 until it.length()) {
                                namesList.add(it.getString(i))
                            }
                        }

                        val pricesList = mutableListOf<String>()
                        productPricesArray?.let {
                            for (i in 0 until it.length()) {
                                pricesList.add(it.getString(i))
                            }
                        }

                        // Align names and prices based on index
                        val maxLength = maxOf(namesList.size, pricesList.size)
                        for (i in 0 until maxLength) {
                            val name = namesList.getOrElse(i) { "Unknown Item" }
                            val price = pricesList.getOrElse(i) { "0.00" }
                            items.add(ReceiptItem(name, price))
                        }

                        val receiptAnalysis = ReceiptAnalysis(
                            vendor = vendor,
                            date = date,
                            total = totalStr,
                            items = items,
                            receiptID = receiptID,
                        )

                        callback(receiptAnalysis)

                    } catch (e: Exception) {
                        Log.e("ApiClient", "❌ JSON parse error: ${e.message}. Raw JSON: ${rawJsonResponse}", e)
                        callback(null)
                    } finally {
                        response.body?.close()
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("ApiClient", "❌ Unexpected error: ${e.message}", e)
            callback(null)
        }
    }
}
