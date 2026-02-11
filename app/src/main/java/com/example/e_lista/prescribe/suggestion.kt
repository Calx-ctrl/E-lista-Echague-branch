package com.example.e_lista.prescribe

import com.example.e_lista.Expense
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
object FinancialAdvisor {


    private const val API_KEY = "KEY"

    private val generativeModel = GenerativeModel(
        modelName = "gemini-3-flash-preview",
        apiKey = API_KEY
    )

    suspend fun getAdvice(expenses: List<Expense>): String {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Prepare Data
                val totalSpent = expenses.sumOf { it.total }

                // Group by Category
                val categoryMap = expenses.groupBy { it.category }
                    .mapValues { entry -> entry.value.sumOf { it.total } }

                val topCategory = categoryMap.maxByOrNull { it.value }
                val topCatName = topCategory?.key ?: "General"
                val topCatAmount = topCategory?.value ?: 0.0
                // 2. Build Prompt
                val prompt = """
                    You are a financial advisor.
                    My data:
                    - Total: ₱$totalSpent
                    - Top Category: $topCatName (₱$topCatAmount)
                    
                    Give me 1 specific insight and 2 actionable saving tips for '$topCatName'.
                    Keep it under 3 sentences.
                """.trimIndent()

                // 3. Generate
                val response = generativeModel.generateContent(prompt)
                response.text ?: "No advice generated."

            } catch (e: Exception) {
                // Log the full error to see what's wrong
                e.printStackTrace()
                "AI Service Error: ${e.message}"
            }
        }
    }
}