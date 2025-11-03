package com.example.e_lista

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.e_lista.databinding.ActivityExpenses12Binding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class Expenses12Activity : AppCompatActivity() {

    private lateinit var binding: ActivityExpenses12Binding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var userID: String
    private lateinit var expenseDatabase: DatabaseReference
    private lateinit var adapter: ExpenseAdapter

    private val expenseList = mutableListOf<Expense>() // Full list from Firebase
    private val displayedList = mutableListOf<Expense>() // Filtered list

    enum class FilterType { ALL, DAILY, WEEKLY, MONTHLY }
    private var currentFilter = FilterType.ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpenses12Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase setup
        mAuth = FirebaseAuth.getInstance()
        userID = mAuth.currentUser?.uid ?: "UnknownUser"
        expenseDatabase = FirebaseDatabase.getInstance()
            .getReference("ExpenseData")
            .child(userID)

        // RecyclerView setup
        adapter = ExpenseAdapter(displayedList) { expense, _ ->
            showExpenseDetailsDialog(expense)
        }
        binding.expenseRecycler.layoutManager = LinearLayoutManager(this)
        binding.expenseRecycler.adapter = adapter

        // Load expenses
        loadExpenses()

        // Add Expense button
        binding.btnAddExpense.setOnClickListener { showAddExpenseDialog() }

        // Floating camera button
        binding.fabCamera.setOnClickListener {
            startActivity(Intent(this, Camera11Activity::class.java))
        }

        // Bottom navigation
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { navigateTo(Home9Activity::class.java); true }
                R.id.nav_wallet -> true
                R.id.nav_camera_placeholder -> { navigateTo(Camera11Activity::class.java); true }
                R.id.nav_stats -> { navigateTo(ChartDesign10Activity::class.java); true }
                R.id.nav_profile -> { navigateTo(Profile13Activity::class.java); true }
                else -> false
            }
        }
        binding.bottomNavigationView.selectedItemId = R.id.nav_wallet

        // Filters
        binding.filterAll.setOnClickListener { applyFilter(FilterType.ALL) }
        binding.filterDaily.setOnClickListener { applyFilter(FilterType.DAILY) }
        binding.filterWeekly.setOnClickListener { applyFilter(FilterType.WEEKLY) }
        binding.filterMonthly.setOnClickListener { applyFilter(FilterType.MONTHLY) }

        updateFilterUI()
    }

    private fun navigateTo(cls: Class<*>) {
        if (this::class.java != cls) {
            startActivity(Intent(this, cls))
            finish()
        }
    }

    private fun loadExpenses() {
        expenseDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                expenseList.clear()
                for (expenseSnap in snapshot.children) {
                    val expense = expenseSnap.getValue(Expense::class.java)
                    if (expense != null) expenseList.add(expense)
                }
                applyFilter(currentFilter)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@Expenses12Activity,
                    "Failed to load expenses: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun applyFilter(filter: FilterType) {
        currentFilter = filter
        displayedList.clear()

        val filtered = when (filter) {
            FilterType.ALL -> expenseList
            FilterType.DAILY -> expenseList.filter { isSameDay(it.date) }
            FilterType.WEEKLY -> expenseList.filter { isSameWeek(it.date) }
            FilterType.MONTHLY -> expenseList.filter { isSameMonth(it.date) }
        }

        displayedList.addAll(filtered)
        adapter.notifyDataSetChanged()
        updateFilterUI()
    }

    private fun updateFilterUI() {
        val activeColor = resources.getColor(R.color.green_primary, theme)
        val inactiveColor = resources.getColor(R.color.gray_light, theme)
        val white = resources.getColor(android.R.color.white)
        val black = resources.getColor(android.R.color.black)

        fun style(button: Button, isActive: Boolean) {
            button.setBackgroundColor(if (isActive) activeColor else inactiveColor)
            button.setTextColor(if (isActive) white else black)
        }

        style(binding.filterAll, currentFilter == FilterType.ALL)
        style(binding.filterDaily, currentFilter == FilterType.DAILY)
        style(binding.filterWeekly, currentFilter == FilterType.WEEKLY)
        style(binding.filterMonthly, currentFilter == FilterType.MONTHLY)
    }

    // Safe date parsing
    private fun parseDateSafe(dateStr: String): Calendar? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cal = Calendar.getInstance()
            cal.time = sdf.parse(dateStr) ?: return null
            cal
        } catch (e: Exception) {
            null
        }
    }

    private fun isSameDay(dateStr: String) = parseDateSafe(dateStr)?.let { cal ->
        val today = Calendar.getInstance()
        cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    } ?: false

    private fun isSameWeek(dateStr: String) = parseDateSafe(dateStr)?.let { cal ->
        val today = Calendar.getInstance()
        cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                cal.get(Calendar.WEEK_OF_YEAR) == today.get(Calendar.WEEK_OF_YEAR)
    } ?: false

    private fun isSameMonth(dateStr: String) = parseDateSafe(dateStr)?.let { cal ->
        val today = Calendar.getInstance()
        cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                cal.get(Calendar.MONTH) == today.get(Calendar.MONTH)
    } ?: false

    // Add Expense Dialog
    private fun showAddExpenseDialog() {
        val dialogView = layoutInflater.inflate(R.layout.activity_add_category_12_1, null)
        val iconPreview = dialogView.findViewById<ImageView>(R.id.iconPreview)
        val iconButton = dialogView.findViewById<Button>(R.id.btnChangeIcon)
        val nameEditText = dialogView.findViewById<EditText>(R.id.inputName)
        val dateEditText = dialogView.findViewById<EditText>(R.id.inputDate)
        val amountEditText = dialogView.findViewById<EditText>(R.id.inputAmount)
        val descEditText = dialogView.findViewById<EditText>(R.id.inputDescription)
        val doneButton = dialogView.findViewById<Button>(R.id.btnDone)

        var selectedIcon = R.drawable.ic_palette
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        iconButton.setOnClickListener {
            showCategorySelectionDialog(iconPreview) { newIcon -> selectedIcon = newIcon }
        }

        dateEditText.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    dateEditText.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        doneButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val date = dateEditText.text.toString().trim()
            val amountText = amountEditText.text.toString().trim()
            val description = descEditText.text.toString().trim()

            if (name.isEmpty() || date.isEmpty() || amountText.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null) {
                Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val expenseId = expenseDatabase.push().key ?: return@setOnClickListener
            val newExpense = Expense(expenseId, selectedIcon, name, date, amount, description)

            expenseDatabase.child(expenseId).setValue(newExpense)
                .addOnSuccessListener {
                    Toast.makeText(this, "Added successfully", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to add expense: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showExpenseDetailsDialog(expense: Expense) {
        val dialogView = layoutInflater.inflate(R.layout.activity_add_category_12_1, null)
        val iconPreview = dialogView.findViewById<ImageView>(R.id.iconPreview)
        val nameEditText = dialogView.findViewById<EditText>(R.id.inputName)
        val dateEditText = dialogView.findViewById<EditText>(R.id.inputDate)
        val amountEditText = dialogView.findViewById<EditText>(R.id.inputAmount)
        val descEditText = dialogView.findViewById<EditText>(R.id.inputDescription)
        val doneButton = dialogView.findViewById<Button>(R.id.btnDone)
        val container = dialogView.findViewById<LinearLayout>(R.id.dialogContainer)

        iconPreview.setImageResource(expense.iconResId)
        nameEditText.setText(expense.title)
        dateEditText.setText(expense.date)
        amountEditText.setText(expense.amount.toString())
        descEditText.setText(expense.description ?: "")

        nameEditText.isEnabled = false
        dateEditText.isEnabled = false
        amountEditText.isEnabled = false
        descEditText.isEnabled = false
        doneButton.text = "Close"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        doneButton.setOnClickListener { dialog.dismiss() }

        val deleteButton = Button(this).apply {
            text = "Delete"
            setBackgroundColor(resources.getColor(android.R.color.holo_red_light))
            setTextColor(resources.getColor(android.R.color.white))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 16 }

            setOnClickListener {
                expenseDatabase.child(expense.id).removeValue().addOnSuccessListener {
                    Toast.makeText(this@Expenses12Activity, "Expense deleted", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
        }
        container.addView(deleteButton)
    }

    private fun showCategorySelectionDialog(iconPreview: ImageView, onIconSelected: (Int) -> Unit) {
        val dialog = AlertDialog.Builder(this).create()
        val categories = listOf("Home", "Family", "Credit", "Entertainment", "Tools", "Misc")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        for (cat in categories) {
            val tv = TextView(this)
            tv.text = cat
            tv.textSize = 16f
            tv.setPadding(24, 16, 24, 16)
            tv.setOnClickListener {
                val iconRes = getIconForCategory(cat)
                iconPreview.setImageResource(iconRes)
                onIconSelected(iconRes)
                dialog.dismiss()
            }
            layout.addView(tv)
        }
        dialog.setView(layout)
        dialog.show()
    }

    private fun getIconForCategory(category: String): Int {
        return when (category) {
            "Rent", "Groceries", "Fuel", "Electricity", "Internet", "Home", "Garden" -> R.drawable.ic_home
            "Education", "Health", "Insurance" -> R.drawable.ic_family
            "Savings", "Loan", "Credit" -> R.drawable.ic_credit_card
            "Movies", "Games", "Travel" -> R.drawable.ic_entertainment
            "Furniture" -> R.drawable.ic_tools
            "Donation", "Misc" -> R.drawable.ic_misc
            else -> R.drawable.ic_palette
        }
    }
}


