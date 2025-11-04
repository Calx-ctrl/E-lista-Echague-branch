package com.example.e_lista

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
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

    private val expenseList = mutableListOf<Expense>()
    private val displayedList = mutableListOf<Expense>()

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
        val changeIconButton = dialogView.findViewById<Button>(R.id.btnChangeIcon)
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val nameEditText = dialogView.findViewById<EditText>(R.id.inputName)
        val dateEditText = dialogView.findViewById<EditText>(R.id.inputDate)
        val amountEditText = dialogView.findViewById<EditText>(R.id.inputAmount)
        val descEditText = dialogView.findViewById<EditText>(R.id.inputDescription)
        val doneButton = dialogView.findViewById<Button>(R.id.btnDone)

        val categories = listOf("Food", "Transport", "Bills", "Shopping", "Entertainment", "Others")
        categorySpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)

        var selectedIcon = R.drawable.ic_palette
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        // Icon selection
        changeIconButton.setOnClickListener {
            showIconSelectionPopup(iconPreview) { newIcon ->
                selectedIcon = newIcon
            }
        }

        // Date picker
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

        // Done button
        doneButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val date = dateEditText.text.toString().trim()
            val amountText = amountEditText.text.toString().trim()
            val description = descEditText.text.toString().trim()
            val selectedCategory = categorySpinner.selectedItem.toString()

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
                    Toast.makeText(this, "Failed to add expense: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
        }
    }

    // Edit Expense Dialog
    private fun showExpenseDetailsDialog(expense: Expense) {
        val dialogView = layoutInflater.inflate(R.layout.activity_add_category_12_1, null)
        val container = dialogView.findViewById<LinearLayout>(R.id.dialogContainer)
        val iconPreview = dialogView.findViewById<ImageView>(R.id.iconPreview)
        val iconButton = dialogView.findViewById<Button>(R.id.btnChangeIcon)
        val nameEditText = dialogView.findViewById<EditText>(R.id.inputName)
        val dateEditText = dialogView.findViewById<EditText>(R.id.inputDate)
        val amountEditText = dialogView.findViewById<EditText>(R.id.inputAmount)
        val descEditText = dialogView.findViewById<EditText>(R.id.inputDescription)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val doneButton = dialogView.findViewById<Button>(R.id.btnDone)

        // Initial values
        iconPreview.setImageResource(expense.iconResId)
        nameEditText.setText(expense.title)
        dateEditText.setText(expense.date)
        amountEditText.setText(expense.amount.toString())
        descEditText.setText(expense.description ?: "")

        val categories = listOf("Food", "Transport", "Bills", "Shopping", "Entertainment", "Others")
        spinnerCategory.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinnerCategory.setSelection(
            categories.indexOfFirst { it.equals(expense.category, true) }
                .takeIf { it >= 0 } ?: 0
        )


        setFieldsEditable(false, nameEditText, dateEditText, amountEditText, descEditText, spinnerCategory)
        iconButton.isEnabled = false

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        // Delete button
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

        // Edit / Save toggle
        doneButton.text = "Edit"
        doneButton.setOnClickListener {
            if (doneButton.text == "Edit") {
                setFieldsEditable(true, nameEditText, dateEditText, amountEditText, descEditText, spinnerCategory)
                iconButton.isEnabled = true
                doneButton.text = "Save"

                iconButton.setOnClickListener {
                    showIconSelectionPopup(iconPreview) { newIcon ->
                        iconPreview.setImageResource(newIcon)
                        expense.iconResId = newIcon
                    }
                }

                val cancelButton = Button(this).apply {
                    text = "Cancel"
                    setBackgroundColor(resources.getColor(android.R.color.darker_gray))
                    setTextColor(resources.getColor(android.R.color.white))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = 8 }
                    setOnClickListener {
                        setFieldsEditable(false, nameEditText, dateEditText, amountEditText, descEditText, spinnerCategory)
                        iconButton.isEnabled = false
                        doneButton.text = "Edit"
                        container.removeView(this)
                        iconPreview.setImageResource(expense.iconResId)
                    }
                }
                container.addView(cancelButton)

            } else if (doneButton.text == "Save") {
                val updatedExpense = expense.copy(
                    title = nameEditText.text.toString(),
                    category = spinnerCategory.selectedItem.toString(),  // updated
                    date = dateEditText.text.toString(),
                    amount = amountEditText.text.toString().toDoubleOrNull() ?: 0.0,
                    description = descEditText.text.toString(),
                    iconResId = expense.iconResId
                )

                expenseDatabase.child(expense.id).setValue(updatedExpense).addOnSuccessListener {
                    Toast.makeText(this, "Expense updated", Toast.LENGTH_SHORT).show()
                    setFieldsEditable(false, nameEditText, dateEditText, amountEditText, descEditText, spinnerCategory)
                    iconButton.isEnabled = false
                    doneButton.text = "Edit"
                    dialog.dismiss()
                }
            }
        }
    }

    private fun setFieldsEditable(enabled: Boolean, vararg views: View) {
        views.forEach {
            when (it) {
                is EditText -> it.isEnabled = enabled
                is Spinner -> it.isEnabled = enabled
            }
        }
    }

    // NEW icon selection popup
    private fun showIconSelectionPopup(iconPreview: ImageView, onIconSelected: (Int) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.activity_category_popup_12_2, null)
        val iconGrid = dialogView.findViewById<GridLayout>(R.id.iconGrid)

        val icons = listOf(
            R.drawable.ic_home,
            R.drawable.ic_lightbulb,
            R.drawable.ic_family,
            R.drawable.ic_entertainment,
            R.drawable.ic_credit_card,
            R.drawable.ic_tools,
            R.drawable.ic_misc,
            R.drawable.ic_palette
        )
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        icons.forEach { iconRes ->
            val imageView = ImageView(this).apply {
                setImageResource(iconRes)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 120
                    height = 120
                    setMargins(16,16,16,16)
                }
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setOnClickListener {
                    iconPreview.setImageResource(iconRes)
                    onIconSelected(iconRes)
                    dialog.dismiss() // ✅ now visible
                }
            }
            iconGrid.addView(imageView)
        }
        }
}


