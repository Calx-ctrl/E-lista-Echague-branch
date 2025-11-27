package com.example.e_lista

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.e_lista.databinding.ItemExpenseBinding
import com.example.e_lista.databinding.ItemExpenseHeaderBinding
import java.text.SimpleDateFormat
import java.util.*

class TopSpendingAdapter(private val items: List<TopSpendingItem>, private val expenseMap: Map<String, Expense>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object { private const val TYPE_HEADER = 0; private const val TYPE_ITEM = 1 }

    override fun getItemViewType(position: Int) = if (position == 0) TYPE_HEADER else TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val binding = ItemExpenseHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            HeaderViewHolder(binding)
        } else {
            val binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ItemViewHolder(binding)
        }
    }

    override fun getItemCount(): Int = items.size + 1

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            holder.binding.categoryNameTextView.text = "Top Spending"
        } else if (holder is ItemViewHolder) {
            val item = items[position - 1]

            holder.binding.categoryNameTextView.text = item.name
            holder.binding.amountTextView.text = "₱%.2f".format(item.amount)

            val parentExpense = expenseMap[item.name]
            val category = parentExpense?.category ?: "Others"
            val timestamp = parentExpense?.timestamp ?: 0L

            holder.binding.categoryIconImageView.setImageResource(getIconForCategory(category))

            val date = if (timestamp != 0L) {
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                sdf.format(Date(timestamp))
            } else ""
            holder.binding.expenseDateTextView.text = date
        }
    }

    private fun getIconForCategory(category: String): Int {
        return when (category) {
            "Food" -> R.drawable.ic_food
            "Transport" -> R.drawable.ic_car
            "Bills" -> R.drawable.ic_receipt
            "Shopping" -> R.drawable.ic_shopping
            "Entertainment" -> R.drawable.ic_entertainment
            else -> R.drawable.coffee_icon
        }
    }

    inner class HeaderViewHolder(val binding: ItemExpenseHeaderBinding) : RecyclerView.ViewHolder(binding.root)
    inner class ItemViewHolder(val binding: ItemExpenseBinding) : RecyclerView.ViewHolder(binding.root)
}
