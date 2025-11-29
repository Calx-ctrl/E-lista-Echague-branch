package com.example.e_lista

data class TopSpendingItem(
    val title: String,
    val amount: Double,
    val date: String,      // Formatted as "MMM dd, yyyy"
    val category: String
)
