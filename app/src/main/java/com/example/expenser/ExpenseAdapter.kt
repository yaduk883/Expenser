package com.example.expenser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ExpenseAdapter(
    private var expenses: List<Expense>,
    private val onItemClick: (Expense) -> Unit,
    private val onItemLongClick: (Expense) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    class ExpenseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val category: TextView = view.findViewById(R.id.tvExpenseCategory)
        val sub: TextView = view.findViewById(R.id.tvExpenseSub)
        val amount: TextView = view.findViewById(R.id.tvExpenseAmount)
        val date: TextView = view.findViewById(R.id.tvExpenseDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val item = expenses[position]
        holder.category.text = item.category
        holder.sub.text = item.subCategory
        holder.amount.text = String.format("₹%.2f", item.amountAsDecimal)
        
        // Format and set date
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        holder.date.text = sdf.format(Date.from(item.date))

        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.itemView.setOnLongClickListener {
            onItemLongClick(item)
            true
        }
    }

    override fun getItemCount() = expenses.size

    fun updateList(newList: List<Expense>) {
        expenses = newList
        notifyDataSetChanged()
    }
}
