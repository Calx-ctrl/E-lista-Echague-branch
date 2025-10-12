package com.example.e_lista
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// ✅ Main expense model — now parcelable so it can be sent between activities
@Parcelize
data class Expense(
    val iconResId: Int,   // Example: R.drawable.food_icon
    val title: String,    // Example: "Food"
    val date: String,     // Example: "Oct 10, 2025"
    val amount: Double    // Example: 120.0
) : Parcelable

// ✅ Optional helper model for chart summaries, etc.
data class ExpenseItem(
    val category: String,
    val amount: Double
)
