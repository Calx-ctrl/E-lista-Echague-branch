package com.example.e_lista

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.e_lista.scanner.ReceiptAnalysis

class ScannedReceipt : AppCompatActivity() {

    private lateinit var productsLayout: LinearLayout
    private lateinit var tvStoreName: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvTotal: TextView
    private lateinit var tvReceiptId: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_confirm_11_1)


        productsLayout = findViewById(R.id.itemContainer)
        tvStoreName = findViewById(R.id.inputName)
        tvDate = findViewById(R.id.inputDate)
        tvTotal = findViewById(R.id.Total)
        tvReceiptId = findViewById(R.id.inputDescription)


        val analysis = intent.getSerializableExtra("analysis") as? ReceiptAnalysis

        if (analysis != null) {
            tvStoreName.text = analysis.vendor
            tvDate.text = "Date: ${analysis.date}"

            tvReceiptId.text = "Receipt ID: ${analysis.receiptID}"

            productsLayout.removeAllViews()
            for (item in analysis.items) {
                val nameView = TextView(this)
                nameView.text = item.name
                nameView.textSize = 16f

                val priceView = TextView(this)
                priceView.text = "₱${item.price}"
                priceView.textSize = 16f
                priceView.setTextColor(getColor(R.color.green_primary))

                productsLayout.addView(nameView)
                productsLayout.addView(priceView)
            }
        }
    }
}
