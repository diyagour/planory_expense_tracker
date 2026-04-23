package com.example.planory.ui.profile

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.planory.R
import com.example.planory.data.AppDatabase
import com.example.planory.data.BudgetManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvUsername = view.findViewById<TextView>(R.id.tvUsername)
        val tvEmail = view.findViewById<TextView>(R.id.tvEmail)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        val etBudget = view.findViewById<EditText>(R.id.etBudget)
        val btnSaveBudget = view.findViewById<Button>(R.id.btnSaveBudget)
        val tvBudget = view.findViewById<TextView>(R.id.tvBudget)

        val tvTotalSpent = view.findViewById<TextView>(R.id.tvTotalSpent)
        val tvTransactions = view.findViewById<TextView>(R.id.tvTransactions)
        val tvTopCategory = view.findViewById<TextView>(R.id.tvTopCategory)

        // 🌙 DARK MODE SWITCH
        val switchDarkMode = view.findViewById<Switch>(R.id.switchDarkMode)

        val prefs = requireContext()
            .getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

        val isDarkMode = prefs.getBoolean("dark_mode", false)
        switchDarkMode.isChecked = isDarkMode

        switchDarkMode.setOnCheckedChangeListener { _, enabled ->
            prefs.edit().putBoolean("dark_mode", enabled).apply()

            AppCompatDelegate.setDefaultNightMode(
                if (enabled)
                    AppCompatDelegate.MODE_NIGHT_YES
                else
                    AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            findNavController().navigate(R.id.loginFragment)
            return
        }

        tvUsername.text = user.displayName ?: "User"
        tvEmail.text = user.email

        val budgetManager = BudgetManager(requireContext())
        val savedBudget = budgetManager.getBudget()
        tvBudget.text = if (savedBudget > 0) "₹ $savedBudget" else "No budget set"

        btnSaveBudget.setOnClickListener {
            val budget = etBudget.text.toString().toIntOrNull()
            if (budget != null && budget > 0) {
                budgetManager.saveBudget(budget)
                tvBudget.text = "₹ $budget"
                etBudget.text.clear()
            }
        }

        val dao = AppDatabase.getDatabase(requireContext()).expenseDao()

        lifecycleScope.launch {
            val expenses = dao.getExpensesForUser(user.uid)

            if (expenses.isEmpty()) {
                tvTotalSpent.text = "₹ 0"
                tvTransactions.text = "0"
                tvTopCategory.text = "None"
                return@launch
            }

            tvTotalSpent.text = "₹ ${expenses.sumOf { it.amount }}"
            tvTransactions.text = expenses.size.toString()

            val topCategory = expenses
                .groupBy { it.category }
                .maxByOrNull { it.value.size }
                ?.key ?: "None"

            tvTopCategory.text = topCategory
        }

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            findNavController().navigate(R.id.loginFragment)
        }
    }
}
