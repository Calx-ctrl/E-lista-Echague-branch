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
            startActivity(Intent(this, ReceiptScanUpload::class.java))
        }

        // Bottom navigation
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { navigateTo(Home9Activity::class.java); true }
                R.id.nav_wallet -> true
                R.id.nav_camera_placeholder -> { navigateTo(ReceiptScanUpload::class.java); true }
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

        // 🆕 Add product list EditText
        val productListEditText = EditText(this).apply {
            hint = "Product list (e.g. Milk - ₱50, Bread - ₱30)"
            setPadding(16, 16, 16, 16)
        }

        val container = dialogView.findViewById<LinearLayout>(R.id.dialogContainer)
        container.addView(productListEditText, container.childCount - 1)

        val categories = listOf("Food", "Transport", "Bills", "Shopping", "Entertainment", "Others")
        categorySpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)

        var selectedIcon = R.drawable.ic_palette
        iconPreview.setImageResource(selectedIcon)

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        // ✅ Fix: Make icon change work
        changeIconButton.setOnClickListener {
            showIconSelectionPopup(iconPreview) { newIcon ->
                selectedIcon = newIcon
                iconPreview.setImageResource(newIcon)
            }
        }

        // ✅ Fix: Date picker for adding
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
            val productList = productListEditText.text.toString().trim()
            val category = categorySpinner.selectedItem.toString()

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
            val newExpense = Expense(
                id = expenseId,
                iconResId = selectedIcon,
                title = name,
                date = date,
                amount = amount,
                description = description,
                category = category,
                productList = productList
            )

            expenseDatabase.child(expenseId).setValue(newExpense)
                .addOnSuccessListener {
                    Toast.makeText(this, "Expense added", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to add: ${it.message}", Toast.LENGTH_SHORT).show()
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

        // 🆕 Add product list field for editing
        val productListEditText = EditText(this).apply {
            hint = "Product list (e.g. Milk - ₱50)"
            setPadding(16, 16, 16, 16)
            setText(expense.productList ?: "")
        }
        container.addView(productListEditText, container.childCount - 1)

        // Load initial data
        iconPreview.setImageResource(expense.iconResId)
        nameEditText.setText(expense.title)
        dateEditText.setText(expense.date)
        amountEditText.setText(expense.amount.toString())
        descEditText.setText(expense.description ?: "")

        val categories = listOf("Food", "Transport", "Bills", "Shopping", "Entertainment", "Others")
        spinnerCategory.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinnerCategory.setSelection(categories.indexOfFirst { it.equals(expense.category, true) }.takeIf { it >= 0 } ?: 0)

        setFieldsEditable(false, nameEditText, dateEditText, amountEditText, descEditText, spinnerCategory, productListEditText)
        iconButton.isEnabled = false

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        val deleteButton = Button(this).apply {
            text = "Delete"
            setBackgroundColor(resources.getColor(android.R.color.holo_red_light))
            setTextColor(resources.getColor(android.R.color.white))
            setOnClickListener {
                expenseDatabase.child(expense.id).removeValue()
                dialog.dismiss()
            }
        }
        container.addView(deleteButton)

        doneButton.text = "Edit"
        doneButton.setOnClickListener {
            if (doneButton.text == "Edit") {
                setFieldsEditable(true, nameEditText, dateEditText, amountEditText, descEditText, spinnerCategory, productListEditText)
                iconButton.isEnabled = true
                doneButton.text = "Save"

                iconButton.setOnClickListener {
                    showIconSelectionPopup(iconPreview) { newIcon ->
                        expense.iconResId = newIcon
                        iconPreview.setImageResource(newIcon)
                    }
                }

                // ✅ Make date editable
                dateEditText.setOnClickListener {
                    val cal = Calendar.getInstance()
                    DatePickerDialog(
                        this,
                        { _, year, month, day ->
                            cal.set(year, month, day)
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            dateEditText.setText(dateFormat.format(cal.time))
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }

            } else {
                val updatedExpense = expense.copy(
                    title = nameEditText.text.toString(),
                    category = spinnerCategory.selectedItem.toString(),
                    date = dateEditText.text.toString(),
                    amount = amountEditText.text.toString().toDoubleOrNull() ?: 0.0,
                    description = descEditText.text.toString(),
                    productList = productListEditText.text.toString()
                )

                expenseDatabase.child(expense.id).setValue(updatedExpense).addOnSuccessListener {
                    Toast.makeText(this, "Updated successfully", Toast.LENGTH_SHORT).show()
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
            R.drawable.ic_receipt
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


