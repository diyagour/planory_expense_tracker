package com.example.planory.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.planory.R
import com.example.planory.data.AppDatabase
import com.example.planory.data.BudgetManager
import com.google.firebase.auth.FirebaseAuth

class BudgetCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {

        val user = FirebaseAuth.getInstance().currentUser ?: return Result.success()

        val budgetManager = BudgetManager(applicationContext)
        val monthlyBudget = budgetManager.getBudget()

        if (monthlyBudget <= 0) return Result.success()

        val dao = AppDatabase.getDatabase(applicationContext).expenseDao()
        val totalSpent = dao.getCurrentMonthTotal(user.uid)

        when {
            totalSpent >= monthlyBudget -> {
                showNotification(
                    "Budget Exceeded 🚨",
                    "You have exceeded your monthly budget of ₹$monthlyBudget"
                )
            }

            totalSpent >= monthlyBudget * 0.8 -> {
                showNotification(
                    "Budget Warning ⚠️",
                    "You’ve used 80% of your monthly budget"
                )
            }
        }

        return Result.success()
    }

    private fun showNotification(title: String, message: String) {

        val channelId = "budget_alerts"
        val manager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .build()

        manager.notify(1, notification)
    }
}
