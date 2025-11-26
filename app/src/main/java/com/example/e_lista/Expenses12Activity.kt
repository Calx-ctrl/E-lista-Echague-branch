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
import com.example.e_lista.scanner.ReceiptAnalysis
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.gson.Gson
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
    private var groupedDisplayedList = mutableListOf<GroupedListItem>()
    //item containers
    lateinit var itemContainer: LinearLayout
    var itemCount = 0

    // ExpensesActivity.kt
    enum class FilterType { ALL, DAILY_WEEK, MONTHLY, YEARLY }


    private var currentFilter = FilterType.ALL

    private val categoryIcons = mapOf(
        "Category..." to R.drawable.ic_misc,
        "Food" to R.drawable.ic_food,
        "Transport" to R.drawable.ic_car,
        "Bills" to R.drawable.ic_receipt,
        "Shopping" to R.drawable.ic_shopping,
        "Entertainment" to R.drawable.ic_entertainment,
        "Others" to R.drawable.ic_misc
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpenses12Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // If coming from camera scan
        val analysisJson = intent.getStringExtra("analysis")
        if (analysisJson != null) {
            val analysisObj = Gson().fromJson(analysisJson, ReceiptAnalysis::class.java)
            showAddExpenseDialog(analysisObj) // Pass scanned receipt into dialog
        }

        // Firebase setup
        mAuth = FirebaseAuth.getInstance()
        userID = mAuth.currentUser?.uid ?: "UnknownUser"
        expenseDatabase = FirebaseDatabase.getInstance()
            .getReference("ExpenseData")
            .child(userID)

        binding.btnFilterDate.setOnClickListener {
            openDatePicker()
        }

        // -------------------------------
        // RecyclerView setup
        // -------------------------------
        // Use groupedDisplayedList to support daily/monthly/yearly grouping

        // 1️⃣ Convert displayedList (MutableList<Expense>) into groupedDisplayedList (MutableList<GroupedListItem>)
        groupedDisplayedList = mutableListOf<GroupedListItem>()

// Example grouping by date for daily filter
        val grouped = displayedList.groupBy { it.date } // you can also group by month or year later
        grouped.forEach { (date, expenses) ->
            groupedDisplayedList.add(GroupedListItem.Header(date))  // header for the date
            expenses.forEach { expense ->
                groupedDisplayedList.add(GroupedListItem.ExpenseItem(expense)) // wrap expense
            }
        }

// 2️⃣ Pass the grouped list to the adapter
        adapter = ExpenseAdapter(groupedDisplayedList) { item, _ ->
                showExpenseDetailsDialog(item)
        }
        binding.expenseRecycler.layoutManager = LinearLayoutManager(this)
        binding.expenseRecycler.adapter = adapter


        // Load expenses from Firebase
        loadExpenses()

        // -------------------------------
        // Add Expense button
        // -------------------------------
        binding.btnAddExpense.setOnClickListener { showAddExpenseDialog() }

        // -------------------------------
        // Floating camera button
        // -------------------------------
        binding.fabCamera.setOnClickListener {
            val intent = Intent(this, ReceiptScanUpload::class.java)
            intent.putExtra("parentContext", "Home9Activity")
            startActivity(intent)
        }

        // -------------------------------
        // Bottom navigation setup
        // -------------------------------
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    if (this !is Home9Activity) {
                        startActivity(Intent(this, Home9Activity::class.java))
                        overridePendingTransition(0, 0)
                        finish()
                    }
                    true
                }
                R.id.nav_wallet -> true
                R.id.nav_camera_placeholder -> {
                    if (this !is ReceiptScanUpload) {
                        startActivity(Intent(this, ReceiptScanUpload::class.java))
                        overridePendingTransition(0, 0)
                        finish()
                    }
                    true
                }
                R.id.nav_stats -> {
                    if (this !is ChartDesign10Activity) {
                        startActivity(Intent(this, ChartDesign10Activity::class.java))
                        overridePendingTransition(0, 0)
                        finish()
                    }
                    true
                }
                R.id.nav_profile -> {
                    if (this !is Profile13Activity) {
                        startActivity(Intent(this, Profile13Activity::class.java))
                        overridePendingTransition(0, 0)
                        finish()
                    }
                    true
                }
                else -> false
            }
        }
        binding.bottomNavigationView.selectedItemId = R.id.nav_wallet

        // -------------------------------
        // Filters setup
        // -------------------------------
        binding.filterAll.setOnClickListener {
            expenseList.sortWith(
                compareByDescending<Expense> { it.date }
                    .thenByDescending { it.timestamp }
            )
            applyFilter(FilterType.ALL)
        }

        binding.filterDaily.setOnClickListener {
            expenseList.sortWith(
                compareByDescending<Expense> { it.date }
                    .thenByDescending { it.timestamp }
            )
            applyFilter(FilterType.DAILY_WEEK) // Will group by day
        }

        binding.filterMonthly.setOnClickListener {
            expenseList.sortWith(
                compareByDescending<Expense> { it.date }
                    .thenByDescending { it.timestamp }
            )
            applyFilter(FilterType.MONTHLY) // Will group by month
        }

        binding.filterYearly.setOnClickListener {
            expenseList.sortWith(
                compareByDescending<Expense> { it.date }
                    .thenByDescending { it.timestamp }
            )
            applyFilter(FilterType.YEARLY) // Will group by year
        }

        // Update filter button UI colors
        updateFilterUI()
    }
     private fun openDatePicker() {
        val calendar = Calendar.getInstance()

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datepicker = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->

            val selectedDateStr = "%04d-%02d-%02d".format(selectedYear, selectedMonth + 1, selectedDay)

            // Switch to ALL filter and rebuild grouped list
            applyFilter(FilterType.ALL)

            // Scroll to the selected date header after adapter updates
            binding.expenseRecycler.post {
                scrollToDateHeader(selectedDateStr)
            }

        }, year, month, day)

         datepicker.datePicker.maxDate = System.currentTimeMillis()
         datepicker.show()
    }

    private fun scrollToDateHeader(dateStr: String) {
        val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfOutput = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault()) // Header format used in applyFilter

        val formattedDate = try {
            sdfOutput.format(sdfInput.parse(dateStr) ?: Date())
        } catch (e: Exception) {
            dateStr
        }

        // Find the index of the header in groupedDisplayedList
        val index = groupedDisplayedList.indexOfFirst {
            it is GroupedListItem.Header && it.title == formattedDate
        }

        if (index != -1) {
            val layoutManager = binding.expenseRecycler.layoutManager as? LinearLayoutManager
            layoutManager?.scrollToPositionWithOffset(index, 0) // 0 offset => header at top
        } else {
            Toast.makeText(this, "No expenses found on $formattedDate", Toast.LENGTH_SHORT).show()
        }
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

                expenseList.sortWith(compareByDescending<Expense> { it.date }
                    .thenByDescending { it.timestamp })

                applyFilter(currentFilter) // this will rebuild groupedDisplayedList and notify adapter
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
        groupedDisplayedList.clear()

        val filtered = when (filter) {
            FilterType.ALL -> expenseList               // we'll group by date below
            FilterType.DAILY_WEEK -> expenseList.filter { isInCurrentWeek(it.date) }
            FilterType.MONTHLY -> expenseList.filter { isSameMonth(it.date) }  // current month
            FilterType.YEARLY -> expenseList.filter { isSameYear(it.date) }    // current year
        }

        // Grouping logic
        val grouped = when (filter) {
            FilterType.ALL -> filtered.groupBy { it.date }.toSortedMap(reverseOrder()) // group by date
            FilterType.DAILY_WEEK -> filtered.groupBy { it.date }                      // group by today
            FilterType.MONTHLY -> filtered.groupBy { it.date.substring(0, 7) }        // YYYY-MM
            FilterType.YEARLY -> filtered.groupBy { it.date.substring(0, 4) }         // YYYY
        }

        // Format headers nicely for daily grouping
        val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfOutput = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault()) // Monday, Nov 25, 2025

        grouped.forEach { (groupTitle, expenses) ->
            val formattedTitle = try {
                sdfOutput.format(sdfInput.parse(groupTitle) ?: Date())
            } catch (e: Exception) {
                groupTitle // fallback to original if parsing fails
            }

            groupedDisplayedList.add(GroupedListItem.Header(formattedTitle))
            expenses.forEach { groupedDisplayedList.add(GroupedListItem.ExpenseItem(it)) }
        }

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
        style(binding.filterDaily, currentFilter == FilterType.DAILY_WEEK)
        style(binding.filterMonthly, currentFilter == FilterType.MONTHLY)
        style(binding.filterYearly, currentFilter == FilterType.YEARLY)
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

    private fun isSameYear(dateStr: String) = parseDateSafe(dateStr)?.let { cal ->
        val today = Calendar.getInstance()
        cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
    } ?: false

    private fun isSameMonth(dateStr: String) = parseDateSafe(dateStr)?.let { cal ->
        val today = Calendar.getInstance()
        cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                cal.get(Calendar.MONTH) == today.get(Calendar.MONTH)
    } ?: false

    // Checks if a date string (yyyy-MM-dd) is in the current week
    private fun isInCurrentWeek(dateStr: String): Boolean {
        val cal = parseDateSafe(dateStr) ?: return false
        val today = Calendar.getInstance()

        // get first day of week
        val weekStart = today.clone() as Calendar
        weekStart.set(Calendar.DAY_OF_WEEK, weekStart.firstDayOfWeek)
        weekStart.set(Calendar.HOUR_OF_DAY, 0)
        weekStart.set(Calendar.MINUTE, 0)
        weekStart.set(Calendar.SECOND, 0)
        weekStart.set(Calendar.MILLISECOND, 0)

        // get last day of week
        val weekEnd = weekStart.clone() as Calendar
        weekEnd.add(Calendar.DAY_OF_WEEK, 6)

        return cal.timeInMillis in weekStart.timeInMillis..weekEnd.timeInMillis
    }




    // Add Expense Dialog
    private fun showAddExpenseDialog(
        analysis: ReceiptAnalysis? = null
    ) {
        val dialogView = layoutInflater.inflate(R.layout.activity_add_category_12_1, null)
        val iconPreview = dialogView.findViewById<ImageView>(R.id.iconPreview)
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val nameEditText = dialogView.findViewById<EditText>(R.id.inputName)
        val dateEditText = dialogView.findViewById<EditText>(R.id.inputDate)
        val Total = dialogView.findViewById<TextView>(R.id.Total)
        itemCount = 0
        val itemContainer = dialogView.findViewById<LinearLayout>(R.id.itemContainer)
        addNewItemRow(itemContainer, Total = Total)
        val descEditText = dialogView.findViewById<EditText>(R.id.inputDescription)
        val doneButton = dialogView.findViewById<Button>(R.id.btnDone)

        val categories = listOf(
            "Category...",
            "Food",
            "Transport",
            "Bills",
            "Shopping",
            "Entertainment",
            "Others"
        )
        val adapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            categories
        )

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter
        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedCategory = parent?.getItemAtPosition(position).toString()
                categoryIcons[selectedCategory]?.let { iconRes ->
                    iconPreview.setImageResource(iconRes)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // optional: reset to default icon if nothing selected
                iconPreview.setImageResource(R.drawable.ic_palette)
            }
        }

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        dateEditText.setText(dateFormat.format(calendar.time))

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

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

        analysis?.let { data ->

            // BASIC FIELDS
            nameEditText.setText(data.vendor)

            // ✅ Parse and reformat date to yyyy-MM-dd
            val parsedDate = try {
                // Try parsing common formats
                val possibleFormats = listOf(
                    SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()),
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                )
                var cal: Calendar? = null
                for (fmt in possibleFormats) {
                    try {
                        val date = fmt.parse(data.date)
                        if (date != null) {
                            cal = Calendar.getInstance()
                            cal.time = date
                            break
                        }
                    } catch (e: Exception) {
                        // ignore, try next format
                    }
                }
                cal
            } catch (e: Exception) {
                null
            }

            val sdfOutput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            dateEditText.setText(parsedDate?.let { sdfOutput.format(it.time) } ?: "")

            descEditText.setText(data.receiptID)

            // CLEAR existing item rows
            itemContainer.removeAllViews()
            itemCount = 0

            // REBUILD each row using addNewItemRow()
            data.items.forEach { receiptItem ->
                addNewItemRow(
                    itemContainer,
                    Total = Total,
                    autoAdd = false
                )  // <-- disable auto-add

                val itemView = itemContainer.getChildAt(itemContainer.childCount - 1)
                val itemName = itemView.findViewById<EditText>(R.id.etItem)
                val itemPrice = itemView.findViewById<EditText>(R.id.etAmount)

                itemName.setText(receiptItem.name)

                val priceDouble = receiptItem.price
                    .replace("[^0-9.]".toRegex(), "")
                    .toDoubleOrNull() ?: 0.0

                itemPrice.setText(priceDouble.toString())
            }

            // add one empty row manually at the end
            addNewItemRow(itemContainer, Total = Total)
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
                    val amountText =
                        row.findViewById<EditText>(R.id.etAmount).text.toString().trim()

                    if (itemText.isNotEmpty() || amountText.isNotEmpty()) {
                        // Validate item name
                        if (itemText.isEmpty()) {
                            Toast.makeText(
                                this,
                                "Please enter item name for amount \"$amountText\"",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setOnClickListener
                        }

                        // Validate amount
                        if (amountText.isEmpty()) {
                            Toast.makeText(
                                this,
                                "Please enter amount for item \"$itemText\"",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setOnClickListener
                        }

                        val amountValue = amountText.toDoubleOrNull()
                        if (amountValue == null) {
                            Toast.makeText(
                                this,
                                "Enter a valid number for \"$itemText\"",
                                Toast.LENGTH_SHORT
                            ).show()
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
            val newExpense = Expense(
                id = expenseId,
                title = name,
                date = date,
                category = selectedCategory,
                description = description,
                timestamp = timestamp,
                items = itemsList
            )

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
        Total: TextView? = null,
        autoAdd: Boolean = true   // <-- NEW FLAG
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
                if (!autoAdd) return  // <-- prevent auto-add while pre-filling

                val isLastRow = (row == itemContainer.getChildAt(itemContainer.childCount - 1))
                val itemText = etItem.text.toString().trim()
                val amountText = etAmount.text.toString().trim()
                val isFilled = itemText.isNotEmpty() || amountText.isNotEmpty()

                if (isFilled && isLastRow) {
                    addNewItemRow(itemContainer, Total = Total)
                }

                if (!isFilled && !isLastRow) {
                    itemContainer.removeView(row)
                    renumberRows(itemContainer)
                }

                Total?.let { updateTotal(itemContainer, it) }
            }

            override fun afterTextChanged(s: Editable?) {}
        }

        etItem.addTextChangedListener(watcher)
        etAmount.addTextChangedListener(watcher)

        itemContainer.addView(row)
        renumberRows(itemContainer)
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
        nameEditText.setText(expense.title)
        dateEditText.setText(expense.date)
        descEditText.setText(expense.description ?: "")

        val categories = listOf("Food", "Transport", "Bills", "Shopping", "Entertainment", "Others")
        val adapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            categories
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedCategory = parent?.getItemAtPosition(position).toString()
                categoryIcons[selectedCategory]?.let { iconRes ->
                    iconPreview.setImageResource(iconRes)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // optional: reset to default icon if nothing selected
                iconPreview.setImageResource(R.drawable.ic_palette)
            }
        }


        spinnerCategory.setSelection(
            categories.indexOfFirst { it.equals(expense.category, true) }
                .takeIf { it >= 0 } ?: 0
        )
        itemCount = 0
        // Load all items into item rows
        itemContainer.removeAllViews()
        if (expense.items != null && expense.items.isNotEmpty()) {
            expense.items.forEach { item ->
                addNewItemRow(
                    itemContainer,
                    Total = Total,
                    itemName = item.itemName,
                    amountValue = item.itemAmount
                )
            }
            addNewItemRow(itemContainer, Total = Total)
        } else {
            addNewItemRow(itemContainer, Total = Total) // fallback
        }

        setFieldsEditable(false, nameEditText, dateEditText, descEditText, spinnerCategory)
        setItemRowsEditable(itemContainer, false)


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
                doneButton.text = "Save"


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
                        val itemName =
                            row.findViewById<EditText>(R.id.etItem).text.toString().trim()
                        val amountText =
                            row.findViewById<EditText>(R.id.etAmount).text.toString().trim()

                        if (itemName.isNotEmpty() || amountText.isNotEmpty()) {
                            // Validate item name
                            if (itemName.isEmpty()) {
                                Toast.makeText(
                                    this,
                                    "Please enter item name for amount \"$amountText\"",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@setOnClickListener
                            }

                            // Validate amount
                            if (amountText.isEmpty()) {
                                Toast.makeText(
                                    this,
                                    "Please enter amount for item \"$itemName\"",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@setOnClickListener
                            }

                            val amountValue = amountText.toDoubleOrNull()
                            if (amountValue == null) {
                                Toast.makeText(
                                    this,
                                    "Enter a valid number for \"$itemName\"",
                                    Toast.LENGTH_SHORT
                                ).show()
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
                    items = updatedItems,
                )

                expenseDatabase.child(expense.id).setValue(updatedExpense)
                    .addOnSuccessListener {
                        // 1️⃣ Update local list
                        val index = expenseList.indexOfFirst { it.id == updatedExpense.id }
                        if (index != -1) {
                            expenseList[index] = updatedExpense
                        }

                        // 2️⃣ Re-apply the current filter to refresh groupedDisplayedList & adapter
                        applyFilter(currentFilter)

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
}