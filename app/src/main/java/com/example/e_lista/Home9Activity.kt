package com.example.e_lista

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.e_lista.databinding.ActivityHome9Binding
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class Home9Activity : AppCompatActivity() {

    private lateinit var binding: ActivityHome9Binding
    private lateinit var expenseDatabase: DatabaseReference
    private lateinit var adapter: ExpenseAdapter
    private val expenseList = mutableListOf<Expense>()
    private var displayedList = mutableListOf<Expense>()
    private var totalBalance = 0.0

    enum class FilterType { DAILY, WEEKLY, MONTHLY }
    private var currentFilter = FilterType.DAILY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHome9Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase setup
        expenseDatabase = FirebaseDatabase.getInstance().getReference("ExpenseData").child("yourUserId").child("expenses") // Replace "yourUserId" with the actual user ID

        // RecyclerView setup
        adapter = ExpenseAdapter(displayedList) { expense, _ ->
            // Handle click on expense item
            Toast.makeText(this, "Clicked: ${expense.title}", Toast.LENGTH_SHORT).show()
        }
        binding.expensesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.expensesRecyclerView.adapter = adapter

        // Load expenses from Firebase
        loadExpenses()

        // Button actions for filters
        binding.filterDay.setOnClickListener {
            applyFilter(FilterType.DAILY)
        }

        binding.filterWeek.setOnClickListener {
            applyFilter(FilterType.WEEKLY)
        }

        binding.filterMonth.setOnClickListener {
            applyFilter(FilterType.MONTHLY)
        }

        // Bottom navigation setup

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    true
                }

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
        binding.bottomNavigationView.selectedItemId = R.id.nav_home
        // "See All" button action
        binding.seeAll.setOnClickListener {
            startActivity(Intent(this, Expenses12Activity::class.java))
        }
    }

    private fun loadExpenses() {
        expenseDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                expenseList.clear()
                for (expenseSnap in snapshot.children) {
                    val expense = expenseSnap.getValue(Expense::class.java)
                    expense?.let { expenseList.add(it) }
                }
                applyFilter(currentFilter)  // Apply the current filter to display expenses
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Home9Activity, "Failed to load expenses: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun applyFilter(filter: FilterType) {
        currentFilter = filter
        displayedList.clear()
        totalBalance = 0.0

        val calendar = Calendar.getInstance()
        val today = calendar.time

        // Filter expenses based on selected filter (Daily, Weekly, Monthly)
        when (filter) {
            FilterType.DAILY -> {
                displayedList = expenseList.filter { isSameDay(it.date, today) }.toMutableList()
            }
            FilterType.WEEKLY -> {
                displayedList = expenseList.filter { isSameWeek(it.date, today) }.toMutableList()
            }
            FilterType.MONTHLY -> {
                displayedList = expenseList.filter { isSameMonth(it.date, today) }.toMutableList()
            }
        }

        // Update total balance
        totalBalance = displayedList.sumOf { it.amount }

        // Update UI
        binding.totalBalance.text = "₱${"%.2f".format(totalBalance)}"
        adapter.notifyDataSetChanged()
    }

    private fun isSameDay(dateStr: String, today: Date): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val expenseDate = sdf.parse(dateStr)
        val calExpense = Calendar.getInstance()
        calExpense.time = expenseDate

        val calToday = Calendar.getInstance()
        calToday.time = today

        return calExpense.get(Calendar.YEAR) == calToday.get(Calendar.YEAR) &&
                calExpense.get(Calendar.DAY_OF_YEAR) == calToday.get(Calendar.DAY_OF_YEAR)
    }

    private fun isSameWeek(dateStr: String, today: Date): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val expenseDate = sdf.parse(dateStr)
        val calExpense = Calendar.getInstance()
        calExpense.time = expenseDate

        val calToday = Calendar.getInstance()
        calToday.time = today

        return calExpense.get(Calendar.YEAR) == calToday.get(Calendar.YEAR) &&
                calExpense.get(Calendar.WEEK_OF_YEAR) == calToday.get(Calendar.WEEK_OF_YEAR)
    }

    private fun isSameMonth(dateStr: String, today: Date): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val expenseDate = sdf.parse(dateStr)
        val calExpense = Calendar.getInstance()
        calExpense.time = expenseDate

        val calToday = Calendar.getInstance()
        calToday.time = today

        return calExpense.get(Calendar.YEAR) == calToday.get(Calendar.YEAR) &&
                calExpense.get(Calendar.MONTH) == calToday.get(Calendar.MONTH)
    }
}
