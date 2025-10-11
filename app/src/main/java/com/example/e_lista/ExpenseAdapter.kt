package com.example.e_lista

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ExpenseAdapter(private val expenseList: List<Expense>) :
    RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.categoryIconImageView)
        val title: TextView = itemView.findViewById(R.id.categoryNameTextView)
        val date: TextView = itemView.findViewById(R.id.expenseDateTextView)
        val amount: TextView = itemView.findViewById(R.id.amountTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenseList[position]
        holder.icon.setImageResource(expense.iconResId)
        holder.title.text = expense.title
        holder.date.text = expense.date
        holder.amount.text = "₱${expense.amount}" // converted to String + formatted
    }

    override fun getItemCount() = expenseList.size
}
