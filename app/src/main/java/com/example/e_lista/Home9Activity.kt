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
            expenseList.sortByDescending { it.date }
            applyFilter(FilterType.DAILY)
        }

        binding.filterWeek.setOnClickListener {
            expenseList.sortByDescending { it.date }
            applyFilter(FilterType.WEEKLY)
        }

        binding.filterMonth.setOnClickListener {
            expenseList.sortByDescending { it.date }
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

    private fun loadExpenses() {
        expenseDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                expenseList.clear()
                for (expenseSnap in snapshot.children) {
                    val expense = expenseSnap.getValue(Expense::class.java)
                    expense?.let { expenseList.add(it) }
                }
                expenseList.sortByDescending { it.date }
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
        displayedList.addAll(
            when (filter) {
                FilterType.DAILY -> expenseList.filter { isSameDay(it.date, today) }
                FilterType.WEEKLY -> expenseList.filter { isSameWeek(it.date, today) }
                FilterType.MONTHLY -> expenseList.filter { isSameMonth(it.date, today) }
            }
        )


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

                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to add expense: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            dialog.dismiss()
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
                    Toast.makeText(this@Home9Activity, "Expense deleted", Toast.LENGTH_SHORT).show()
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
                }
                dialog.dismiss()
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
