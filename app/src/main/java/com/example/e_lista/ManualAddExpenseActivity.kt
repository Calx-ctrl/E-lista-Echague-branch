package com.example.e_lista

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ManualAddExpenseActivity : AppCompatActivity() {

    // Main layout views
    private lateinit var categoryListContainer: LinearLayout
    private lateinit var addButton: Button
    private lateinit var removeButton: Button

    // We’ll store the expenses in a list
    private val expenseList = mutableListOf<Expense>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ✅ This should match your layout filename
        setContentView(R.layout.activity_category_settings)

        // ✅ Match the IDs in your XML
        categoryListContainer = findViewById(R.id.categoryListContainer)
        addButton = findViewById(R.id.addCategoryButton)
        removeButton = findViewById(R.id.removeCategoryButton)

        addButton.setOnClickListener { showAddExpenseDialog() }
        removeButton.setOnClickListener { removeLastExpense() }
    }

    private fun showAddExpenseDialog() {
        // ✅ Inflate your dialog layout (activity_add_category.xml)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.activity_add_category_12_1, null)

        val iconButton = dialogView.findViewById<Button>(R.id.btnChangeIcon)
        val nameEditText = dialogView.findViewById<EditText>(R.id.inputName)
        val dateEditText = dialogView.findViewById<EditText>(R.id.inputDate)
        val amountEditText = dialogView.findViewById<EditText>(R.id.inputAmount)
        val doneButton = dialogView.findViewById<Button>(R.id.btnDone)

        var selectedIcon = R.drawable.ic_palette
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Icon button (placeholder)
        iconButton.setOnClickListener {
            Toast.makeText(this, "Icon picker not implemented yet!", Toast.LENGTH_SHORT).show()
        }

        // Date picker
        dateEditText.setOnClickListener {
            DatePickerDialog(this,
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    dateEditText.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        doneButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val date = dateEditText.text.toString().trim()
            val amountText = amountEditText.text.toString().trim()

            if (name.isEmpty() || date.isEmpty() || amountText.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null) {
                Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newExpense = Expense(
                iconResId = selectedIcon,
                title = name,
                date = date,
                amount = amount
            )

            expenseList.add(newExpense)
            addExpenseToView(newExpense)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun addExpenseToView(expense: Expense) {
        // Inflate one item for display (you can use item_expense.xml if you have one)
        val itemView = layoutInflater.inflate(R.layout.item_expense, categoryListContainer, false)

        val iconView = itemView.findViewById<ImageView>(R.id.categoryIconImageView)
        val nameView = itemView.findViewById<TextView>(R.id.categoryNameTextView)
        val dateView = itemView.findViewById<TextView>(R.id.expenseDateTextView)
        val amountView = itemView.findViewById<TextView>(R.id.amountTextView)

        iconView.setImageResource(expense.iconResId)
        nameView.text = expense.title
        dateView.text = expense.date
        amountView.text = "₱%.2f".format(expense.amount)

        categoryListContainer.addView(itemView, 0)
    }

    private fun removeLastExpense() {
        if (expenseList.isNotEmpty()) {
            expenseList.removeAt(expenseList.lastIndex)
            categoryListContainer.removeViewAt(0)
            Toast.makeText(this, "Last category removed", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No categories to remove", Toast.LENGTH_SHORT).show()
        }
    }
}
