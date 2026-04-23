package com.example.planory.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val totalAmount: Int,
    val savedAmount: Int,
    val userId: String

) {
    val remainingAmount: Int
        get() = totalAmount - savedAmount
}
