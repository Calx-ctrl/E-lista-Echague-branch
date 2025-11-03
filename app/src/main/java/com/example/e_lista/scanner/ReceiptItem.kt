package com.example.e_lista.scanner
import java.io.Serializable
import com.example.e_lista.scanner.ReceiptAnalysis
data class ReceiptItem(
    val name: String,
    val price: String,

) : Serializable
