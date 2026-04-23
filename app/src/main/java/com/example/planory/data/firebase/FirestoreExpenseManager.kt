package com.example.planory.data.firebase

import com.example.planory.data.entity.ExpenseEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirestoreExpenseManager {

    private val db = FirebaseFirestore.getInstance()

    fun uploadExpense(expense: ExpenseEntity) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("expenses")
            .document("${userId}_${expense.id}")
            .set(expense)
    }

    fun downloadExpenses(
        onResult: (List<ExpenseEntity>) -> Unit
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("expenses")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.toObjects(ExpenseEntity::class.java)
                onResult(list)
            }
    }
}