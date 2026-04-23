package com.example.planory.data.firestore

import com.example.planory.data.entity.ExpenseEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object FirestoreExpenseService {

    private val db = FirebaseFirestore.getInstance()

    fun uploadExpense(userId: String, expense: ExpenseEntity) {
        db.collection("users")
            .document(userId)
            .collection("expenses")
            .document(expense.id.toString())
            .set(expense, SetOptions.merge())
    }

    fun fetchExpenses(
        userId: String,
        onResult: (List<ExpenseEntity>) -> Unit
    ) {
        db.collection("users")
            .document(userId)
            .collection("expenses")
            .get()
            .addOnSuccessListener { snapshot ->
                val expenses = snapshot.toObjects(ExpenseEntity::class.java)
                onResult(expenses)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }
}
