package com.example.planory.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String,
    val amount: Double,
    val category: String,
    val note: String?,
    val timestamp: Long
){
    // 🔥 REQUIRED by Firestore
    constructor() : this(0, "", 0.0, "", null, 0L)
}
