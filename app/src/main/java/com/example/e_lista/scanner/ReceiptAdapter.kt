package com.example.e_lista.scanner


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.e_lista.R


class ReceiptAdapter(private var items: List<ReceiptItem>) :
    RecyclerView.Adapter<ReceiptAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtName: TextView = view.findViewById(R.id.txtName)
        val txtPrice: TextView = view.findViewById(R.id.txtPrice)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_confirm_11_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.txtName.text = item.name
        holder.txtPrice.text = "₱${item.price}"
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<ReceiptItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
