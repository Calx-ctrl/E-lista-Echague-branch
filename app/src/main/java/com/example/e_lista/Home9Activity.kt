package com.example.e_lista

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.e_lista.databinding.ActivityHome9Binding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class Home9Activity : AppCompatActivity() {

    private lateinit var binding: ActivityHome9Binding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var userID: String
    private lateinit var expenseDatabase: DatabaseReference
    private lateinit var adapter: ExpenseAdapter
    //bugfix for database trying to load after logging out
    private var expensesListener: ValueEventListener? = null

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
        mAuth = FirebaseAuth.getInstance()
        userID = mAuth.currentUser?.uid ?: "UnknownUser"
        expenseDatabase = FirebaseDatabase.getInstance()
            .getReference("ExpenseData")
            .child(userID)


        // RecyclerView setup
        adapter = ExpenseAdapter(displayedList) { expense, _ ->
            // Handle click on expense item
            //Toast.makeText(this, "Clicked: ${expense.title}", Toast.LENGTH_SHORT).show()
        }
        binding.expensesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.expensesRecyclerView.adapter = adapter

        // Load expenses from Firebase
        loadExpenses()

        // Button actions for filters
        binding.filterDay.setOnClickListener {
            expenseList.sortWith(
                compareByDescending<Expense> { it.date }  // Sort by date first
                    .thenByDescending { it.timestamp }    // Then by full time inside the date
            )
            applyFilter(FilterType.DAILY)
        }

        binding.filterWeek.setOnClickListener {
            expenseList.sortWith(
                compareByDescending<Expense> { it.date }  // Sort by date first
                    .thenByDescending { it.timestamp }    // Then by full time inside the date
            )
            applyFilter(FilterType.WEEKLY)
        }

        binding.filterMonth.setOnClickListener {
            expenseList.sortWith(
                compareByDescending<Expense> { it.date }  // Sort by date first
                    .thenByDescending { it.timestamp }    // Then by full time inside the date
            )
            applyFilter(FilterType.MONTHLY)
        }

        binding.fab.setOnClickListener {
            startActivity(Intent(this, ReceiptScanUpload::class.java))
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
                        //overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        overridePendingTransition(0, 0)
                        finish()
                    }
                    true
                }

                R.id.nav_camera_placeholder -> {
                    if (this !is ReceiptScanUpload) {
                        startActivity(Intent(this, ReceiptScanUpload::class.java))
                        //overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        overridePendingTransition(0, 0)
                        finish()
                    }
                    true
                }

                R.id.nav_stats -> {
                    if (this !is ChartDesign10Activity) {
                        startActivity(Intent(this, ChartDesign10Activity::class.java))
                        //overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        overridePendingTransition(0, 0)
                        finish()
                    }
                    true
                }

                R.id.nav_profile -> {
                    if (this !is Profile13Activity) {
                        startActivity(Intent(this, Profile13Activity::class.java))
                        //overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        overridePendingTransition(0, 0)
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

    override fun onStop() {
        super.onStop()
        expensesListener?.let {
            expenseDatabase.removeEventListener(it)
        }
    }
    private fun loadExpenses() {
        expensesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                expenseList.clear()
                for (expenseSnap in snapshot.children) {
                    val expense = expenseSnap.getValue(Expense::class.java)
                    expense?.let { expenseList.add(it) }
                }

                expenseList.sortWith(
                    compareByDescending<Expense> { it.date }  // Sort by date first
                        .thenByDescending { it.timestamp }    // Then by full time inside the date
                )
                applyFilter(currentFilter)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@Home9Activity,
                    "Failed to load expenses: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        expenseDatabase.addValueEventListener(expensesListener!!)
    }

    private fun applyFilter(filter: FilterType) {
        currentFilter = filter
        displayedList.clear()
        totalBalance = 0.0

        val calendar = Calendar.getInstance()
        val today = calendar.time

        // Filter expenses based on selected filter (Daily, Weekly, Monthly)
        displayedList.addAll(
            when (filter) {
                FilterType.DAILY -> expenseList.filter { isSameDay(it.date, today) }
                FilterType.WEEKLY -> expenseList.filter { isSameWeek(it.date, today) }
                FilterType.MONTHLY -> expenseList.filter { isSameMonth(it.date, today) }
            }
        )


        // Update total balance
        totalBalance = displayedList.sumOf { it.total }

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
