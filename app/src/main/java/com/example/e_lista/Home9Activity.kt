package com.example.e_lista

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.e_lista.databinding.ActivityHome9Binding
import com.example.e_lista.prescribe.FinancialAdvisor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.launch
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

    // Home9Activity.kt
    enum class FilterType { DAILY, WEEKLY, MONTHLY }
    private var currentFilter = FilterType.DAILY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHome9Binding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFirebase()
        setupRecycler()
        setupFilters()
        setupBottomNav()
        setupButtons()

        loadExpenses()
    }

    // ---------------------------------------------------------
    // INITIAL SETUP
    // ---------------------------------------------------------

    private fun setupFirebase() {
        mAuth = FirebaseAuth.getInstance()
        userID = mAuth.currentUser?.uid ?: "UnknownUser"
        expenseDatabase = FirebaseDatabase.getInstance()
            .getReference("ExpenseData")
            .child(userID)
    }

    private fun setupRecycler() {
        adapter = ExpenseAdapter(groupedDisplayedList) { _, _ -> }
        binding.expensesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.expensesRecyclerView.adapter = adapter
    }

    private fun setupFilters() {
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

    }

    private fun setupButtons() {
        binding.fab.setOnClickListener {
            startActivity(Intent(this, ReceiptScanUpload::class.java))
        }

        binding.seeAll.setOnClickListener {
            startActivity(Intent(this, Expenses12Activity::class.java))
        }
        binding.fabMic.setOnClickListener {
            Toast.makeText(this, "Voice input coming soon 🎤", Toast.LENGTH_SHORT).show()
            // later: start voice recognition
        }

        binding.fabSupport.setOnClickListener {
            // 1. Check if we have data to analyze
            if (expenseList.isEmpty()) {
                Toast.makeText(this, "No expenses to analyze yet!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Show a "Loading..." Dialog
            val loadingDialog = AlertDialog.Builder(this)
                .setTitle("AI Financial Advisor")
                .setMessage("Analyzing your spending habits... Please wait.")
                .setCancelable(false) // Prevent user from clicking away while loading
                .create()
            loadingDialog.show()

            // 3. Call the AI in a background thread
            lifecycleScope.launch {
                try {
                    // Call your object from suggestion.kt
                    val advice = FinancialAdvisor.getAdvice(expenseList)

                    // 4. Close Loading & Show Advice
                    loadingDialog.dismiss()
                    showAdviceResultDialog(advice)

                } catch (e: Exception) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@Home9Activity, "AI Error: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }
    private fun showAdviceResultDialog(advice: String) {
        AlertDialog.Builder(this)
            .setTitle("💡 Smart Suggestions")
            .setMessage(advice)
            .setPositiveButton("Thanks!") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }
    private fun setupBottomNav() {
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
    }

    private fun navigateTo(activityClass: Class<*>): Boolean {
        if (this::class.java != activityClass) {
            startActivity(Intent(this, activityClass))
            overridePendingTransition(0, 0)
            finish()
        }
        return true
    }

    // ---------------------------------------------------------
    // LOAD EXPENSES
    // ---------------------------------------------------------

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

    override fun onStop() {
        super.onStop()
        expensesListener?.let { expenseDatabase.removeEventListener(it) }
    }

    // ---------------------------------------------------------
    // FILTERING & GROUPING
    // ---------------------------------------------------------

    private fun sortExpenses() {
        expenseList.sortWith(
            compareByDescending<Expense> { it.date }
                .thenByDescending { it.timestamp }
        )
    }

    private fun applyGroupedFilter(filter: FilterType) {
        currentFilter = filter
        groupedDisplayedList.clear()

        val today = Calendar.getInstance()

        val filteredExpenses = when (filter) {
            FilterType.DAILY -> expenseList.filter { isSameDay(it.date, today) } // ✅ today only
            FilterType.WEEKLY -> expenseList.filter { isSameWeek(it.date, today) }
            FilterType.MONTHLY -> expenseList.filter { isSameMonth(it.date, today) }
        }

        totalBalance = filteredExpenses.sumOf { it.total }
        binding.totalBalance.text = "₱${"%.2f".format(totalBalance)}"

        val grouped = when (filter) {
            FilterType.DAILY -> filteredExpenses.groupBy { it.date } // group by each date in the week
            FilterType.WEEKLY -> filteredExpenses.groupBy { getWeekLabel(it.date) }
            FilterType.MONTHLY -> filteredExpenses.groupBy { it.date.substring(0, 7) }
        }

        grouped.forEach { (header, list) ->
            groupedDisplayedList.add(GroupedListItem.Header(header))
            list.forEach { groupedDisplayedList.add(GroupedListItem.ExpenseItem(it)) }
        }

        adapter.notifyDataSetChanged()
    }


    // ---------------------------------------------------------
    // DATE PICKER LOGIC
    // ---------------------------------------------------------

    private fun openDatePicker() {
        val today = Calendar.getInstance()
        val year = today.get(Calendar.YEAR)
        val month = today.get(Calendar.MONTH)
        val day = today.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, y, m, d ->
            val selected = String.format("%04d-%02d-%02d", y, m + 1, d)
            filterBySpecificDate(selected)
        }, year, month, day).show()
    }

    private fun filterBySpecificDate(selectedDate: String) {
        groupedDisplayedList.clear()

        val filtered = expenseList.filter { it.date == selectedDate }
        totalBalance = filtered.sumOf { it.total }
        binding.totalBalance.text = "₱${"%.2f".format(totalBalance)}"

        if (filtered.isEmpty()) {
            groupedDisplayedList.add(GroupedListItem.Header("No expenses on $selectedDate"))
        } else {
            groupedDisplayedList.add(GroupedListItem.Header(selectedDate))
            filtered.forEach {
                groupedDisplayedList.add(GroupedListItem.ExpenseItem(it))
            }
        }

        adapter.notifyDataSetChanged()
    }

    // ---------------------------------------------------------
    // DATE HELPERS
    // ---------------------------------------------------------

    private fun parseDateSafe(dateStr: String): Calendar? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateStr) ?: return null
            Calendar.getInstance().apply { time = date }
        } catch (e: Exception) {
            null
        }
    }

    private fun isSameDay(dateStr: String, reference: Calendar): Boolean {
        val cal = parseDateSafe(dateStr) ?: return false
        return cal.get(Calendar.YEAR) == reference.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == reference.get(Calendar.DAY_OF_YEAR)
    }

    private fun isSameWeek(dateStr: String, reference: Calendar): Boolean {
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

    private fun isSameMonth(dateStr: String, reference: Calendar): Boolean {
        val cal = parseDateSafe(dateStr) ?: return false
        return cal.get(Calendar.YEAR) == reference.get(Calendar.YEAR) &&
                cal.get(Calendar.MONTH) == reference.get(Calendar.MONTH)
    }
    private fun isInCurrentWeek(dateStr: String): Boolean {
        val cal = parseDateSafe(dateStr) ?: return false
        val today = Calendar.getInstance()

        // start of week
        val weekStart = today.clone() as Calendar
        weekStart.set(Calendar.DAY_OF_WEEK, weekStart.firstDayOfWeek)
        weekStart.set(Calendar.HOUR_OF_DAY, 0)
        weekStart.set(Calendar.MINUTE, 0)
        weekStart.set(Calendar.SECOND, 0)
        weekStart.set(Calendar.MILLISECOND, 0)

        // end of week
        val weekEnd = weekStart.clone() as Calendar
        weekEnd.add(Calendar.DAY_OF_WEEK, 6)
        weekEnd.set(Calendar.HOUR_OF_DAY, 23)
        weekEnd.set(Calendar.MINUTE, 59)
        weekEnd.set(Calendar.SECOND, 59)
        weekEnd.set(Calendar.MILLISECOND, 999)

        return cal.timeInMillis in weekStart.timeInMillis..weekEnd.timeInMillis
    }


    private fun getWeekLabel(dateStr: String): String {
        val cal = parseDateSafe(dateStr) ?: return ""
        val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
        val startOfWeek = cal.clone() as Calendar
        startOfWeek.set(Calendar.DAY_OF_WEEK, startOfWeek.firstDayOfWeek)
        val endOfWeek = startOfWeek.clone() as Calendar
        endOfWeek.add(Calendar.DAY_OF_WEEK, 6)
        return "Week of ${sdf.format(startOfWeek.time)} - ${sdf.format(endOfWeek.time)}"
    }

}
