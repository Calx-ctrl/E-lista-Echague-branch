package com.example.e_lista

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity

class ManualActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expenses_12)

        // Find the dropdown view by its ID
        val periodDropdown = findViewById<AutoCompleteTextView>(R.id.periodDropdown)

        // Define the options
        val items = listOf("Daily", "Weekly", "Monthly")

        // Create and attach adapter
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        periodDropdown.setAdapter(adapter)

        // 👇 Add this right after setting the adapter
        periodDropdown.setOnClickListener {
            periodDropdown.showDropDown()
        }
    }
}