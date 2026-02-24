package com.example.e_lista

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class ExpenseItem(
    var itemName: String = "",
    var itemAmount: Double = 0.0
)

data class Expense(
    var id: String = "",
    var title: String = "",
    var date: String = "",
    val category: String = "",
    var description: String? = "",
    var location: String = "",       // <-- NEW: Added Location field
    var timestamp: Long = 0L,
    var items: MutableList<ExpenseItem> = mutableListOf()

) {
    val total: Double
        get() = items.sumOf { it.itemAmount }
}