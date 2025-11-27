package com.example.e_lista

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.e_lista.databinding.ItemTopSpendingBinding

class TopSpendingAdapter(private val items: List<TopSpendingItem>) :
    RecyclerView.Adapter<TopSpendingAdapter.TopSpendingViewHolder>() {

    inner class TopSpendingViewHolder(val binding: ItemTopSpendingBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopSpendingViewHolder {
        val binding = ItemTopSpendingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TopSpendingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TopSpendingViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvItemName.text = item.name
        holder.binding.tvItemAmount.text = "₱%.2f".format(item.amount)
    }

    override fun getItemCount(): Int = items.size
}
