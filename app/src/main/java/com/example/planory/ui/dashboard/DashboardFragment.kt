package com.example.planory.ui.dashboard

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Button
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.planory.R
import com.example.planory.data.AppDatabase
import com.example.planory.data.BudgetManager
import com.example.planory.data.ai.BudgetRecommendationEngine
import com.example.planory.data.entity.ExpenseEntity
import com.example.planory.ui.add.AddExpenseBottomSheet
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.snackbar.Snackbar
import java.util.Calendar

// ✅ ADD THIS IMPORT
import com.example.planory.data.firestore.FirestoreExpenseService

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private lateinit var tvEmpty: TextView
    private lateinit var rvExpenses: RecyclerView
    private lateinit var adapter: ExpenseAdapter

    private lateinit var cardRecommendation: View
    private lateinit var tvRecommendation: TextView

    private lateinit var tvBudgetAlert: TextView
    private lateinit var layoutBudgetProgress: View
    private lateinit var budgetProgressBar: ProgressBar
    private lateinit var tvBudgetProgressText: TextView
    private lateinit var layoutDateFilter: View

    private lateinit var etSearch: EditText
    private var allExpenses = listOf<ExpenseEntity>()

    // 🔹 DATE FILTER BUTTONS
    private lateinit var btnToday: Button
    private lateinit var btnWeek: Button
    private lateinit var btnMonth: Button

    // ✅ FIX: remember active filter
    private var activeFilter: String = "ALL"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvWelcome = view.findViewById<TextView>(R.id.tvWelcome)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        rvExpenses = view.findViewById(R.id.rvExpenses)

        cardRecommendation = view.findViewById(R.id.cardRecommendation)
        tvRecommendation = view.findViewById(R.id.tvRecommendation)

        tvBudgetAlert = view.findViewById(R.id.tvBudgetAlert)
        layoutBudgetProgress = view.findViewById(R.id.layoutBudgetProgress)
        budgetProgressBar = view.findViewById(R.id.budgetProgressBar)
        tvBudgetProgressText = view.findViewById(R.id.tvBudgetProgressText)

        etSearch = view.findViewById(R.id.etSearch)

        btnToday = view.findViewById(R.id.btnToday)
        btnWeek = view.findViewById(R.id.btnWeek)
        btnMonth = view.findViewById(R.id.btnMonth)
        layoutDateFilter = view.findViewById(R.id.layoutDateFilter)
        layoutDateFilter.visibility = View.VISIBLE

        val fab = view.findViewById<FloatingActionButton>(R.id.fabAdd)

        tvWelcome.text = "Welcome 👋"

        adapter = ExpenseAdapter(
            mutableListOf(),
            onEdit = { expense -> editExpense(expense) }
        )

        rvExpenses.layoutManager = LinearLayoutManager(requireContext())
        rvExpenses.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val deletedExpense = adapter.removeAt(position)

                lifecycleScope.launch {
                    AppDatabase.getDatabase(requireContext())
                        .expenseDao()
                        .deleteExpense(deletedExpense)
                }

                Snackbar.make(rvExpenses, "Expense deleted", Snackbar.LENGTH_LONG)
                    .setAction("UNDO") {
                        adapter.restoreAt(deletedExpense, position)
                        lifecycleScope.launch {
                            AppDatabase.getDatabase(requireContext())
                                .expenseDao()
                                .insertExpense(deletedExpense)
                        }
                    }
                    .show()
            }
        })

        itemTouchHelper.attachToRecyclerView(rvExpenses)

        parentFragmentManager.setFragmentResultListener(
            "expense_added",
            viewLifecycleOwner
        ) { _, _ ->
            loadExpenses()
        }

        fab.setOnClickListener {
            AddExpenseBottomSheet()
                .show(parentFragmentManager, "AddExpense")
        }

        // 🔍 SEARCH (UNCHANGED)
        etSearch.addTextChangedListener { text ->
            val query = text?.toString()?.trim().orEmpty()
            val filtered = if (query.isEmpty()) allExpenses else {
                allExpenses.filter {
                    it.category.contains(query, true) ||
                            it.note?.contains(query, true) == true
                }
            }
            adapter.updateData(filtered)
        }

        // ✅ DATE FILTER BUTTONS (UNCHANGED)
        btnToday.setOnClickListener {
            activeFilter = "TODAY"
            filterExpenses("TODAY")
        }

        btnWeek.setOnClickListener {
            activeFilter = "WEEK"
            filterExpenses("WEEK")
        }

        btnMonth.setOnClickListener {
            activeFilter = "MONTH"
            filterExpenses("MONTH")
        }

        // ✅ EXISTING CALL
        loadExpenses()

        // ✅ ADD THIS — download once on app launch
        syncFromFirestore()
    }

    private fun loadExpenses() {
        val dao = AppDatabase.getDatabase(requireContext()).expenseDao()

        viewLifecycleOwner.lifecycleScope.launch {
            val user = FirebaseAuth.getInstance().currentUser ?: return@launch
            val expenses = dao.getExpensesForUser(user.uid)

            allExpenses = expenses

            if (expenses.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                rvExpenses.visibility = View.GONE
                cardRecommendation.visibility = View.GONE
                adapter.updateData(emptyList())
                return@launch
            }

            tvEmpty.visibility = View.GONE
            rvExpenses.visibility = View.VISIBLE

            when (activeFilter) {
                "TODAY" -> filterExpenses("TODAY")
                "WEEK" -> filterExpenses("WEEK")
                "MONTH" -> filterExpenses("MONTH")
                else -> adapter.updateData(allExpenses)
            }

            val recommendation = BudgetRecommendationEngine.generate(expenses)
            if (recommendation != null) {
                tvRecommendation.text = recommendation
                cardRecommendation.visibility = View.VISIBLE
            } else {
                cardRecommendation.visibility = View.GONE
            }

            val budgetManager = BudgetManager(requireContext())
            val budget = budgetManager.getBudget()

            if (budget > 0) {
                val totalSpent = expenses.sumOf { it.amount }
                val percentUsed = ((totalSpent / budget.toDouble()) * 100).toInt()

                layoutBudgetProgress.visibility = View.VISIBLE
                budgetProgressBar.progress = percentUsed.coerceAtMost(100)
                tvBudgetProgressText.text =
                    "₹$totalSpent of ₹$budget used ($percentUsed%)"

                when {
                    percentUsed >= 100 -> {
                        budgetProgressBar.progressTintList =
                            ColorStateList.valueOf(Color.parseColor("#D32F2F"))
                        tvBudgetAlert.visibility = View.VISIBLE
                        tvBudgetAlert.text = "🚨 You exceeded your monthly budget!"
                    }
                    percentUsed >= 80 -> {
                        budgetProgressBar.progressTintList =
                            ColorStateList.valueOf(Color.parseColor("#F57C00"))
                        tvBudgetAlert.visibility = View.VISIBLE
                        tvBudgetAlert.text = "⚠️ You’ve used 80% of your budget"
                    }
                    else -> {
                        budgetProgressBar.progressTintList =
                            ColorStateList.valueOf(Color.parseColor("#2E7D32"))
                        tvBudgetAlert.visibility = View.GONE
                    }
                }
            } else {
                layoutBudgetProgress.visibility = View.GONE
                tvBudgetAlert.visibility = View.GONE
            }
        }
    }

    // ✅ ADD THIS FUNCTION (class level, untouched logic)
    private fun syncFromFirestore() {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        FirestoreExpenseService.fetchExpenses(user.uid) { cloudExpenses ->
            if (cloudExpenses.isEmpty()) return@fetchExpenses

            val dao = AppDatabase.getDatabase(requireContext()).expenseDao()

            lifecycleScope.launch {
                cloudExpenses.forEach {
                    dao.insertExpense(it) // last-write wins
                }
                loadExpenses()
            }
        }
    }

    // ✅ DATE FILTER (UNCHANGED)
    private fun filterExpenses(range: String) {
        val now = Calendar.getInstance()

        val filtered = allExpenses.filter { expense ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = expense.timestamp
            }

            when (range) {
                "TODAY" ->
                    cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) &&
                            cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)

                "WEEK" ->
                    cal.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR) &&
                            cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)

                "MONTH" ->
                    cal.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                            cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)

                else -> true
            }
        }

        if (filtered.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvExpenses.visibility = View.GONE
            tvEmpty.text = "No expenses for this period"
        } else {
            tvEmpty.visibility = View.GONE
            rvExpenses.visibility = View.VISIBLE
            adapter.updateData(filtered)
        }
    }

    private fun editExpense(expense: ExpenseEntity) {
        AddExpenseBottomSheet.newInstance(expense.id)
            .show(parentFragmentManager, "EditExpense")
    }
}
