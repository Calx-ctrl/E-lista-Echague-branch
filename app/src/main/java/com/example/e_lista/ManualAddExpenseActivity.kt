package com.example.e_lista

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class ManualAddExpenseActivity : AppCompatActivity() {

    private lateinit var categoryListContainer: LinearLayout
    private lateinit var addButton: Button
    private lateinit var removeButton: Button

    private val expenseList = mutableListOf<Expense>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_settings)

        categoryListContainer = findViewById(R.id.categoryListContainer)
        addButton = findViewById(R.id.addCategoryButton)
        removeButton = findViewById(R.id.removeCategoryButton)

        addButton.setOnClickListener { showAddExpenseDialog() }
        removeButton.setOnClickListener { removeLastExpense() }
    }

    private fun showAddExpenseDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.activity_add_category_12_1, null)

        val iconPreview = dialogView.findViewById<ImageView>(R.id.iconPreview)
        val iconButton = dialogView.findViewById<Button>(R.id.btnChangeIcon)
        val nameEditText = dialogView.findViewById<EditText>(R.id.inputName)
        val dateEditText = dialogView.findViewById<EditText>(R.id.inputDate)
        val amountEditText = dialogView.findViewById<EditText>(R.id.inputAmount)
        val doneButton = dialogView.findViewById<Button>(R.id.btnDone)

        var selectedIcon = R.drawable.ic_palette
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // ✅ Create dialog first
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        // ✅ Category picker popup
        iconButton.setOnClickListener {
            showCategorySelectionDialog(iconPreview) { newIcon ->
                selectedIcon = newIcon
            }
        }

        // ✅ Date picker
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

        // ✅ Done button action
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
    }

    // ✅ Icon category selection dialog
    private fun showCategorySelectionDialog(iconPreview: ImageView, onIconSelected: (Int) -> Unit) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.activity_category_popup_12_2)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        val scrollView = dialog.findViewById<ScrollView>(R.id.scrollView)
        val rootLayout = scrollView?.getChildAt(0) as? LinearLayout

        rootLayout?.let {
            for (i in 0 until it.childCount) {
                val view = it.getChildAt(i)
                if (view is LinearLayout) {
                    for (j in 0 until view.childCount) {
                        val subView = view.getChildAt(j)
                        if (subView is TextView) {
                            subView.setOnClickListener {
                                val selected = subView.text.toString()
                                val newIcon = getIconForCategory(selected)
                                iconPreview.setImageResource(newIcon)
                                onIconSelected(newIcon)
                                Toast.makeText(this, "Selected: $selected", Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                            }
                        }
                    }
                }
            }
        }
    }

    // ✅ Category → Icon logic
    private fun getIconForCategory(category: String): Int {
        return when {
            category.contains("Rent", true) || category.contains("Grocer", true) || category.contains("Fuel", true) ->
                R.drawable.ic_home
            category.contains("Electricity", true) || category.contains("Internet", true) ->
                R.drawable.ic_lightbulb
            category.contains("Education", true) || category.contains("Health", true) || category.contains("Insurance", true) ->
                R.drawable.ic_family
            category.contains("Movies", true) || category.contains("Games", true) || category.contains("Travel", true) ->
                R.drawable.ic_entertainment
            category.contains("Savings", true) || category.contains("Loan", true) || category.contains("Credit", true) ->
                R.drawable.ic_credit_card
            category.contains("Furniture", true) || category.contains("Home", true) || category.contains("Garden", true) ->
                R.drawable.ic_tools
            category.contains("Donation", true) || category.contains("Misc", true) ->
                R.drawable.ic_misc
            else -> R.drawable.ic_palette
        }
    }

    // ✅ Add expense card dynamically
    private fun addExpenseToView(expense: Expense) {
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

    // ✅ Remove last expense
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
