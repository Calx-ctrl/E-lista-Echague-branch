package com.example.e_lista

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
    //bugfix for database trying to load after logging out
    private var expensesListener: ValueEventListener? = null

    //item containers
    lateinit var itemContainer: LinearLayout
    var itemCount = 0

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
            val intent = Intent(this, ReceiptScanUpload::class.java)
            intent.putExtra("parentContext", "Home9Activity")
            startActivity(intent)
        }

        // Bottom navigation
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    if (this !is Home9Activity) {
                        startActivity(Intent(this, Home9Activity::class.java))
                       // overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                        overridePendingTransition(0, 0)
                        finish()
                    }
                    true
                }

                R.id.nav_wallet -> {
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
                        //overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
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
        binding.bottomNavigationView.selectedItemId = R.id.nav_wallet

        // Filters
        binding.filterAll.setOnClickListener {
            expenseList.sortWith(
                compareByDescending<Expense> { it.date }  // Sort by date first
                    .thenByDescending { it.timestamp }    // Then by full time inside the date
            )
            applyFilter(FilterType.ALL)
        }
        binding.filterDaily.setOnClickListener {
            expenseList.sortWith(
                compareByDescending<Expense> { it.date }  // Sort by date first
                    .thenByDescending { it.timestamp }    // Then by full time inside the date
            )
            applyFilter(FilterType.DAILY)
        }
        binding.filterWeekly.setOnClickListener {
            expenseList.sortWith(
                compareByDescending<Expense> { it.date }  // Sort by date first
                    .thenByDescending { it.timestamp }    // Then by full time inside the date
            )
            applyFilter(FilterType.WEEKLY)
        }
        binding.filterMonthly.setOnClickListener {
            expenseList.sortWith(
                compareByDescending<Expense> { it.date }  // Sort by date first
                    .thenByDescending { it.timestamp }    // Then by full time inside the date
            )
            applyFilter(FilterType.MONTHLY)
        }

        updateFilterUI()
    }

    private fun navigateTo(cls: Class<*>) {
        if (this::class.java != cls) {
            startActivity(Intent(this, cls))
            finish()
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
                    this@Expenses12Activity,
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
        val Total = dialogView.findViewById<TextView>(R.id.Total)
        itemCount = 0
        val itemContainer = dialogView.findViewById<LinearLayout>(R.id.itemContainer)
        addNewItemRow(itemContainer, Total = Total)
        val descEditText = dialogView.findViewById<EditText>(R.id.inputDescription)
        val doneButton = dialogView.findViewById<Button>(R.id.btnDone)

        val categories = listOf("Category...", "Food", "Transport", "Bills", "Shopping", "Entertainment", "Others")
        categorySpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)

        var selectedIcon = R.drawable.ic_palette
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        dateEditText.setText(dateFormat.format(calendar.time))

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
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    dateEditText.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            // 🔒 Prevent selecting future dates
            datePicker.datePicker.maxDate = System.currentTimeMillis()

            datePicker.show()
        }

        // Done button
        doneButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val date = dateEditText.text.toString().trim()
            //val amountText = amountEditText.text.toString().trim()
            val description = descEditText.text.toString().trim()
            val selectedCategory = categorySpinner.selectedItem.toString()
            val timestamp = System.currentTimeMillis()

            if (selectedCategory == "Category...") {
                Toast.makeText(this, "Please choose a category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (name.isEmpty() || date.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val itemsList = mutableListOf<ExpenseItem>()

            for (i in 0 until itemContainer.childCount) {
                val row = itemContainer.getChildAt(i)

                if (row is LinearLayout && row.tag == "itemRow") {
                    val itemText = row.findViewById<EditText>(R.id.etItem).text.toString().trim()
                    val amountText = row.findViewById<EditText>(R.id.etAmount).text.toString().trim()

                    if (itemText.isNotEmpty() || amountText.isNotEmpty()) {
                        // Validate item name
                        if (itemText.isEmpty()) {
                            Toast.makeText(this, "Please enter item name for amount \"$amountText\"", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        // Validate amount
                        if (amountText.isEmpty()) {
                            Toast.makeText(this, "Please enter amount for item \"$itemText\"", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        val amountValue = amountText.toDoubleOrNull()
                        if (amountValue == null) {
                            Toast.makeText(this, "Enter a valid number for \"$itemText\"", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        itemsList.add(ExpenseItem(itemText, amountValue))
                    }

                }
            }

            // ❗ Check if user added at least 1 item
            if (itemsList.isEmpty()) {
                Toast.makeText(this, "Add at least one item", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val expenseId = expenseDatabase.push().key ?: return@setOnClickListener
            val newExpense = Expense(id=expenseId,
                                    iconResId=selectedIcon,
                                    title= name,
                                    date= date,
                                    category = selectedCategory,
                                    description = description,
                                    timestamp = timestamp,
                                    items = itemsList  )

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

    private fun addNewItemRow(
        itemContainer: LinearLayout,
        itemName: String = "",
        amountValue: Double? = null,
        Total: TextView? = null   // <-- NEW
    ) {
        itemCount++

        val row = layoutInflater.inflate(R.layout.item_row, itemContainer, false)
        row.tag = "itemRow"

        val tvIndex = row.findViewById<TextView>(R.id.tvIndex)
        val etItem = row.findViewById<EditText>(R.id.etItem)
        val etAmount = row.findViewById<EditText>(R.id.etAmount)

        tvIndex.text = itemCount.toString()
        if (itemName.isNotEmpty()) etItem.setText(itemName)
        amountValue?.let { etAmount.setText(it.toString()) }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val isLastRow = (row == itemContainer.getChildAt(itemContainer.childCount - 1))
                val itemText = etItem.text.toString().trim()
                val amountText = etAmount.text.toString().trim()
                val isFilled = itemText.isNotEmpty() || amountText.isNotEmpty()

                if (isFilled && isLastRow) {
                    addNewItemRow(itemContainer, Total = Total) // pass tvTotal down
                }

                if (!isFilled && !isLastRow) {
                    itemContainer.removeView(row)
                    renumberRows(itemContainer)
                }

                // 🔥 UPDATE TOTAL whenever amount changes
                Total?.let { updateTotal(itemContainer, it) }
            }

            override fun afterTextChanged(s: Editable?) {}
        }

        etItem.addTextChangedListener(watcher)
        etAmount.addTextChangedListener(watcher)

        itemContainer.addView(row)
        renumberRows(itemContainer)

        // 🔥 INITIAL total update
        Total?.let { updateTotal(itemContainer, it) }
    }




    private fun renumberRows(container: LinearLayout) {
        var index = 1
        for (i in 0 until container.childCount) {
            val row = container.getChildAt(i)
            if (row.tag == "itemRow") {
                val tvIndex = row.findViewById<TextView>(R.id.tvIndex)
                tvIndex.text = index.toString()
                index++
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
        val descEditText = dialogView.findViewById<EditText>(R.id.inputDescription)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val itemContainer = dialogView.findViewById<LinearLayout>(R.id.itemContainer)
        val doneButton = dialogView.findViewById<Button>(R.id.btnDone)
        val Total = dialogView.findViewById<TextView>(R.id.Total)
        // ----------------------------------------------------------
        // 1️⃣ INITIAL VALUES
        // ----------------------------------------------------------
        iconPreview.setImageResource(expense.iconResId)
        nameEditText.setText(expense.title)
        dateEditText.setText(expense.date)
        descEditText.setText(expense.description ?: "")

        val categories = listOf("Food", "Transport", "Bills", "Shopping", "Entertainment", "Others")
        spinnerCategory.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)

        spinnerCategory.setSelection(
            categories.indexOfFirst { it.equals(expense.category, true) }
                .takeIf { it >= 0 } ?: 0
        )
        itemCount = 0
        // Load all items into item rows
        itemContainer.removeAllViews()
        if (expense.items != null && expense.items.isNotEmpty()) {
            expense.items.forEach { item ->
                addNewItemRow(itemContainer, Total = Total, itemName = item.itemName, amountValue = item.itemAmount)
            }
            addNewItemRow(itemContainer, Total = Total)
        } else {
            addNewItemRow(itemContainer, Total = Total) // fallback
        }

        setFieldsEditable(false, nameEditText, dateEditText, descEditText, spinnerCategory)
        setItemRowsEditable(itemContainer, false)
        iconButton.isEnabled = false

        // ----------------------------------------------------------
        // 2️⃣ DATE PICKER
        // ----------------------------------------------------------
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        dateEditText.setOnClickListener {
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    dateEditText.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            datePicker.datePicker.maxDate = System.currentTimeMillis()
            datePicker.show()
        }

        // ----------------------------------------------------------
        // 3️⃣ SHOW DIALOG
        // ----------------------------------------------------------
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        // ----------------------------------------------------------
        // 4️⃣ DELETE BUTTON
        // ----------------------------------------------------------
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
                    Toast.makeText(
                        this@Expenses12Activity,
                        "Expense deleted",
                        Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
                }
            }
        }
        container.addView(deleteButton)

        // ----------------------------------------------------------
        // 5️⃣ EDIT / SAVE TOGGLE
        // ----------------------------------------------------------
        doneButton.text = "Edit"
        doneButton.setOnClickListener {

            if (doneButton.text == "Edit") {
                // ENABLE ALL FIELDS
                setFieldsEditable(true, nameEditText, dateEditText, descEditText, spinnerCategory)
                setItemRowsEditable(itemContainer, true)
                iconButton.isEnabled = true
                doneButton.text = "Save"

                // Icon selector
                iconButton.setOnClickListener {
                    showIconSelectionPopup(iconPreview) { newIcon ->
                        iconPreview.setImageResource(newIcon)
                        expense.iconResId = newIcon
                    }
                }

                // Cancel button dynamically added
                val cancelButton = Button(this).apply {
                    text = "Cancel"
                    setBackgroundColor(resources.getColor(android.R.color.darker_gray))
                    setTextColor(resources.getColor(android.R.color.white))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = 8 }
                    setOnClickListener {
                        dialog.dismiss() // Close without saving
                    }
                }
                container.addView(cancelButton)

            } else if (doneButton.text == "Save") {
                // ----------------------------------------------------------
                // 6️⃣ BUILD UPDATED ITEM LIST
                // ----------------------------------------------------------
                val updatedItems = mutableListOf<ExpenseItem>()
                for (i in 0 until itemContainer.childCount) {
                    val row = itemContainer.getChildAt(i)
                    if (row.tag == "itemRow") {
                        val itemName = row.findViewById<EditText>(R.id.etItem).text.toString().trim()
                        val amountText = row.findViewById<EditText>(R.id.etAmount).text.toString().trim()

                        if (itemName.isNotEmpty() || amountText.isNotEmpty()) {
                            // Validate item name
                            if (itemName.isEmpty()) {
                                Toast.makeText(this, "Please enter item name for amount \"$amountText\"", Toast.LENGTH_SHORT).show()
                                return@setOnClickListener
                            }

                            // Validate amount
                            if (amountText.isEmpty()) {
                                Toast.makeText(this, "Please enter amount for item \"$itemName\"", Toast.LENGTH_SHORT).show()
                                return@setOnClickListener
                            }

                            val amountValue = amountText.toDoubleOrNull()
                            if (amountValue == null) {
                                Toast.makeText(this, "Enter a valid number for \"$itemName\"", Toast.LENGTH_SHORT).show()
                                return@setOnClickListener
                            }

                            updatedItems.add(ExpenseItem(itemName, amountValue))
                        }
                    }
                }

                if (updatedItems.isEmpty()) {
                    Toast.makeText(this, "Add at least one item", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // ----------------------------------------------------------
                // 7️⃣ SAVE UPDATED EXPENSE
                // ----------------------------------------------------------
                val updatedExpense = expense.copy(
                    title = nameEditText.text.toString(),
                    category = spinnerCategory.selectedItem.toString(),
                    date = dateEditText.text.toString(),
                    description = descEditText.text.toString(),
                    iconResId = expense.iconResId,
                    items = updatedItems,
                )

                expenseDatabase.child(expense.id).setValue(updatedExpense)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Expense updated", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
            }
        }
    }

    private fun updateTotal(itemContainer: LinearLayout, Total: TextView) {
        var total = 0.0
        for (i in 0 until itemContainer.childCount) {
            val row = itemContainer.getChildAt(i)
            if (row.tag == "itemRow") {
                val amountText = row.findViewById<EditText>(R.id.etAmount).text.toString().trim()
                val amount = amountText.toDoubleOrNull() ?: 0.0
                total += amount
            }
        }
        Total.text = "Total: $total"
    }

    private fun setFieldsEditable(enabled: Boolean, vararg views: View) {
        views.forEach {
            when (it) {
                is EditText -> it.isEnabled = enabled
                is Spinner -> it.isEnabled = enabled
            }
        }
    }

    private fun setItemRowsEditable(container: LinearLayout, enabled: Boolean) {
        for (i in 0 until container.childCount) {
            val row = container.getChildAt(i)
            if (row.tag == "itemRow") {
                row.findViewById<EditText>(R.id.etItem).isEnabled = enabled
                row.findViewById<EditText>(R.id.etAmount).isEnabled = enabled
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


