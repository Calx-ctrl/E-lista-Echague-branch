package com.example.e_lista

import android.content.Context
import android.util.Log
import android.widget.Toast
import okio.IOException
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.text.SimpleDateFormat
import java.util.*

class VoiceCommandManager(
    private val context: Context,
    private val onStatusChange: (String) -> Unit,
    private val onExpenseParsed: (Expense) -> Unit
) {

    private var speechService: SpeechService? = null
    private var model: Model? = null
    private var isModelLoaded = false

    // We restrict words to this list for 99% accuracy
    private val grammar = "[" +
            "\"add\", \"remove\", \"delete\", " +
            "\"one\", \"two\", \"three\", \"four\", \"five\", \"six\", \"seven\", \"eight\", \"nine\", \"zero\", " +
            "\"ten\", \"eleven\", \"twelve\", \"thirteen\", \"fourteen\", \"fifteen\", " +
            "\"twenty\", \"thirty\", \"forty\", \"fifty\", \"sixty\", \"seventy\", \"eighty\", \"ninety\", " +
            "\"hundred\", \"thousand\", \"million\", " +
            "\"food\", \"transport\", \"bills\", \"shopping\", \"entertainment\", \"others\", " +
            "\"pesos\", \"in\", \"for\", \"on\", \"to\"" +
            "]"

    fun init() {
        if (!isModelLoaded) {
            onStatusChange("Loading...")

            // OLD WAY (Deleting this): StorageService.unpack(...) ❌

            // NEW WAY (Robust): Direct Asset Loading ✅
            val modelPath = java.io.File(context.filesDir, "model")

            if (!modelPath.exists()) {
                // If the model isn't in storage yet, copy it manually
                try {
                    val assetManager = context.assets
                    copyAssets(assetManager, "model", modelPath.absolutePath)
                } catch (e: IOException) {
                    onStatusChange("Copy Error: ${e.message}")
                    return
                }
            }

            // Now load from the path we just verified
            try {
                model = Model(modelPath.absolutePath)
                isModelLoaded = true
                onStatusChange("Mic Ready")
            } catch (e: Exception) {
                onStatusChange("Model Error: ${e.message}")
            }
        }
    }

    // Helper function to recursively copy the asset folder
    private fun copyAssets(assetManager: android.content.res.AssetManager, assetPath: String, destPath: String) {
        val files = assetManager.list(assetPath) ?: return
        val destDir = java.io.File(destPath)
        if (!destDir.exists()) destDir.mkdirs()

        for (filename in files) {
            val fullAssetPath = if (assetPath.isEmpty()) filename else "$assetPath/$filename"
            val destFile = java.io.File(destDir, filename)

            if (filename.contains(".")) {
                // It's a file, copy it
                val inputStream = assetManager.open(fullAssetPath)
                val outputStream = java.io.FileOutputStream(destFile)
                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()
            } else {
                // It's a folder (like 'conf' or 'graph'), recurse into it
                copyAssets(assetManager, fullAssetPath, destFile.absolutePath)
            }
        }
    }

    fun startListening() {
        if (!isModelLoaded || model == null) {
            Toast.makeText(context, "Model not ready yet", Toast.LENGTH_SHORT).show()
            init()
            return
        }

        try {
            val rec = Recognizer(model, 16000.0f, grammar)
            speechService = SpeechService(rec, 16000.0f)
            speechService?.startListening(object : RecognitionListener {
                override fun onPartialResult(hypothesis: String?) {}
                override fun onResult(hypothesis: String?) { if (hypothesis != null) parseResult(hypothesis) }
                override fun onFinalResult(hypothesis: String?) { if (hypothesis != null) parseResult(hypothesis) }
                override fun onError(e: Exception?) { onStatusChange("Error: ${e?.message}") }
                override fun onTimeout() {}
            })
            onStatusChange("Listening...")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopListening() {
        speechService?.stop()
        speechService = null
        onStatusChange("Processing...")
    }

    private fun parseResult(jsonResult: String) {
        try {
            val jsonObject = JSONObject(jsonResult)
            val text = jsonObject.optString("text", "")

            if (text.isNotEmpty() && text.contains("add")) {
                processCommand(text)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun processCommand(command: String) {
        val lowerCommand = command.lowercase()
        val validCategories = listOf("Food", "Transport", "Bills", "Shopping", "Entertainment", "Others")

        // 1. Find Category
        val matchedCategory = validCategories.firstOrNull { lowerCommand.contains(it.lowercase()) } ?: "Others"

        // 2. Find Amount (Digits or Words)
        val amount = extractAmount(lowerCommand)

        if (amount > 0) {
            val timestamp = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            // Construct the Expense Object
            val newExpense = Expense(
                title = "Voice Entry",
                date = dateFormat.format(Date(timestamp)),
                category = matchedCategory,
                description = "Voice: $command",
                timestamp = timestamp,
                items = mutableListOf(ExpenseItem("Item", amount))
            )
            onExpenseParsed(newExpense)
        }
    }

    // Helper to convert "five hundred" -> 500.0
    private fun extractAmount(text: String): Double {
        val digitRegex = "(\\d+)".toRegex()
        val match = digitRegex.find(text)
        if (match != null) return match.value.toDouble()

        var total = 0.0
        var current = 0.0
        val words = text.split(" ")
        val numberMap = mapOf(
            "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
            "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
            "twenty" to 20, "thirty" to 30, "fifty" to 50, "hundred" to 100, "thousand" to 1000
        )

        for (word in words) {
            val value = numberMap[word] ?: continue
            if (value == 100 || value == 1000) {
                current = if (current == 0.0) value.toDouble() else current * value
            } else {
                current += value
            }
        }
        total += current
        return total
    }

    fun destroy() {
        speechService?.shutdown()
    }
}