package com.example.planory.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.planory.R
import com.example.planory.data.entity.ExpenseEntity
import com.example.planory.databinding.ItemExpenseBinding
import com.google.android.material.card.MaterialCardView

class ExpenseAdapter(
    private val expenses: MutableList<ExpenseEntity>,
    private val onEdit: (ExpenseEntity) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    private val colors = listOf(
        R.color.pastel_yellow,
        R.color.pastel_green,
        R.color.pastel_blue,
        R.color.pastel_pink,
        R.color.pastel_purple,
        R.color.pastel_orange,
        R.color.pastel_teal,
        R.color.pastel_lavender,
        R.color.pastel_peach,
        R.color.pastel_mint
    )

    inner class ExpenseViewHolder(
        val binding: ItemExpenseBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenses[position]

        // 🎯 DATA BINDING
        holder.binding.expense = expense
        holder.binding.executePendingBindings()

        // 🎨 Pastel background (unchanged behavior)
        (holder.binding.root as MaterialCardView).setCardBackgroundColor(
            holder.binding.root.context.getColor(
                colors[position % colors.size]
            )
        )

        // 👉 TAP = EDIT
        holder.itemView.setOnClickListener {
            onEdit(expense)
        }
    }

    override fun getItemCount(): Int = expenses.size

    fun updateData(newExpenses: List<ExpenseEntity>) {
        expenses.clear()
        expenses.addAll(newExpenses)
        notifyDataSetChanged()
    }

    // swipe helpers (unchanged)
    fun removeAt(position: Int): ExpenseEntity {
        val removed = expenses[position]
        expenses.removeAt(position)
        notifyItemRemoved(position)
        return removed
    }

    fun restoreAt(expense: ExpenseEntity, position: Int) {
        expenses.add(position, expense)
        notifyItemInserted(position)
    }
}
