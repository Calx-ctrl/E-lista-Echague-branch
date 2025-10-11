package com.example.e_lista

data class Expense(
    val iconResId: Int,   // example: R.drawable.food_icon
    val title: String,    // example: "Food"
    val date: String,     // example: "Oct 10, 2025"
    val amount: Double,    // example: 120
)