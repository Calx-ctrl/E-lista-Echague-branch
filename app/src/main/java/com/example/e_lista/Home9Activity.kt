package com.example.e_lista

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.e_lista.databinding.ActivityHome9Binding
import com.google.android.material.bottomnavigation.BottomNavigationView

class Home9Activity : AppCompatActivity() {

    private lateinit var binding: ActivityHome9Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHome9Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- RecyclerView Setup (Placeholder example) ---
        binding.expensesRecyclerView.layoutManager = LinearLayoutManager(this)
        // Example placeholder data adapter (you can replace this later)
        val sampleExpenses = listOf(
            Expense(R.drawable.food_icon, "Food", "Oct 10, 2025", 120.0),
            Expense(R.drawable.car_icon, "Transport", "Oct 9, 2025", 80.0),
            Expense(R.drawable.coffee_icon, "Coffee", "Oct 8, 2025", 60.0)
        )
        val adapter = ExpenseAdapter(sampleExpenses)
        binding.expensesRecyclerView.adapter = adapter

        // --- FloatingActionButton Click ---
        binding.fab.setOnClickListener {
            Toast.makeText(this, "Camera button clicked!", Toast.LENGTH_SHORT).show()
            // TODO: Replace with your camera activity or expense add feature
        }

        // --- See All Click ---
        binding.seeAll.setOnClickListener {
            Toast.makeText(this, "Viewing all expenses...", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to a full expense history screen if available
        }

        val bottomNavigation: BottomNavigationView = binding.bottomNavigationView

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Already in Home — no action needed
                    true
                }

                R.id.nav_wallet -> {
                    val intent = Intent(this, Expenses12Activity::class.java)
                    startActivity(intent)
                    true
                }

                R.id.nav_camera_placeholder -> {
                    val intent = Intent(this, Camera11Activity::class.java)
                    startActivity(intent)
                    true
                }

                R.id.nav_stats -> {
                    val intent = Intent(this, ChartDesign10Activity::class.java)
                    startActivity(intent)
                    true
                }

                R.id.nav_profile -> {
                    val intent = Intent(this, Profile13Activity::class.java)
                    startActivity(intent)
                    true
                }

                else -> false
            }
        }
    }
    }
