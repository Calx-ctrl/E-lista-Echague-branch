package com.example.e_lista

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// Main expense model — now parcelable so it can be sent between activities
@Parcelize
data class Expense(
    val id: String = "",          // Firebase key
    val iconResId: Int = 0,
    val title: String = "",
    val date: String = "",
    val amount: Double = 0.0,
    val description: String? = "" // optional
) : Parcelable

// Optional helper model for chart summaries, etc.
data class ExpenseItem(
    val category: String,
    val amount: Double
)
