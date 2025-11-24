package com.example.e_lista

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

// Sealed class for grouped list items
sealed class GroupedListItem {
    data class Header(val title: String) : GroupedListItem()
    data class ExpenseItem(val expense: Expense) : GroupedListItem()
}

class ExpenseAdapter(
    private val itemList: MutableList<GroupedListItem>,
    private val onExpenseClick: (expense: Expense, position: Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val categoryIcons = mapOf(
        "Food" to R.drawable.ic_food,
        "Transport" to R.drawable.ic_car,
        "Bills" to R.drawable.ic_receipt,
        "Shopping" to R.drawable.ic_shopping,
        "Entertainment" to R.drawable.ic_entertainment,
        "Others" to R.drawable.ic_misc
    )

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    // ViewHolder for expense items
    inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.categoryIconImageView)
        val title: TextView = itemView.findViewById(R.id.categoryNameTextView)
        val date: TextView = itemView.findViewById(R.id.expenseDateTextView)
        val amount: TextView = itemView.findViewById(R.id.amountTextView)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION && getItemViewType(position) == TYPE_ITEM) {
                    val item = itemList[position] as GroupedListItem.ExpenseItem
                    onExpenseClick(item.expense, position)
                }
            }
        }
    }

    // ViewHolder for headers
    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val headerTitle: TextView = itemView.findViewById(R.id.categoryNameTextView)
    }

    override fun getItemViewType(position: Int): Int {
        return when (itemList[position]) {
            is GroupedListItem.Header -> TYPE_HEADER
            is GroupedListItem.ExpenseItem -> TYPE_ITEM
        }
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
        val item = itemList[position]
        when (item) {
            is GroupedListItem.Header -> {
                (holder as HeaderViewHolder).headerTitle.text = item.title
            }
            is GroupedListItem.ExpenseItem -> {
                holder as ExpenseViewHolder
                val expense = item.expense
                val iconRes = categoryIcons[expense.category] ?: R.drawable.ic_palette
                holder.icon.setImageResource(iconRes)
                holder.title.text = expense.title
                holder.date.text = expense.date.ifEmpty {
                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
                }
                holder.amount.text = "₱%.2f".format(expense.total)
            }
        }
    }

    override fun getItemCount(): Int = itemList.size

    fun updateList(newList: List<GroupedListItem>) {
        itemList.clear()
        itemList.addAll(newList)
        notifyDataSetChanged()
    }
}
