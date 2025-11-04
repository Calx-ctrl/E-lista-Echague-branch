package com.example.e_lista

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ExpenseAdapter(
    private val expenseList: MutableList<Expense>,
    private val onExpenseClick: (expense: Expense, position: Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    // ViewHolder for expense item
    inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.categoryIconImageView)
        val title: TextView = itemView.findViewById(R.id.categoryNameTextView)
        val date: TextView = itemView.findViewById(R.id.expenseDateTextView)
        val amount: TextView = itemView.findViewById(R.id.amountTextView)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION && getItemViewType(position) == TYPE_ITEM) {
                    onExpenseClick(expenseList[position], position)
                }
            }
        }
    }

    // ViewHolder for header
    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val headerTitle: TextView = itemView.findViewById(R.id.categoryNameTextView)
    }

    override fun getItemViewType(position: Int): Int {
        return if (expenseList[position].iconResId == 0) TYPE_HEADER else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_expense_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_expense, parent, false)
            ExpenseViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val expense = expenseList[position]
        if (getItemViewType(position) == TYPE_HEADER) {
            (holder as HeaderViewHolder).headerTitle.text = expense.title
        } else {
            holder as ExpenseViewHolder
            holder.icon.setImageResource(expense.iconResId)
            holder.title.text = expense.title
            holder.date.text = if (expense.date.isNotEmpty()) expense.date
            else SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
            holder.amount.text = "₱%.2f".format(expense.amount)
        }
    }

    override fun getItemCount(): Int = expenseList.size

    fun addExpense(expense: Expense) {
        expenseList.add(0, expense)
        notifyItemInserted(0)
    }

    fun removeExpenseAt(position: Int) {
        if (position in 0 until expenseList.size) {
            expenseList.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun updateExpenses(newList: List<Expense>) {
        expenseList.clear()
        expenseList.addAll(newList)
        notifyDataSetChanged()
    }
}
