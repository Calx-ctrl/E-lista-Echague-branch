package com.example.e_lista

import android.content.Intent
import android.os.Bundle
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
    private var expensesListener: ValueEventListener? = null

    private val expenseList = mutableListOf<Expense>()
    private val groupedDisplayedList = mutableListOf<GroupedListItem>()
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
        adapter = ExpenseAdapter(groupedDisplayedList) { expense, _ ->
            // Handle click on expense item
            //Toast.makeText(this, "Clicked: ${expense.title}", Toast.LENGTH_SHORT).show()
        }
        binding.expensesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.expensesRecyclerView.adapter = adapter

        // Load expenses from Firebase
        loadExpenses()

        // Filter buttons
        binding.filterDay.setOnClickListener {
            sortExpenses()
            applyGroupedFilter(FilterType.DAILY)
        }
        binding.filterWeek.setOnClickListener {
            sortExpenses()
            applyGroupedFilter(FilterType.WEEKLY)
        }
        binding.filterMonth.setOnClickListener {
            sortExpenses()
            applyGroupedFilter(FilterType.MONTHLY)
        }

        // Floating action button
        binding.fab.setOnClickListener {
            startActivity(Intent(this, ReceiptScanUpload::class.java))
        }

        // Bottom navigation
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_wallet -> navigateTo(Expenses12Activity::class.java)
                R.id.nav_camera_placeholder -> navigateTo(ReceiptScanUpload::class.java)
                R.id.nav_stats -> navigateTo(ChartDesign10Activity::class.java)
                R.id.nav_profile -> navigateTo(Profile13Activity::class.java)
                else -> false
            }
        }
        binding.bottomNavigationView.selectedItemId = R.id.nav_home

        // See All button
        binding.seeAll.setOnClickListener {
            startActivity(Intent(this, Expenses12Activity::class.java))
        }
    }

    private fun navigateTo(activityClass: Class<*>) : Boolean {
        if (this::class.java != activityClass) {
            startActivity(Intent(this, activityClass))
            overridePendingTransition(0, 0)
            finish()
        }
        return true
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
                sortExpenses()
                applyGroupedFilter(currentFilter)
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

    private fun sortExpenses() {
        expenseList.sortWith(
            compareByDescending<Expense> { it.date }
                .thenByDescending { it.timestamp }
        )
    }

    private fun applyGroupedFilter(filter: FilterType) {
        currentFilter = filter
        groupedDisplayedList.clear()
        totalBalance = 0.0

        val today = Calendar.getInstance() // reference point

        val filteredExpenses = when (filter) {
            FilterType.DAILY -> expenseList.filter { isSameDay(it.date, today) }
            FilterType.WEEKLY -> expenseList.filter { isSameWeek(it.date, today) }
            FilterType.MONTHLY -> expenseList.filter { isSameMonth(it.date, today) }
        }


        // Update total balance
        totalBalance = filteredExpenses.sumOf { it.total }
        binding.totalBalance.text = "₱${"%.2f".format(totalBalance)}"

        // Group by date (or week/month label)
        val grouped = when (filter) {
            FilterType.DAILY -> filteredExpenses.groupBy { it.date }
            FilterType.WEEKLY -> filteredExpenses.groupBy { getWeekLabel(it.date) }
            FilterType.MONTHLY -> filteredExpenses.groupBy { it.date.substring(0, 7) } // YYYY-MM
        }

        grouped.forEach { (groupTitle, expenses) ->
            groupedDisplayedList.add(GroupedListItem.Header(groupTitle))
            expenses.forEach { groupedDisplayedList.add(GroupedListItem.ExpenseItem(it)) }
        }

        adapter.notifyDataSetChanged()
    }

    private fun isSameDay(dateStr: String, reference: Calendar = Calendar.getInstance()): Boolean {
        val cal = parseDateSafe(dateStr) ?: return false
        return cal.get(Calendar.YEAR) == reference.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == reference.get(Calendar.DAY_OF_YEAR)
    }

    private fun isSameWeek(dateStr: String, reference: Calendar = Calendar.getInstance()): Boolean {
        val cal = parseDateSafe(dateStr) ?: return false
        val weekStart = reference.clone() as Calendar
        weekStart.set(Calendar.DAY_OF_WEEK, weekStart.firstDayOfWeek)
        weekStart.set(Calendar.HOUR_OF_DAY, 0)
        weekStart.set(Calendar.MINUTE, 0)
        weekStart.set(Calendar.SECOND, 0)
        weekStart.set(Calendar.MILLISECOND, 0)

        val weekEnd = weekStart.clone() as Calendar
        weekEnd.add(Calendar.DAY_OF_WEEK, 6)
        weekEnd.set(Calendar.HOUR_OF_DAY, 23)
        weekEnd.set(Calendar.MINUTE, 59)
        weekEnd.set(Calendar.SECOND, 59)
        weekEnd.set(Calendar.MILLISECOND, 999)

        return cal.timeInMillis in weekStart.timeInMillis..weekEnd.timeInMillis
    }

    private fun isSameMonth(dateStr: String, reference: Calendar = Calendar.getInstance()): Boolean {
        val cal = parseDateSafe(dateStr) ?: return false
        return cal.get(Calendar.YEAR) == reference.get(Calendar.YEAR) &&
                cal.get(Calendar.MONTH) == reference.get(Calendar.MONTH)
    }


    // Helper to return week label like "Week of Nov 24"
    private fun getWeekLabel(dateStr: String): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(dateStr)
        val cal = Calendar.getInstance().apply { time = date }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())
        return "Week of $month $day"
    }
    private fun parseDateSafe(dateStr: String): Calendar? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateStr) ?: return null
            Calendar.getInstance().apply { time = date }
        } catch (e: Exception) {
            null
        }
    }
}
