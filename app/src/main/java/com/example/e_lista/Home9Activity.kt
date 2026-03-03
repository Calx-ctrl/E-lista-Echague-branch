package com.example.e_lista

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.e_lista.databinding.ActivityHome9Binding
import com.example.e_lista.prescribe.FinancialAdvisor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.widget.TextView
import android.widget.ImageView

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

    private var voiceDialog: BottomSheetDialog? = null
    private var tvVoiceStatus: TextView? = null
    private var tvVoiceText: TextView? = null

    // Home9Activity.kt
    enum class FilterType { DAILY, WEEKLY, MONTHLY }
    private var currentFilter = FilterType.DAILY

    private lateinit var voiceManager: VoiceCommandManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHome9Binding.inflate(layoutInflater)
        setContentView(binding.root)

        //Initialize voice manager
        // Initialize voice manager
        voiceManager = VoiceCommandManager(this,
            onStatusChange = { status ->
                android.util.Log.d("VoiceDebug", "Status: $status")

                // Update the BottomSheet UI if it's showing
                tvVoiceStatus?.text = status

                if (status.contains("Error") || status == "Finished") {
                    binding.root.postDelayed({ voiceDialog?.dismiss() }, 1000)
                }
            },
            onTextHeard = { partialText ->
                // ✅ This updates the popup text in real-time!
                tvVoiceText?.text = partialText
            },
            onExpenseParsed = { expense ->
                saveVoiceExpenseToFirebase(expense)
                voiceDialog?.dismiss()
            }
        )

        // Load the model immediately so it is ready
        voiceManager.init()

        // Check Permissions
        checkMicPermission()

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
    private fun updateButtonColors(activeFilter: FilterType) {
        // Parse the colors (Change the inactive color hex if you want a different shade)
        val activeColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#0F6E52")) // Dark Green
        val inactiveColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4DB6AC")) // Lighter Teal

        // 1. Reset all buttons to the inactive color first
        binding.filterDay.backgroundTintList = inactiveColor
        binding.filterWeek.backgroundTintList = inactiveColor
        binding.filterMonth.backgroundTintList = inactiveColor

        // 2. Apply the active color to the selected button
        when (activeFilter) {
            FilterType.DAILY -> binding.filterDay.backgroundTintList = activeColor
            FilterType.WEEKLY -> binding.filterWeek.backgroundTintList = activeColor
            FilterType.MONTHLY -> binding.filterMonth.backgroundTintList = activeColor
        }
    }
    private fun setupFilters() {
        // Set default state on load
        updateButtonColors(FilterType.DAILY)

        binding.filterDay.setOnClickListener {
            updateButtonColors(FilterType.DAILY)
            sortExpenses()
            applyGroupedFilter(FilterType.DAILY)
        }

        binding.filterWeek.setOnClickListener {
            updateButtonColors(FilterType.WEEKLY)
            sortExpenses()
            applyGroupedFilter(FilterType.WEEKLY)
        }

        binding.filterMonth.setOnClickListener {
            updateButtonColors(FilterType.MONTHLY)
            sortExpenses()
            applyGroupedFilter(FilterType.MONTHLY)
        }
    }

    private fun showVoiceInputDialog() {
        voiceDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_voice_input, null)
        voiceDialog?.setContentView(view)

        tvVoiceStatus = view.findViewById(R.id.tvVoiceStatus)
        tvVoiceText = view.findViewById(R.id.tvVoiceText)
        val ivMicIndicator = view.findViewById<ImageView>(R.id.ivMicIndicator)

        // Stop listening if the user swipes the dialog down to close it
        voiceDialog?.setOnDismissListener {
            voiceManager.stopListening()
        }

        voiceDialog?.show()

        // Start listening as soon as the dialog opens
        voiceManager.startListening()
    }

    private fun setupButtons() {
        binding.fab.setOnClickListener {
            startActivity(Intent(this, ReceiptScanUpload::class.java))
        }

        binding.seeAll.setOnClickListener {
            startActivity(Intent(this, Expenses12Activity::class.java))
        }

        // --- UPDATED MIC BUTTON LOGIC ---
        binding.fabMic.setOnClickListener {
            // TAP TO OPEN GOOGLE-STYLE BOTTOM SHEET
            showVoiceInputDialog()
        }
        // --------------------------------

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

    private fun saveVoiceExpenseToFirebase(expense: Expense) {
        val key = expenseDatabase.push().key ?: return
        expense.id = key

        expenseDatabase.child(key).setValue(expense)
            .addOnSuccessListener {
                Toast.makeText(this, "🎤 Added ₱${expense.total} to ${expense.category}", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save voice expense", Toast.LENGTH_SHORT).show()
            }
    }

    // 5. PERMISSION CHECKER
    private fun checkMicPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
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

    override fun onDestroy() {
        super.onDestroy()
        voiceManager.destroy()
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
