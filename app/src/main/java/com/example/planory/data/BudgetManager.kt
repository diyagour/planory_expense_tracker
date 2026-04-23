package com.example.planory.data

import android.content.Context

class BudgetManager(context: Context) {

    private val prefs = context.getSharedPreferences("budget_prefs", Context.MODE_PRIVATE)

    fun saveBudget(budget: Int) {
        prefs.edit().putInt("budget", budget).apply()
    }

    fun getBudget(): Int {
        return prefs.getInt("budget", 0)
    }
}
