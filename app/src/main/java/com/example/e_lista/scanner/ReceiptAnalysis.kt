package com.example.e_lista.scanner
import java.io.Serializable
import com.example.e_lista.scanner.ReceiptAnalysis

//naka class para mas madali storage sa database
data class ReceiptAnalysis(
    val vendor: String,
    val date: String,
    val total: String,
    val items: List<ReceiptItem>,
    val receiptID: String
): Serializable
