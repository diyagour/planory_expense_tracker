package com.example.planory.ui.goals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.planory.R
import com.example.planory.data.entity.GoalEntity

class GoalsAdapter(
    private val goals: MutableList<GoalEntity>
) : RecyclerView.Adapter<GoalsAdapter.GoalViewHolder>() {

    inner class GoalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvGoalTitle)
        val progress: TextView = view.findViewById(R.id.tvGoalProgress)
        val remaining: TextView = view.findViewById(R.id.tvRemaining)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_goal, parent, false)
        return GoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val goal = goals[position]

        holder.title.text = goal.title
        holder.progress.text = "Saved ₹${goal.savedAmount} / ₹${goal.totalAmount}"
        holder.remaining.text = "Remaining ₹${goal.remainingAmount}"
    }

    override fun getItemCount(): Int = goals.size

    fun updateData(newGoals: List<GoalEntity>) {
        goals.clear()
        goals.addAll(newGoals)
        notifyDataSetChanged()
    }
}
