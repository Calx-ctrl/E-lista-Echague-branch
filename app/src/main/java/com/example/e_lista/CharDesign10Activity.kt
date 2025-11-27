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
                val filteredExpenses = filterExpensesByPeriod(expenses, period)
                val categoryTotals = calculateCategoryTotals(filteredExpenses)
                val lineTotals = calculateLineTotals(filteredExpenses, period)
                val (topExpenses, expenseMap) = calculateTopSpending(filteredExpenses)

                setupPieChart(binding.pieChart, categoryTotals)
                setupLineChart(binding.lineChart, lineTotals, period)
                val adapter = TopSpendingAdapter(topExpenses, expenseMap)
                binding.topSpendingList.adapter = adapter
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
                "WEEK" -> {
                    calendar.timeInMillis = now
                    val weekNow = calendar.get(Calendar.WEEK_OF_YEAR)
                    val yearNow = calendar.get(Calendar.YEAR)

                    calendar.timeInMillis = ts
                    val expenseWeek = calendar.get(Calendar.WEEK_OF_YEAR)
                    val expenseYear = calendar.get(Calendar.YEAR)

                    weekNow == expenseWeek && yearNow == expenseYear
                }
                "MONTH" -> {
                    calendar.timeInMillis = now
                    val monthNow = calendar.get(Calendar.MONTH)
                    val yearNow = calendar.get(Calendar.YEAR)

                    calendar.timeInMillis = ts
                    val expenseMonth = calendar.get(Calendar.MONTH)
                    val expenseYear = calendar.get(Calendar.YEAR)

                    monthNow == expenseMonth && yearNow == expenseYear
                }
                "YEAR" -> {
                    calendar.timeInMillis = now
                    val yearNow = calendar.get(Calendar.YEAR)

                    calendar.timeInMillis = ts
                    val expenseYear = calendar.get(Calendar.YEAR)

                    yearNow == expenseYear
                }
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

    private fun calculateTopSpending(expenses: List<Expense>): Pair<List<TopSpendingItem>, Map<String, Expense>> {
        val allItemsWithParent = mutableMapOf<String, Expense>()
        val allItems = mutableListOf<TopSpendingItem>()
        expenses.forEach { expense ->
            expense.items.forEach { item ->
                allItems.add(TopSpendingItem(item.itemName, item.itemAmount))
                allItemsWithParent[item.itemName] = expense
            }
        }
        val topItems = allItems.sortedByDescending { it.amount }.take(5)
        return Pair(topItems, allItemsWithParent)
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
