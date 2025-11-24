package com.example.e_lista

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// Main expense model — now parcelable so it can be sent between activities

data class ExpenseItem(
    var itemName: String = "",
    var itemAmount: Double = 0.0
)
data class Expense(
    var id: String = "",                 // Firebase key
    var title: String = "",
    var date: String = "",
    val category: String = "",      // NEW
    var description: String? = "",
    var timestamp: Long = 0L,
    var items: MutableList<ExpenseItem> = mutableListOf()

) {
    val total: Double
        get() = items.sumOf { it.itemAmount }
}


