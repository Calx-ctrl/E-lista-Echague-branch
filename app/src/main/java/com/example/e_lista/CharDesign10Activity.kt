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
        binding.fabCamera.setOnClickListener {
            startActivity(Intent(this, ReceiptScanUpload::class.java))
        }
        setupBottomNavigation()

        binding.topSpendingList.layoutManager = LinearLayoutManager(this)

        binding.filterGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val chips = group.children.toList()
            when (checkedIds.firstOrNull()) {
                chips.getOrNull(0)?.id -> fetchAndDisplayData("DAY")
                chips.getOrNull(1)?.id -> fetchAndDisplayData("WEEK")
                chips.getOrNull(2)?.id -> fetchAndDisplayData("MONTH")
                chips.getOrNull(3)?.id -> fetchAndDisplayData("YEAR")
                else -> fetchAndDisplayData("DAY")
            }
        }

        binding.filterGroup.check(binding.filterGroup.children.firstOrNull()?.id ?: -1)
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { startActivity(Intent(this, Home9Activity::class.java)); finish(); true }
                R.id.nav_wallet -> { startActivity(Intent(this, Expenses12Activity::class.java)); finish(); true }
                R.id.nav_camera_placeholder -> { startActivity(Intent(this, ReceiptScanUpload::class.java)); finish(); true }
                R.id.nav_stats -> true
                R.id.nav_profile -> { startActivity(Intent(this, Profile13Activity::class.java)); finish(); true }
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
                binding.topSpendingList.adapter = TopSpendingAdapter(topCategories)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun filterExpensesByPeriod(expenses: List<Expense>, period: String): List<Expense> {
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis

        return expenses.filter { exp ->
            val ts = exp.timestamp
            when (period) {
                "DAY" -> exp.date == getTodayDate()  // Compare inputted date
                "WEEK" -> isSameWeek(exp.date, cal)
                "MONTH" -> isSameMonth(exp.date, cal)
                "YEAR" -> isSameYear(exp.date, cal)
                else -> false
            }
        }
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
        val formatter = when (period) {
            "DAY" -> SimpleDateFormat("HH", Locale.getDefault())
            "WEEK", "MONTH" -> SimpleDateFormat("dd", Locale.getDefault())
            "YEAR" -> SimpleDateFormat("MMM", Locale.getDefault())
            else -> SimpleDateFormat("dd", Locale.getDefault())
        }

        val totals = mutableMapOf<String, Float>()
        expenses.forEach { exp ->
            val key = formatter.format(Date(exp.timestamp))
            totals[key] = totals.getOrDefault(key, 0f) + exp.total.toFloat()
        }

        return totals.toSortedMap()
    }

    private fun calculateTopSpending(expenses: List<Expense>): List<TopSpendingItem> {
        if (expenses.isEmpty()) {
            return listOf(
                TopSpendingItem(
                    title = "No data available",
                    amount = 0.0,
                    date = "",
                    category = ""
                )
            )
        }

        return expenses.sortedByDescending { it.total }
            .take(5)
            .map { exp ->
                val formattedDate = try {
                    val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val sdfOutput = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    val date = sdfInput.parse(exp.date)
                    if (date != null) sdfOutput.format(date) else exp.date
                } catch (e: Exception) {
                    exp.date
                }

                TopSpendingItem(
                    title = exp.title,
                    amount = exp.total,
                    date = formattedDate,
                    category = if (categories.contains(exp.category)) exp.category else "Others"
                )
            }
    }

    private fun setupPieChart(pie: PieChart, categoryTotals: Map<String, Float>) {
        val entries = categoryTotals
            .filter { it.value > 0 }
            .map { PieEntry(it.value, it.key) }

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                Color.parseColor("#16a085"),
                Color.parseColor("#27ae60"),
                Color.parseColor("#2ecc71"),
                Color.parseColor("#f39c12"),
                Color.parseColor("#e74c3c"),
                Color.parseColor("#8e44ad")
            )
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
            centerText = ""
            setDrawHoleEnabled(true)
            holeRadius = 35f
            transparentCircleRadius = 40f
            legend.apply {
                orientation = Legend.LegendOrientation.HORIZONTAL
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                isWordWrapEnabled = true
                textSize = 12f
            }
            animateY(1000)
            invalidate()
        }
    }

    private fun setupLineChart(chart: LineChart, totals: Map<String, Float>, period: String) {
        val entries = totals.values.mapIndexed { index, value ->
            Entry(index.toFloat(), value)
        }

        val labels = totals.keys.toList()

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
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(labels)
                setDrawGridLines(false)
            }
            axisRight.isEnabled = false
            axisLeft.axisMinimum = 0f
            legend.isEnabled = false
            animateY(1000)
            invalidate()
        }
    }

    // ----------------- DATE HELPERS -----------------
    private fun getTodayDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun isSameWeek(dateStr: String, reference: Calendar): Boolean {
        val cal = try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            Calendar.getInstance().apply { time = sdf.parse(dateStr)!! }
        } catch (e: Exception) { return false }

        val weekStart = reference.clone() as Calendar
        weekStart.set(Calendar.DAY_OF_WEEK, weekStart.firstDayOfWeek)
        weekStart.set(Calendar.HOUR_OF_DAY, 0)
        weekStart.set(Calendar.MINUTE, 0)
        weekStart.set(Calendar.SECOND, 0)
        weekStart.set(Calendar.MILLISECOND, 0)

        val weekEnd = weekStart.clone() as Calendar
        weekEnd.add(Calendar.DAY_OF_WEEK, 6)
        weekEnd.set(Calendar.HOUR_OF_DAY, 23)
        weekEnd.set(Calendar.MINUTE, 59)
        weekEnd.set(Calendar.SECOND, 59)
        weekEnd.set(Calendar.MILLISECOND, 999)

        return cal.timeInMillis in weekStart.timeInMillis..weekEnd.timeInMillis
    }

    private fun isSameMonth(dateStr: String, reference: Calendar): Boolean {
        val cal = try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            Calendar.getInstance().apply { time = sdf.parse(dateStr)!! }
        } catch (e: Exception) { return false }

        return cal.get(Calendar.YEAR) == reference.get(Calendar.YEAR) &&
                cal.get(Calendar.MONTH) == reference.get(Calendar.MONTH)
    }

    private fun isSameYear(dateStr: String, reference: Calendar): Boolean {
        val cal = try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            Calendar.getInstance().apply { time = sdf.parse(dateStr)!! }
        } catch (e: Exception) { return false }

        return cal.get(Calendar.YEAR) == reference.get(Calendar.YEAR)
    }
}
