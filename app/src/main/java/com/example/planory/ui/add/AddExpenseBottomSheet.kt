package com.example.planory.ui.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.planory.R
import com.example.planory.data.AppDatabase
import com.example.planory.data.entity.ExpenseEntity
import com.example.planory.data.worker.BudgetCheckWorker
import com.example.planory.data.firestore.FirestoreExpenseService   // ✅ ADD
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class AddExpenseBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottomsheet_add_expense, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etAmount = view.findViewById<EditText>(R.id.etAmount)
        val etCategory = view.findViewById<EditText>(R.id.etCategory)
        val etNote = view.findViewById<EditText>(R.id.etNote)
        val btnSave = view.findViewById<Button>(R.id.btnSaveExpense)

        val dao = AppDatabase.getDatabase(requireContext()).expenseDao()

        // ✅ LOAD EXPENSE BY ID (EDIT MODE)
        val expenseId = arguments?.getInt("expense_id", -1) ?: -1
        var existingExpense: ExpenseEntity? = null

        if (expenseId != -1) {
            lifecycleScope.launch {
                existingExpense = dao.getExpenseById(expenseId)

                existingExpense?.let {
                    etAmount.setText(it.amount.toString())
                    etCategory.setText(it.category)
                    etNote.setText(it.note ?: "")
                }
            }
        }

        btnSave.setOnClickListener {

            val amount = etAmount.text.toString().toDoubleOrNull()
            val category = etCategory.text.toString().trim()
            val note = etNote.text.toString().trim()

            if (amount == null || category.isEmpty()) {
                Toast.makeText(requireContext(), "Enter valid data", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {

                val expense = ExpenseEntity(
                    id = existingExpense?.id ?: 0, // 👈 add OR edit
                    userId = user.uid,
                    amount = amount,
                    category = category,
                    note = note,
                    timestamp = System.currentTimeMillis()
                )

                if (existingExpense == null) {
                    // ✅ ADD
                    dao.insertExpense(expense)
                } else {
                    // ✅ EDIT
                    dao.updateExpense(expense)
                }

                // 🔥 ONE-LINE CLOUD SYNC
                //FirestoreExpenseService.uploadExpense(user.uid, expense)

                parentFragmentManager.setFragmentResult(
                    "expense_added",
                    Bundle()
                )

                dismiss()
            }

            // ✅ Budget check (unchanged)
            WorkManager.getInstance(requireContext())
                .enqueue(
                    OneTimeWorkRequestBuilder<BudgetCheckWorker>().build()
                )
        }
    }

    companion object {
        fun newInstance(expenseId: Int): AddExpenseBottomSheet {
            return AddExpenseBottomSheet().apply {
                arguments = Bundle().apply {
                    putInt("expense_id", expenseId)
                }
            }
        }
    }
}
