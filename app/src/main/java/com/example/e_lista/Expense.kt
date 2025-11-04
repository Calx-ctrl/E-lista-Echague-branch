package com.example.e_lista

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// Main expense model — now parcelable so it can be sent between activities
@Parcelize
data class Expense(
    var id: String = "",                 // Firebase key
    var iconResId: Int = R.drawable.ic_palette, // default icon
    var title: String = "",
    var date: String = "",
    var amount: Double = 0.0,
    var description: String? = ""        // optional
) : Parcelable

// Optional helper model for chart summaries, etc.
data class ExpenseItem(
    val category: String,
    val amount: Double
)
