package com.example.e_lista

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.e_lista.databinding.ActivityHome9Binding
import com.google.android.material.bottomnavigation.BottomNavigationView

class Home9Activity : AppCompatActivity() {

    private lateinit var binding: ActivityHome9Binding
    private lateinit var adapter: ExpenseAdapter
    private val expenseList = mutableListOf<Expense>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHome9Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- RecyclerView Setup ---
        binding.expensesRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ExpenseAdapter(expenseList)
        binding.expensesRecyclerView.adapter = adapter

        // --- Check if an expense was passed from Expenses12Activity ---
        intent.getParcelableExtra<Expense>("NEW_EXPENSE")?.let { newExpense ->
            expenseList.add(newExpense)
            adapter.notifyItemInserted(expenseList.size - 1)
        }

        // --- FloatingActionButton Click (Camera) ---
        binding.fab.setOnClickListener {
            startActivity(Intent(this, Camera11Activity::class.java))
        }

        // --- See All Click ---
        binding.seeAll.setOnClickListener {
            startActivity(Intent(this, Expenses12Activity::class.java))
        }

        // --- Bottom Navigation Setup ---
        val bottomNavigation: BottomNavigationView = binding.bottomNavigationView
        bottomNavigation.selectedItemId = R.id.nav_home

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_wallet -> {
                    startActivity(Intent(this, Expenses12Activity::class.java))
                    true
                }
                R.id.nav_camera_placeholder -> {
                    startActivity(Intent(this, Camera11Activity::class.java))
                    true
                }
                R.id.nav_stats -> {
                    startActivity(Intent(this, ChartDesign10Activity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, Profile13Activity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}
