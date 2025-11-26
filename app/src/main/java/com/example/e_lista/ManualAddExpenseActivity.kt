package com.example.e_lista

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.e_lista.databinding.ActivityChartDesign10Binding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ChartDesign10Activity : AppCompatActivity() {

    private lateinit var binding: ActivityChartDesign10Binding

    // Firebase
    private lateinit var mAuth: FirebaseAuth
    private lateinit var userID: String
    private lateinit var expenseDatabase: DatabaseReference

    // Charts
    private lateinit var pieChart: PieChart
    private lateinit var lineChart: LineChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Firebase init
        mAuth = FirebaseAuth.getInstance()
        userID = mAuth.currentUser?.uid ?: "UnknownUser"
        expenseDatabase = FirebaseDatabase.getInstance()
            .getReference("ExpenseData")
            .child(userID)

        binding = ActivityChartDesign10Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // Keep navigation highlighting
        binding.bottomNavigationView.selectedItemId = R.id.nav_stats

        // Hook charts (these IDs must match the XML above)
        pieChart = binding.pieChart
        lineChart = binding.lineChart

        setupPieChartStyle(pieChart)
        setupLineChartStyle(lineChart)

        // FAB -> camera
        binding.fabCamera.setOnClickListener {
            val intent = Intent(this, ReceiptScanUpload::class.java)
            startActivity(intent)
        }

        // Bottom nav unchanged
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    if (this !is Home9Activity) {
                        startActivity(Intent(this, Home9Activity::class.java))
                        overridePendingTransition(0, 0)
                        finish()
                    }
                    true
                }

                R.id.nav_wallet -> {
                    if (this !is Expenses12Activity) {
                        startActivity(Intent(this, Expenses12Activity::class.java))
                        overridePendingTransition(0, 0)
                        finish()
                    }
                    true
                }

                R.id.nav_camera_placeholder -> {
                    if (this !is ReceiptScanUpload) {
                        startActivity(Intent(this, ReceiptScanUpload::class.java))
                        overridePendingTransition(0, 0)
                        finish()
                    }
                    true
                }

                R.id.nav_stats -> {
                    true
                }

                R.id.nav_profile -> {
                    if (this !is Profile13Activity) {
                        startActivity(Intent(this, Profile13Activity::class.java))
                        overridePendingTransition(0, 0)
                        finish()
                    }
                    true
                }

                else -> false
            }
        }

        // Load data from Firebase and update charts
        fetchExpensesAndRenderCharts()
    }

    private fun fetchExpensesAndRenderCharts() {
        // Listen once (load current snapshot)
        expenseDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                // Maps for aggregations
                val categoryTotals = HashMap<String, Double>()
                val dailyTotals = TreeMap<Long, Double>() // keyed by dayStartMillis (sorted)

                for (child in snapshot.children) {
                    val expense = child.getValue(Expense::class.java) ?: continue

                    // compute expense total from items, fallback to expense.total if present
                    val total = if (expense.items.isNotEmpty()) {
                        expense.items.sumOf { it.itemAmount }
                    } else {
                        // if items missing, maybe there is a total property - but user's model computes total property,
                        // so we keep 0 fallback
                        0.0
                    }

                    // Aggregate category totals (use category string; handle empties)
                    val categoryName = if (expense.category.isNullOrBlank()) "Others" else expense.category
                    categoryTotals[categoryName] = categoryTotals.getOrDefault(categoryName, 0.0) + total

                    // Aggregate per day using timestamp
                    val ts = if (expense.timestamp > 0L) expense.timestamp else 0L
                    if (ts > 0L) {
                        // Normalize to day start (midnight) in device timezone
                        val dayStart = getDayStartMillis(ts)
                        dailyTotals[dayStart] = dailyTotals.getOrDefault(dayStart, 0.0) + total
                    }
                }

                // Update the PieChart and LineChart on UI thread (we are already on main thread)
                updatePieChartWithData(pieChart, categoryTotals)
                updateLineChartWithData(lineChart, dailyTotals)
            }

            override fun onCancelled(error: DatabaseError) {
                // handle error (optional)
            }
        })
    }

    // Helper — convert timestamp millis to day-start (local timezone)
    private fun getDayStartMillis(tsMillis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = tsMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun setupPieChartStyle(pieChart: PieChart) {
        pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            setDrawHoleEnabled(true)
            holeRadius = 35f
            transparentCircleRadius = 40f
            centerText = "Spending"
            setCenterTextSize(18f)
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(12f)

            legend.apply {
                orientation = Legend.LegendOrientation.HORIZONTAL
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                textSize = 12f
                form = Legend.LegendForm.CIRCLE
            }
        }
    }

    private fun setupLineChartStyle(lineChart: LineChart) {
        lineChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            axisRight.isEnabled = false

            // x-axis formatting will be done when data is available using labels
            xAxis.apply {
                granularity = 1f
                setDrawGridLines(false)
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            }

            axisLeft.apply {
                setDrawGridLines(true)
            }

            legend.isEnabled = false
        }
    }

    private fun updatePieChartWithData(pieChart: PieChart, categoryTotals: Map<String, Double>) {
        val entries = ArrayList<PieEntry>()
        var totalSum = 0.0
        for ((category, total) in categoryTotals) {
            if (total > 0.0) {
                entries.add(PieEntry(total.toFloat(), category))
                totalSum += total
            }
        }

        if (entries.isEmpty()) {
            pieChart.clear()
            pieChart.centerText = "No data"
            pieChart.invalidate()
            return
        }

        val dataSet = PieDataSet(entries, "Expense Breakdown").apply {
            colors = listOf(
                Color.parseColor("#16a085"),
                Color.parseColor("#27ae60"),
                Color.parseColor("#2ecc71"),
                Color.parseColor("#1abc9c"),
                Color.parseColor("#f39c12"),
                Color.parseColor("#e74c3c"),
                Color.parseColor("#9b59b6")
            )
            sliceSpace = 3f
            valueTextColor = Color.WHITE
            valueTextSize = 12f
        }

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.invalidate()
        pieChart.animateY(900)
    }

    private fun updateLineChartWithData(lineChart: LineChart, dailyTotals: SortedMap<Long, Double>) {
        if (dailyTotals.isEmpty()) {
            lineChart.clear()
            lineChart.invalidate()
            return
        }

        // Build xLabels list (String) and entries (index-based)
        val xLabels = ArrayList<String>()
        val entries = ArrayList<Entry>()

        val sdf = SimpleDateFormat("MMM d", Locale.getDefault()) // label format e.g. "Jan 3"
        var idx = 0f
        for ((dayStart, total) in dailyTotals) {
            xLabels.add(sdf.format(Date(dayStart)))
            entries.add(Entry(idx, total.toFloat()))
            idx += 1f
        }

        val dataSet = LineDataSet(entries, "Daily Spending").apply {
            lineWidth = 2.5f
            circleRadius = 4f
            setDrawCircleHole(true)
            setDrawValues(true)
            valueTextSize = 10f
            valueTextColor = Color.BLACK
            mode = LineDataSet.Mode.LINEAR
            setDrawFilled(false)
            color = Color.parseColor("#c0392b") // line color
            setCircleColor(Color.parseColor("#c0392b"))
        }

        val lineData = LineData(dataSet)
        lineChart.data = lineData

        // Set x-axis labels
        lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
        lineChart.xAxis.labelRotationAngle = 0f
        lineChart.xAxis.setLabelCount(xLabels.size, true)

        lineChart.axisLeft.resetAxisMinimum()
        lineChart.invalidate()
        lineChart.animateY(900)
    }
}
