package com.example.e_lista

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.e_lista.databinding.ActivityExpenses12Binding
import java.text.SimpleDateFormat
import java.util.*

class Expenses12Activity : AppCompatActivity() {

    private lateinit var binding: ActivityExpenses12Binding
    private val expenseList = mutableListOf<Expense>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityExpenses12Binding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d("Expenses12Activity", "✅ Layout inflated successfully")
        } catch (e: Exception) {
            Log.e("Expenses12Activity", "❌ Error inflating layout: ${e.message}")
            e.printStackTrace()
            return
        }

        // 🟢 Back button
        binding.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // ➕ Manual Add Expense — show popup dialog
        binding.btnAddExpense.setOnClickListener {
            showAddExpenseDialog()
        }

        // ⚙️ Bottom Navigation
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, Home9Activity::class.java))
                    true
                }
                R.id.nav_stats -> {
                    startActivity(Intent(this, ChartDesign10Activity::class.java))
                    true
                }
                R.id.nav_wallet -> true // Already here
                R.id.nav_profile -> {
                    startActivity(Intent(this, Profile13Activity::class.java))
                    true
                }
                else -> false
            }
        }

        // ✨ Highlight Wallet icon
        binding.bottomNavigationView.selectedItemId = R.id.nav_wallet
    }

    // ================================
    // 🧩 Manual Add Popup Logic Below
    // ================================

    private fun showAddExpenseDialog() {
        val dialogView = layoutInflater.inflate(R.layout.activity_add_category_12_1, null)

        val iconPreview = dialogView.findViewById<ImageView>(R.id.iconPreview)
        val iconButton = dialogView.findViewById<Button>(R.id.btnChangeIcon)
        val nameEditText = dialogView.findViewById<EditText>(R.id.inputName)
        val dateEditText = dialogView.findViewById<EditText>(R.id.inputDate)
        val amountEditText = dialogView.findViewById<EditText>(R.id.inputAmount)
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

        // Category selection
        iconButton.setOnClickListener {
            showCategorySelectionDialog(iconPreview) { newIcon ->
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

        // Done button — validate and add
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
            Toast.makeText(this, "Expense added successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    // Category popup
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

    // Dynamically add expense to your expense list UI
    private fun addExpenseToView(expense: Expense) {
        val itemView = layoutInflater.inflate(R.layout.item_expense, binding.expenseListContainer, false)

        val iconView = itemView.findViewById<ImageView>(R.id.categoryIconImageView)
        val nameView = itemView.findViewById<TextView>(R.id.categoryNameTextView)
        val dateView = itemView.findViewById<TextView>(R.id.expenseDateTextView)
        val amountView = itemView.findViewById<TextView>(R.id.amountTextView)

        iconView.setImageResource(expense.iconResId)
        nameView.text = expense.title
        dateView.text = expense.date
        amountView.text = "₱%.2f".format(expense.amount)

        binding.expenseListContainer.addView(itemView, 0)
    }
}
