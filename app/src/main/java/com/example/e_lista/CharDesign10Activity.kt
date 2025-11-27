package com.example.e_lista

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
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

        // RecyclerView for Top Spending
        binding.topSpendingList.layoutManager = LinearLayoutManager(this)

        // ChipGroup filtering
        binding.filterGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            when (checkedIds.firstOrNull()) {
                group.getChildAt(0).id -> fetchAndDisplayData("DAY")
                group.getChildAt(1).id -> fetchAndDisplayData("WEEK")
                group.getChildAt(2).id -> fetchAndDisplayData("MONTH")
                group.getChildAt(3).id -> fetchAndDisplayData("YEAR")
                else -> fetchAndDisplayData("DAY")
            }
        }
        binding.filterGroup.check(binding.filterGroup.getChildAt(0).id)
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
                val filteredExpenses = filterExpensesByPeriod(expenses, period)
                val categoryTotals = calculateCategoryTotals(filteredExpenses)
                val lineTotals = calculateLineTotals(filteredExpenses, period)
                val topExpenses = calculateTopSpending(filteredExpenses)

                setupPieChart(binding.pieChart, categoryTotals)
                setupLineChart(binding.lineChart, lineTotals, period)
                displayTopSpending(topExpenses)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun filterExpensesByPeriod(expenses: List<Expense>, period: String): List<Expense> {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        return expenses.filter { expense ->
            val ts = expense.timestamp
            when (period) {
                "DAY" -> now - ts < 24 * 60 * 60 * 1000
                "WEEK" -> { calendar.timeInMillis = now; val weekNow = calendar.get(Calendar.WEEK_OF_YEAR); calendar.timeInMillis = ts; weekNow == calendar.get(Calendar.WEEK_OF_YEAR) && calendar.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR) }
                "MONTH" -> { calendar.timeInMillis = now; val monthNow = calendar.get(Calendar.MONTH); val yearNow = calendar.get(Calendar.YEAR); calendar.timeInMillis = ts; monthNow == calendar.get(Calendar.MONTH) && yearNow == calendar.get(Calendar.YEAR) }
                "YEAR" -> { calendar.timeInMillis = now; val yearNow = calendar.get(Calendar.YEAR); calendar.timeInMillis = ts; yearNow == calendar.get(Calendar.YEAR) }
                else -> false
            }
        }
    }

    private fun calculateCategoryTotals(expenses: List<Expense>): Map<String, Float> {
        val totals = mutableMapOf<String, Float>()
        categories.forEach { totals[it] = 0f }
        expenses.forEach { expense ->
            val cat = if (categories.contains(expense.category)) expense.category else "Others"
            totals[cat] = totals.getOrDefault(cat, 0f) + expense.total.toFloat()
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
        expenses.forEach { expense ->
            val key = formatter.format(Date(expense.timestamp))
            totals[key] = totals.getOrDefault(key, 0f) + expense.total.toFloat()
        }
        return totals.toSortedMap()
    }

    private fun calculateTopSpending(expenses: List<Expense>): List<TopSpendingItem> {
        val allItems = expenses.flatMap { it.items.map { item -> TopSpendingItem(item.itemName, item.itemAmount) } }
        return allItems.sortedByDescending { it.amount }.take(5)
    }

    private fun displayTopSpending(topItems: List<TopSpendingItem>) {
        val adapter = TopSpendingAdapter(topItems)
        binding.topSpendingList.adapter = adapter
    }

    private fun setupPieChart(pieChart: PieChart, categoryTotals: Map<String, Float>) {
        val entries = ArrayList<PieEntry>()
        categoryTotals.forEach { (category, total) -> if (total > 0f) entries.add(PieEntry(total, category)) }

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
            setDrawValues(false)
        }

        pieChart.apply {
            data = PieData(dataSet)
            setUsePercentValues(true)
            setDrawHoleEnabled(true)
            holeRadius = 35f
            transparentCircleRadius = 40f
            centerText = ""
            description.isEnabled = false
            setDrawEntryLabels(false)
            legend.apply {
                orientation = Legend.LegendOrientation.HORIZONTAL
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                isWordWrapEnabled = true
                textSize = 12f
                form = Legend.LegendForm.CIRCLE
            }
            animateY(1000)
            invalidate()
        }
    }

    private fun setupLineChart(lineChart: LineChart, totals: Map<String, Float>, period: String) {
        val entries = ArrayList<Entry>()
        val labels = totals.keys.toList()
        totals.values.forEachIndexed { index, value -> entries.add(Entry(index.toFloat(), value)) }

        val dataSet = LineDataSet(entries, "Total Expenses").apply {
            color = Color.parseColor("#16a085")
            valueTextColor = Color.BLACK
            lineWidth = 2f
            circleRadius = 4f
            setCircleColor(Color.parseColor("#16a085"))
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        lineChart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            xAxis.apply { granularity = 1f; valueFormatter = IndexAxisValueFormatter(labels); setDrawGridLines(false) }
            axisRight.isEnabled = false
            axisLeft.axisMinimum = 0f
            legend.isEnabled = false
            animateY(1000)
            invalidate()
        }
    }
}
