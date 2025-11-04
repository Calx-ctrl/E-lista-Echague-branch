package com.example.e_lista

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.e_lista.databinding.ActivityHome9Binding

class Home9Activity : AppCompatActivity() {

    private lateinit var binding: ActivityHome9Binding
    private lateinit var adapter: ExpenseAdapter
    private val expenseList = mutableListOf<Expense>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHome9Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- RecyclerView Setup ---
        adapter = ExpenseAdapter(expenseList) { expense, position ->
            // Handle click on expense item
            Toast.makeText(this, "Clicked: ${expense.title} at position $position", Toast.LENGTH_SHORT).show()
        }

        binding.expensesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.expensesRecyclerView.adapter = adapter

        // --- Check if an expense was passed from Expenses12Activity ---
        intent.getParcelableExtra<Expense>("NEW_EXPENSE")?.let { newExpense ->
            expenseList.add(newExpense)
            adapter.notifyItemInserted(expenseList.size - 1)
        }

        // --- FloatingActionButton Click (Camera) ---
        binding.fab.setOnClickListener {
            startActivity(Intent(this, ReceiptScanUpload::class.java))
        }

        // --- See All Click ---
        binding.seeAll.setOnClickListener {
            startActivity(Intent(this, Expenses12Activity::class.java))
        }

        // --- Bottom Navigation ---
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true

                R.id.nav_wallet -> {
                    if (this !is Expenses12Activity) {
                        startActivity(Intent(this, Expenses12Activity::class.java))
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        finish()
                    }
                    true
                }

                R.id.nav_camera_placeholder -> {
                    if (this !is ReceiptScanUpload) {
                        startActivity(Intent(this, ReceiptScanUpload::class.java))
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        finish()
                    }
                    true
                }

                R.id.nav_stats -> {
                    if (this !is ChartDesign10Activity) {
                        startActivity(Intent(this, ChartDesign10Activity::class.java))
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        finish()
                    }
                    true
                }

                R.id.nav_profile -> {
                    if (this !is Profile13Activity) {
                        startActivity(Intent(this, Profile13Activity::class.java))
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        finish()
                    }
                    true
                }

                else -> false
            }
        }
    }
}
