    package com.example.e_lista

    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.ImageView
    import android.widget.TextView
    import androidx.recyclerview.widget.RecyclerView
    import java.text.SimpleDateFormat
    import java.util.*
    import com.example.e_lista.Expense
    import com.example.e_lista.R

    class ExpenseAdapter(private val expenseList: MutableList<Expense>) :
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

            // ✅ Set values
            holder.icon.setImageResource(expense.iconResId)
            holder.title.text = expense.title

            // Use provided date, or current date if empty
            val formattedDate = if (expense.date.isNotEmpty()) {
                expense.date
            } else {
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
            }
            holder.date.text = formattedDate

            holder.amount.text = "₱%.2f".format(expense.amount)
        }

        override fun getItemCount() = expenseList.size

        // ✅ Add new expense dynamically
        fun addExpense(expense: Expense) {
            expenseList.add(0, expense)
            notifyItemInserted(0)
        }

        // ✅ Optionally replace entire list
        fun updateExpenses(newList: List<Expense>) {
            expenseList.clear()
            expenseList.addAll(newList)
            notifyDataSetChanged()
        }
    }
