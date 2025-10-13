package com.example.e_lista

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.e_lista.databinding.ActivityChartDesign10Binding
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

class ChartDesign10Activity : AppCompatActivity() {

    private lateinit var binding: ActivityChartDesign10Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChartDesign10Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // Highlight current tab
        binding.bottomNavigationView.selectedItemId = R.id.nav_stats

        // 📸 Floating camera button → CameraActivity
        binding.fabCamera.setOnClickListener {
            val intent = Intent(this, Camera11Activity::class.java)
            startActivity(intent)
        }
        // Back button
        binding.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Navigation
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, Home9Activity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }

                R.id.nav_stats -> true // Already here

                R.id.nav_wallet -> {
                    startActivity(Intent(this, Expenses12Activity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }

                R.id.nav_profile -> {
                    startActivity(Intent(this, Profile13Activity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }

                else -> false
            }
        }

        // Get expense data from intent (default to 0 if none)
        val food = intent.getFloatExtra("food", 0f)
        val transport = intent.getFloatExtra("transport", 0f)
        val bills = intent.getFloatExtra("bills", 0f)
        val others = intent.getFloatExtra("others", 0f)

        setupPieChart(binding.pieChart, food, transport, bills, others)
    }

    private fun setupPieChart(pieChart: PieChart, food: Float, transport: Float, bills: Float, others: Float) {
        val entries = arrayListOf<PieEntry>()

        if (food > 0) entries.add(PieEntry(food, "Food"))
        if (transport > 0) entries.add(PieEntry(transport, "Transport"))
        if (bills > 0) entries.add(PieEntry(bills, "Bills"))
        if (others > 0) entries.add(PieEntry(others, "Others"))

        val dataSet = PieDataSet(entries, "Expense Breakdown").apply {
            colors = listOf(
                Color.parseColor("#16a085"),
                Color.parseColor("#27ae60"),
                Color.parseColor("#2ecc71"),
                Color.parseColor("#1abc9c")
            )
            sliceSpace = 3f
            valueTextColor = Color.WHITE
            valueTextSize = 14f
        }

        val data = PieData(dataSet)

        pieChart.apply {
            this.data = data
            setUsePercentValues(true)
            setDrawHoleEnabled(true)
            holeRadius = 35f
            transparentCircleRadius = 40f
            centerText = "Spending"
            setCenterTextSize(18f)
            description.isEnabled = false

            legend.apply {
                orientation = Legend.LegendOrientation.HORIZONTAL
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                textSize = 12f
                form = Legend.LegendForm.CIRCLE
            }

            animateY(1400)
            invalidate()
        }
    }
}
