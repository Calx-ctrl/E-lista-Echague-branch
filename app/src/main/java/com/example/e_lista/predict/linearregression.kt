package com.example.e_lista.predict
import com.example.e_lista.Expense
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.pow

object ExpensePredictor {
    data class PredictionResult(
        val predictedTotal: Double,
        val trend: String // "Increasing" or "Decreasing"
    )

    fun predictNextMonth(expenses: List<Expense>): PredictionResult {
        if (expenses.size < 2) {
            return PredictionResult(0.0, "Insufficient Data")
        }

        // 1. Convert Data to (X, Y) points
        // X = Days since the first expense (to keep numbers small)
        // Y = Expense Amount
        val sortedExpenses = expenses.sortedBy { it.date }
        val firstDate = parseDate(sortedExpenses.first().date) ?: return PredictionResult(0.0, "Error")

        val xValues = mutableListOf<Double>()
        val yValues = mutableListOf<Double>()

        for (expense in sortedExpenses) {
            val currentDate = parseDate(expense.date)
            if (currentDate != null) {
                val diffInMillis = currentDate.time - firstDate.time
                val days = TimeUnit.MILLISECONDS.toDays(diffInMillis).toDouble()

                xValues.add(days)
                yValues.add(expense.total) // Using the computed property from your Expense class
            }
        }

        // 2. Linear Regression Logic (Least Squares Method)
        // Formula: y = mx + c
        val n = xValues.size.toDouble()
        val sumX = xValues.sum()
        val sumY = yValues.sum()
        val sumXY = xValues.zip(yValues).sumOf { (x, y) -> x * y }
        val sumXSquare = xValues.sumOf { it.pow(2) }

        // Calculate Slope (m)
        val slope = (n * sumXY - sumX * sumY) / (n * sumXSquare - sumX.pow(2))

        // Calculate Intercept (c)
        val intercept = (sumY - slope * sumX) / n

        // 3. Predict Future (Next 30 days)
        val lastDay = xValues.maxOrNull() ?: 0.0
        val futureDays = 30
        var futureTotal = 0.0

        for (i in 1..futureDays) {
            val nextDay = lastDay + i
            val predictedAmount = (slope * nextDay) + intercept
            // Ensure we don't predict negative spending
            futureTotal += if (predictedAmount > 0) predictedAmount else 0.0
        }

        val trend = if (slope > 0) "Increasing Spending" else "Decreasing Spending"

        return PredictionResult(futureTotal, trend)
    }

    private fun parseDate(dateStr: String): Date? {
        // Match this to whatever format you save in Firebase (e.g., "yyyy-MM-dd")
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }







}