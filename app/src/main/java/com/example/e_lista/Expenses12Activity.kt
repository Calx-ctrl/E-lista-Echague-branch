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
            // Inflate layout safely
            binding = ActivityExpenses12Binding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d("Expenses12Activity", "✅ Layout inflated successfully")
        } catch (e: Exception) {
            Log.e("Expenses12Activity", "❌ Error inflating layout: ${e.message}")
            e.printStackTrace()
            return
        }

        // 🟢 Back button
        binding.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 📸 Floating camera button → CameraActivity
        binding.fabCamera.setOnClickListener {
            val intent = Intent(this, Camera11Activity::class.java)
            startActivity(intent)
        }

        // ➕ Manual Add Expense → sends dummy data to Home9Activity
        binding.btnAddExpense.setOnClickListener {
            // Create test expense data
            val intent = Intent(this, Home9Activity::class.java)
            intent.putExtra("expense_title", "Food")
            intent.putExtra("expense_amount", 250.0)
            intent.putExtra("expense_date", "Oct 10, 2025")
            intent.putExtra("expense_icon", R.drawable.ic_category_placeholder) // You can change this to your icon

            startActivity(intent)
            Toast.makeText(this, "Expense added successfully!", Toast.LENGTH_SHORT).show()
        }

        // ⚙️ Bottom Navigation
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

        // ✨ Highlight the Wallet (Expenses) icon in bottom nav
        binding.bottomNavigationView.selectedItemId = R.id.nav_wallet
    }
}
