package com.example.e_lista

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.e_lista.databinding.ActivityChartDesign10Binding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class ChartDesign10Activity : AppCompatActivity() {

    private lateinit var binding: ActivityChartDesign10Binding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var userID: String
    private lateinit var expenseDatabase: DatabaseReference

    private val categories = listOf("Food", "Transport", "Bills", "Shopping", "Entertainment", "Others")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChartDesign10Binding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = FirebaseAuth.getInstance()
        userID = mAuth.currentUser?.uid ?: "UnknownUser"
        expenseDatabase = FirebaseDatabase.getInstance().getReference("ExpenseData").child(userID)

        binding.bottomNavigationView.selectedItemId = R.id.nav_stats
        binding.fabCamera.setOnClickListener { startActivity(Intent(this, ReceiptScanUpload::class.java)) }
        setupBottomNavigation()

        binding.topSpendingList.layoutManager = LinearLayoutManager(this)

        binding.filterGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                R.id.chipWeek -> fetchAndDisplayData("WEEK")
                R.id.chipMonth -> fetchAndDisplayData("MONTH")
                R.id.chipYear -> fetchAndDisplayData("YEAR")
            }
        }

        binding.filterGroup.check(R.id.chipWeek)

    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { startActivity(Intent(this, Home9Activity::class.java)); finish(); overridePendingTransition(0, 0); true  }
                R.id.nav_wallet -> { startActivity(Intent(this, Expenses12Activity::class.java)); finish(); overridePendingTransition(0, 0); true }
                R.id.nav_camera_placeholder -> { startActivity(Intent(this, ReceiptScanUpload::class.java)); finish(); overridePendingTransition(0, 0); true }
                R.id.nav_stats -> true
                R.id.nav_profile -> { startActivity(Intent(this, Profile13Activity::class.java)); finish(); overridePendingTransition(0, 0); true }
                else -> false
            }
        }
    }

    private fun fetchAndDisplayData(period: String) {
        expenseDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val expenses = snapshot.children.mapNotNull { it.getValue(Expense::class.java) }
                val filtered = filterExpensesByPeriod(expenses, period)

                val categoryTotals = calculateCategoryTotals(filtered)
                val lineTotals = calculateLineTotals(filtered, period)
                val topCategories = calculateTopSpending(filtered)

                setupPieChart(binding.pieChart, categoryTotals)
                setupLineChart(binding.lineChart, lineTotals, period)

                if (topCategories.isEmpty()) {
                    // show placeholder if no top spending
                    binding.topSpendingList.adapter = TopSpendingAdapter(
                        listOf(
                            TopSpendingItem(
                                title = "No Data",
                                amount = 0.0,
                                date = "",
                                category = "Misc"
                            )
                        )
                    )
                } else {
                    binding.topSpendingList.adapter = TopSpendingAdapter(topCategories)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun filterExpensesByPeriod(expenses: List<Expense>, period: String): List<Expense> {
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis

        return expenses.filter { exp ->
            val ts = exp.timestamp
            val expCal = parseDateSafe(exp.date) ?: return@filter false

            when (period) {
                "WEEK" -> isSameWeek(exp.date, Calendar.getInstance())
                "MONTH" -> isSameMonth(exp.date, Calendar.getInstance())
                "YEAR" -> expCal.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR)
                else -> false
            }
        }
    }

    private fun parseDateSafe(dateStr: String): Calendar? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateStr) ?: return null
            Calendar.getInstance().apply { time = date }
        } catch (e: Exception) { null }
    }

    private fun isSameDay(dateStr: String, reference: Calendar): Boolean {
        val cal = parseDateSafe(dateStr) ?: return false
        return cal.get(Calendar.YEAR) == reference.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == reference.get(Calendar.DAY_OF_YEAR)
    }

    private fun isSameWeek(dateStr: String, reference: Calendar): Boolean {
        val cal = parseDateSafe(dateStr) ?: return false
        val weekStart = reference.clone() as Calendar
        weekStart.set(Calendar.DAY_OF_WEEK, weekStart.firstDayOfWeek)
        weekStart.set(Calendar.HOUR_OF_DAY, 0); weekStart.set(Calendar.MINUTE, 0)
        weekStart.set(Calendar.SECOND, 0); weekStart.set(Calendar.MILLISECOND, 0)

        val weekEnd = weekStart.clone() as Calendar
        weekEnd.add(Calendar.DAY_OF_WEEK, 6)
        weekEnd.set(Calendar.HOUR_OF_DAY, 23); weekEnd.set(Calendar.MINUTE, 59)
        weekEnd.set(Calendar.SECOND, 59); weekEnd.set(Calendar.MILLISECOND, 999)

        return cal.timeInMillis in weekStart.timeInMillis..weekEnd.timeInMillis
    }

    private fun isSameMonth(dateStr: String, reference: Calendar): Boolean {
        val cal = parseDateSafe(dateStr) ?: return false
        return cal.get(Calendar.YEAR) == reference.get(Calendar.YEAR) &&
                cal.get(Calendar.MONTH) == reference.get(Calendar.MONTH)
    }

    private fun calculateCategoryTotals(expenses: List<Expense>): Map<String, Float> {
        val totals = mutableMapOf<String, Float>()
        categories.forEach { totals[it] = 0f }

        expenses.forEach { exp ->
            val cat = if (categories.contains(exp.category)) exp.category else "Others"
            totals[cat] = totals.getOrDefault(cat, 0f) + exp.total.toFloat()
        }
        return totals
    }

    private fun calculateLineTotals(expenses: List<Expense>, period: String): Map<String, Float> {
        val totals = mutableMapOf<String, Float>()

        when (period) {
            "WEEK" -> {
                val sdf = SimpleDateFormat("EEE", Locale.getDefault()) // Mon, Tue...
                expenses.forEach { exp ->
                    val date = parseDateSafe(exp.date)?.time ?: Date()
                    val key = sdf.format(date)
                    totals[key] = totals.getOrDefault(key, 0f) + exp.total.toFloat()
                }

                // Optional: order by weekday
                val weekdayOrder = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                return totals.toList()
                    .sortedBy { weekdayOrder.indexOf(it.first) }
                    .toMap()
            }

            "MONTH" -> {
                val sdf = SimpleDateFormat("dd", Locale.getDefault()) // Day of month
                expenses.forEach { exp ->
                    val date = parseDateSafe(exp.date)?.time ?: Date()
                    val key = sdf.format(date)
                    totals[key] = totals.getOrDefault(key, 0f) + exp.total.toFloat()
                }
                return totals.toSortedMap()
            }

            "YEAR" -> {
                val sdf = SimpleDateFormat("MMM", Locale.getDefault()) // Jan, Feb...
                expenses.forEach { exp ->
                    val date = parseDateSafe(exp.date)?.time ?: Date()
                    val key = sdf.format(date)
                    totals[key] = totals.getOrDefault(key, 0f) + exp.total.toFloat()
                }
                val monthOrder = listOf(
                    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                )
                return totals.toList()
                    .sortedBy { monthOrder.indexOf(it.first) }
                    .toMap()
            }

            else -> return emptyMap()
        }
    }



    private fun calculateTopSpending(expenses: List<Expense>): List<TopSpendingItem> {
        if (expenses.isEmpty()) return emptyList()
        return expenses.sortedByDescending { it.total }
            .take(5)
            .map {
                TopSpendingItem(
                    title = it.title,
                    amount = it.total,
                    date = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(parseDateSafe(it.date)?.time ?: Date()),
                    category = if (categories.contains(it.category)) it.category else "Others"
                )
            }
    }

    private fun setupPieChart(pie: PieChart, categoryTotals: Map<String, Float>) {
        val entries = categoryTotals.filter { it.value > 0 }
            .map { PieEntry(it.value, it.key) }
            .ifEmpty { listOf(PieEntry(1f, "No Data")) }

        val dataSet = PieDataSet(entries, "").apply {
            // Updated color palette
            colors = if (entries.first().label == "No Data") {
                listOf(Color.LTGRAY)
            } else {
                listOf(
                    Color.parseColor("#16a085"), // teal
                    Color.parseColor("#e67e22"), // orange
                    Color.parseColor("#e74c3c"), // red
                    Color.parseColor("#2980b9"), // blue
                    Color.parseColor("#9b59b6"), // purple
                    Color.parseColor("#f1c40f")  // yellow
                )
            }
            sliceSpace = 3f
            valueTextSize = 11f
            valueFormatter = PercentFormatter(pie)
            setDrawValues(true)
        }

        pie.apply {
            setUsePercentValues(true)
            data = PieData(dataSet)
            description.isEnabled = false
            setDrawEntryLabels(false)
            centerText = if (entries.first().label == "No Data") "No Data Available" else ""
            setDrawHoleEnabled(true)
            holeRadius = 35f
            transparentCircleRadius = 40f
            legend.isEnabled = entries.first().label != "No Data"
            animateY(1000)
            invalidate()
        }
    }


    private fun setupLineChart(chart: LineChart, totals: Map<String, Float>, period: String) {
        val entries = if (totals.isEmpty()) listOf(Entry(0f, 0f)) else totals.values.mapIndexed { index, value -> Entry(index.toFloat(), value) }
        val labels = if (totals.isEmpty()) listOf("No Data") else totals.keys.toList()

        val dataSet = LineDataSet(entries, "Total Expenses").apply {
            color = Color.parseColor("#16a085")
            lineWidth = 2f
            circleRadius = 4f
            setCircleColor(Color.parseColor("#16a085"))
            valueTextColor = Color.BLACK
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        chart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            xAxis.apply {
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setGranularityEnabled(true)

                valueFormatter = IndexAxisValueFormatter(labels)

                setLabelCount(labels.size, true)   // 🔥 FORCE labels
                setDrawGridLines(false)
            }
            axisRight.isEnabled = false
            axisLeft.axisMinimum = 0f
            legend.isEnabled = false
            animateY(1000)
            invalidate()
        }
    }
}
