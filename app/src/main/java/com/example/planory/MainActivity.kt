package com.example.planory

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.planory.data.worker.BudgetCheckWorker
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.concurrent.TimeUnit
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        // 🌙 APPLY SAVED THEME FIRST
        val prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)

        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode)
                AppCompatDelegate.MODE_NIGHT_YES
            else
                AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 🔹 Navigation setup
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                    as? NavHostFragment ?: return

        val navController = navHostFragment.navController
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        bottomNav.setupWithNavController(navController)

        // 🔹 Show BottomNav ONLY on main screens
        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomNav.visibility = when (destination.id) {
                R.id.dashboardFragment,
                R.id.analyticsFragment,
                R.id.profileFragment,
                R.id.goalsFragment -> View.VISIBLE
                else -> View.GONE
            }
        }

        // 🔔 Budget Check Worker
        val workRequest =
            PeriodicWorkRequestBuilder<BudgetCheckWorker>(
                1,
                TimeUnit.DAYS
            ).build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "budget_check",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
    }
}
