package com.example.e_lista

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.e_lista.databinding.ActivityExpenses12Binding

class Expenses12Activity : AppCompatActivity() {

    private lateinit var binding: ActivityExpenses12Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // 🧩 Inflate the layout
            binding = ActivityExpenses12Binding.inflate(layoutInflater)
            Log.d("Expenses12Activity", "✅ Layout inflated successfully")
            setContentView(binding.root)
        } catch (e: Exception) {
            Log.e("Expenses12Activity", "❌ Error inflating layout: ${e.message}")
            e.printStackTrace()
            return
        }

        // 🟢 Back button - returns to previous screen
        binding.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 📸 Floating camera button - navigate to CameraActivity
        binding.fabCamera.setOnClickListener {
            val intent = Intent(this, Camera11Activity::class.java)
            startActivity(intent)
        }

        // ➕ Manual Add Expense button
        binding.btnAddExpense.setOnClickListener {
            val intent = Intent(this, ManualAddExpenseActivity::class.java)
            startActivity(intent)
        }

        // ⚙️ Bottom navigation item selection
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, Home9Activity::class.java))
                    true
                }

                R.id.nav_stats -> {
                    startActivity(Intent(this, ChartDesign10Activity::class.java))
                    true
                }

                R.id.nav_wallet -> true // Already here

                R.id.nav_profile -> {
                    startActivity(Intent(this, Profile13Activity::class.java))
                    true
                }

                else -> false
            }
        }
    }
}
