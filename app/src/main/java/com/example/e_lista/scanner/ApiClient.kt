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
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    // !!! CHANGE THIS TO YOUR PC IP ADDRESS !!!
    // Example: "http://192.168.1.112:5000/scan"
    private const val BACKEND_URL = "http://192.168.1.112:5000/scan"

    fun uploadReceiptFile(file: File, callback: (ReceiptAnalysis?) -> Unit) {
        try {
            val fileSize = file.length()
            Log.d("ApiClient", "Uploading file: ${file.name}, size: $fileSize bytes")

            if (fileSize == 0L) {
                Log.e("ApiClient", "File is empty.")
                callback(null)
                return
            }

            val mediaType = "image/jpeg".toMediaTypeOrNull()

            // 1. MATCH PYTHON: Use "file" as the key, not "image"
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",     // <--- Changed from "image" to "file" to match server.py
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
                    Log.e("ApiClient", "❌ Connection failed: ${e.message}", e)
                    callback(null)
                }

                override fun onResponse(call: Call, response: Response) {
                    val rawJsonResponse = response.body?.string()
                    try {
                        if (!response.isSuccessful) {
                            Log.e("ApiClient", "❌ Server error ${response.code}: $rawJsonResponse")
                            callback(null)
                            return
                        }

                        Log.d("ApiClient", "Raw JSON: $rawJsonResponse")
                        val root = JSONObject(rawJsonResponse ?: "{}")

                        // 2. CHECK SUCCESS FLAG
                        if (!root.optBoolean("success")) {
                            Log.e("ApiClient", "Backend reported failure: ${root.optString("error")}")
                            callback(null)
                            return
                        }

                        // 3. PARSE NEW PYTHON DATA STRUCTURE
                        val data = root.getJSONObject("data")

                        // Map Python keys to your ReceiptAnalysis variables
                        // Python: "store" -> Kotlin: vendor
                        val vendor = data.optString("store", "Unknown Store")

                        // Python: "date" -> Kotlin: date
                        val date = data.optString("date", "Unknown Date")

                        // Python: "total" -> Kotlin: total
                        val totalStr = data.optString("total", "0.00")

                        // Python: "id" -> Kotlin: receiptID
                        val receiptID = data.optString("id", "N/A")

                        // 4. PARSE ITEMS
                        val items = mutableListOf<ReceiptItem>()
                        val productsArray = data.optJSONArray("products")

                        if (productsArray != null) {
                            for (i in 0 until productsArray.length()) {
                                val itemObj = productsArray.getJSONObject(i)
                                // Python: "name" -> Kotlin: name
                                val name = itemObj.optString("name", "Item")
                                // Python: "price" -> Kotlin: price
                                val price = itemObj.optString("price", "0.00")

                                items.add(ReceiptItem(name, price))
                            }
                        }

                        // 5. CREATE EXISTING DATA CLASS (Database Safe)
                        val receiptAnalysis = ReceiptAnalysis(
                            vendor = vendor,
                            date = date,
                            total = totalStr,
                            items = items,
                            receiptID = receiptID
                        )

                        callback(receiptAnalysis)

                    } catch (e: Exception) {
                        Log.e("ApiClient", "❌ Parse Error: ${e.message}", e)
                        callback(null)
                    } finally {
                        response.body?.close()
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("ApiClient", "❌ Error: ${e.message}", e)
            callback(null)
        }
    }
}