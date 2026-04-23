package com.example.planory.data.ai

import com.example.planory.data.entity.ExpenseEntity

object BudgetRecommendationEngine {

    fun generate(expenses: List<ExpenseEntity>): String? {
        if (expenses.isEmpty()) return null

        val totalSpent = expenses.sumOf { it.amount }

        val categoryWise = expenses.groupBy { it.category }
            .mapValues { it.value.sumOf { expense -> expense.amount } }

        val topCategory = categoryWise.maxByOrNull { it.value } ?: return null

        val categoryName = topCategory.key
        val categoryAmount = topCategory.value

        return when {
            categoryAmount > totalSpent * 0.4 -> {
                "You spent ₹${categoryAmount.toInt()} on $categoryName. " +
                        "Reducing it by 20% could save ₹${(categoryAmount * 0.2).toInt()}."
            }

            totalSpent > 50000 -> {
                "Your total spending is high this month (₹${totalSpent.toInt()}). " +
                        "Try setting weekly limits to control expenses."
            }

            else -> {
                "Good job managing your expenses! Keep tracking to save more."
            }
        }
    }
}
