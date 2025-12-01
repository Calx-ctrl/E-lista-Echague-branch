package com.example.e_lista

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.e_lista.databinding.ItemExpenseBinding
import com.example.e_lista.databinding.ItemExpenseHeaderBinding

class TopSpendingAdapter(private val items: List<TopSpendingItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int =
        if (position == 0) TYPE_HEADER else TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val binding = ItemExpenseHeaderBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            HeaderViewHolder(binding)
        } else {
            val binding = ItemExpenseBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            ItemViewHolder(binding)
        }
    }

    override fun getItemCount(): Int = items.size + 1

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            holder.binding.categoryNameTextView.text = "Top Spending"
        } else if (holder is ItemViewHolder) {
            val item = items[position - 1]

            if (item.title == "No data available") {
                holder.binding.categoryNameTextView.text = item.title
                holder.binding.amountTextView.text = ""
                holder.binding.expenseDateTextView.text = ""
                holder.binding.categoryIconImageView.setImageResource(R.drawable.ic_misc)
            } else {
                holder.binding.categoryNameTextView.text = item.title
                holder.binding.amountTextView.text = "₱%.2f".format(item.amount)
                holder.binding.categoryIconImageView.setImageResource(getIconForCategory(item.category))
                holder.binding.expenseDateTextView.text = item.date
            }
        }
    }

    private fun getIconForCategory(category: String): Int {
        return when (category) {
            "Food" -> R.drawable.ic_food
            "Transport" -> R.drawable.ic_car
            "Bills" -> R.drawable.ic_receipt
            "Shopping" -> R.drawable.ic_shopping
            "Entertainment" -> R.drawable.ic_entertainment
            else -> R.drawable.ic_misc
        }
    }

    inner class HeaderViewHolder(val binding: ItemExpenseHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class ItemViewHolder(val binding: ItemExpenseBinding) :
        RecyclerView.ViewHolder(binding.root)
}
