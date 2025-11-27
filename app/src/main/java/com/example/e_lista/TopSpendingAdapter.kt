package com.example.e_lista

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.e_lista.databinding.ItemExpenseBinding
import com.example.e_lista.databinding.ItemExpenseHeaderBinding

class TopSpendingAdapter(private val items: List<TopSpendingItem>) :
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
            holder.binding.expenseDateTextView.text = ""
            holder.binding.categoryIconImageView.setImageResource(R.drawable.ic_category_placeholder)
        }
    }

    inner class HeaderViewHolder(val binding: ItemExpenseHeaderBinding) : RecyclerView.ViewHolder(binding.root)
    inner class ItemViewHolder(val binding: ItemExpenseBinding) : RecyclerView.ViewHolder(binding.root)
}
