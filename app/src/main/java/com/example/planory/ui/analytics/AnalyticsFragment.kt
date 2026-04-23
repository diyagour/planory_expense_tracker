package com.example.planory.ui.analytics

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.planory.R
import com.example.planory.data.AppDatabase
import com.example.planory.data.entity.ExpenseEntity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Calendar
import com.github.mikephil.charting.utils.ColorTemplate
import android.os.Environment
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Bitmap
import androidx.core.content.ContextCompat

class AnalyticsFragment : Fragment(R.layout.fragment_analytics) {
    private val chartColors = listOf(
        R.color.pastel_yellow,        // Hospital
        R.color.pastel_green,         // Medicines
        R.color.pastel_blue_dark,     // Food (darker)
        R.color.pastel_beige_dark,    // Rent (darker)
        R.color.pastel_purple,
        R.color.pastel_orange,
        R.color.pastel_teal,
        R.color.pastel_lavender,
        R.color.pastel_peach,
        R.color.pastel_mint
    )
    private fun exportPDF() {

        val pdfDocument = PdfDocument()
        val paint = Paint()

        val pageInfo = PdfDocument.PageInfo.Builder(1200, 2400, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // 🔹 Title
        paint.textSize = 50f
        paint.isFakeBoldText = true
        canvas.drawText("Planory Analytics Report", 200f, 150f, paint)

        // 🔹 Summary
        paint.textSize = 35f
        paint.isFakeBoldText = false
        canvas.drawText("Total Spent: ${tvTotalSpent.text}", 150f, 250f, paint)
        canvas.drawText("Transactions: ${tvTransactions.text}", 150f, 320f, paint)

        // 🔹 PIE CHART
        val pieBitmap = pieChart.chartBitmap
        val scaledPie = Bitmap.createScaledBitmap(
            pieBitmap,
            900,
            900,
            true
        )

        canvas.drawBitmap(scaledPie, 150f, 400f, null)

        // 🔹 BAR CHART
        val barBitmap = barChart.chartBitmap
        val scaledBar = Bitmap.createScaledBitmap(
            barBitmap,
            1000,
            700,
            true
        )

        canvas.drawBitmap(scaledBar, 100f, 1400f, null)

        pdfDocument.finishPage(page)

        val fileName = "Planory_Analytics_Report.pdf"
        val resolver = requireContext().contentResolver

        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(
                android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
                android.os.Environment.DIRECTORY_DOWNLOADS
            )
        }

        val uri = resolver.insert(
            android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            contentValues
        )

        if (uri != null) {
            resolver.openOutputStream(uri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }

            Toast.makeText(
                requireContext(),
                "PDF saved to Downloads",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                requireContext(),
                "Error saving PDF",
                Toast.LENGTH_LONG
            ).show()
        }

        pdfDocument.close()
    }

    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var tvTotalSpent: TextView
    private lateinit var tvTransactions: TextView
    private lateinit var layoutInsights: LinearLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pieChart = view.findViewById(R.id.pieChart)
        barChart = view.findViewById(R.id.barChart)
        tvTotalSpent = view.findViewById(R.id.tvTotalSpent)
        tvTransactions = view.findViewById(R.id.tvTransactions)
        layoutInsights = view.findViewById(R.id.layoutInsights)

        setupPieChart()
        setupBarChart()
        loadAnalytics()
        val toolbar = view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.statsToolbar)

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_export -> {
                    exportPDF()
                    true
                }
                else -> false
            }
        }

    }

    private fun setupPieChart() {
        pieChart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 60f
            setEntryLabelColor(Color.BLACK)
            centerText = "Spending"
            legend.isEnabled = true
        }
    }

    private fun setupBarChart() {
        barChart.apply {
            description.isEnabled = false
            axisRight.isEnabled = false
            axisLeft.axisMinimum = 0f
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            animateY(800)
        }
    }

    private fun loadAnalytics() {
        val dao = AppDatabase.getDatabase(requireContext()).expenseDao()

        lifecycleScope.launch {
            val user = FirebaseAuth.getInstance().currentUser ?: return@launch

            val expenses = dao.getExpensesForUser(user.uid)

            // Totals
            tvTotalSpent.text = "₹ ${expenses.sumOf { it.amount }}"
            tvTransactions.text = expenses.size.toString()

            // Monthly comparison
            val currentMonthTotal = dao.getCurrentMonthTotal(user.uid)
            val lastMonthTotal = dao.getLastMonthTotal(user.uid)

            val monthlyInsight = generateMonthlySavingsInsight(
                currentMonthTotal,
                lastMonthTotal
            )

            // Generate insights
            val insights = generateInsights(expenses).toMutableList()
            insights.add(0, monthlyInsight)

            // 🔥 ADD BUDGET RECOMMENDATION BACK
            val recommendation =
                com.example.planory.data.ai.BudgetRecommendationEngine.generate(expenses)

            recommendation?.let {
                insights.add(1, it)
            }

            layoutInsights.removeAllViews()
            insights.forEach {
                val tv = TextView(requireContext())
                tv.text = it
                tv.setPadding(0, 8, 0, 8)
                tv.setTextColor(resources.getColor(R.color.text_secondary))
                layoutInsights.addView(tv)
            }

            // Charts
            loadPieChart(expenses)
            loadWeeklyBarChart(expenses)
        }
    }


    private fun loadPieChart(expenses: List<ExpenseEntity>) {
        val entries = expenses.groupBy { it.category }
            .map { PieEntry(it.value.sumOf { e -> e.amount }.toFloat(), it.key) }

        val dataSet = PieDataSet(entries, "")

        dataSet.colors = chartColors.map {
            ContextCompat.getColor(requireContext(), it)
        }

        dataSet.valueTextColor = Color.BLACK

        pieChart.data = PieData(dataSet)
        pieChart.invalidate()
    }

    // 🟢 WEEKLY BREAKDOWN
    private fun loadWeeklyBarChart(expenses: List<ExpenseEntity>) {
        val totals = FloatArray(7)

        expenses.forEach {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.timestamp
            val day = cal.get(Calendar.DAY_OF_WEEK) - 1
            totals[day] += it.amount.toFloat()
        }

        val entries = totals.mapIndexed { index, value ->
            BarEntry(index.toFloat(), value)
        }

        val dataSet = BarDataSet(entries, "Weekly Spend").apply {
            color = Color.parseColor("#FFB703")
            valueTextSize = 10f
        }

        barChart.xAxis.valueFormatter =
            IndexAxisValueFormatter(listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat"))

        barChart.data = BarData(dataSet)
        barChart.invalidate()
    }

    private fun generateInsights(expenses: List<ExpenseEntity>): List<String> {
        val insights = mutableListOf<String>()

        val topCategory = expenses.groupBy { it.category }
            .maxByOrNull { it.value.sumOf { e -> e.amount } }?.key

        topCategory?.let { insights.add("Highest spending on $it") }

        return insights
    }

    // 🔹 STEP B — Monthly savings insight (ADDED)
    private fun generateMonthlySavingsInsight(
        currentMonthTotal: Double,
        lastMonthTotal: Double
    ): String {
        return when {
            currentMonthTotal < lastMonthTotal -> {
                val saved = lastMonthTotal - currentMonthTotal
                "🎉 You saved ₹${saved.toInt()} compared to last month!"
            }
            currentMonthTotal > lastMonthTotal -> {
                val extra = currentMonthTotal - lastMonthTotal
                "⚠️ You spent ₹${extra.toInt()} more than last month."
            }
            else -> {
                "Your spending is the same as last month."
            }
        }
    }
}
