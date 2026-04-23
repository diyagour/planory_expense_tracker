package com.example.planory.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update   // ✅ ADD
import com.example.planory.data.entity.ExpenseEntity

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    // ✅ ADD THIS
    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Query(
        "SELECT * FROM expenses WHERE userId = :userId ORDER BY timestamp DESC"
    )
    suspend fun getExpensesForUser(userId: String): List<ExpenseEntity>

    @Query("DELETE FROM expenses")
    suspend fun clearAll()

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: Int): ExpenseEntity?

    @Query("""
        SELECT IFNULL(SUM(amount), 0)
        FROM expenses
        WHERE userId = :userId
        AND strftime('%m', timestamp / 1000, 'unixepoch') = strftime('%m', 'now')
    """)
    suspend fun getCurrentMonthTotal(userId: String): Double

    @Query("""
        SELECT IFNULL(SUM(amount), 0)
        FROM expenses
        WHERE userId = :userId
        AND strftime('%Y-%m', timestamp / 1000, 'unixepoch')
            = strftime('%Y-%m', 'now', '-1 month')
    """)
    suspend fun getLastMonthTotal(userId: String): Double

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)
}
