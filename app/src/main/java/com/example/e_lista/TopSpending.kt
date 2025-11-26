package com.example.e_lista

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.*

class TopSpendingAdapter(
    private val items: List<Expense>,
    private val highlightFirst: Boolean = true // we visually emphasize the first item
) : RecyclerView.Adapter<TopSpendingAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val root: View = view.findViewById(R.id.itemRoot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_top_spending, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val expense = items[position]
        val amount = expense.items.sumOf { it.itemAmount } // uses item amounts
        val nf = NumberFormat.getCurrencyInstance(Locale("en", "PH")) // ₱ locale
        // If your device doesn't show ₱, fallback
        val amountStr = try { nf.format(amount) } catch (e: Exception) { "₱${"%,.2f".format(amount)}" }

        holder.tvAmount.text = amountStr
        holder.tvTitle.text = if (expense.title.isBlank()) expense.category.ifBlank { "Expense" } else expense.title
        holder.tvDate.text = expense.date

        // highlight the first (largest) item
        if (position == 0 && highlightFirst) {
            holder.root.setBackgroundColor(holder.root.resources.getColor(android.R.color.holo_orange_light))
        } else {
            holder.root.setBackgroundColor(holder.root.resources.getColor(android.R.color.white))
        }
    }

    override fun getItemCount(): Int = items.size
}
