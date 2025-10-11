package com.example.e_lista

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

        setupPieChart(binding.pieChart)
    }

    private fun setupPieChart(pieChart: PieChart) {
        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(40f, "Food"))
        entries.add(PieEntry(25f, "Transport"))
        entries.add(PieEntry(20f, "Bills"))
        entries.add(PieEntry(15f, "Others"))

        val dataSet = PieDataSet(entries, "Expense Breakdown")
        dataSet.colors = listOf(
            Color.parseColor("#16a085"),
            Color.parseColor("#27ae60"),
            Color.parseColor("#2ecc71"),
            Color.parseColor("#1abc9c")
        )
        dataSet.sliceSpace = 3f
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 14f

        val data = PieData(dataSet)

        pieChart.data = data
        pieChart.setUsePercentValues(true)
        pieChart.setDrawHoleEnabled(true)
        pieChart.holeRadius = 35f
        pieChart.transparentCircleRadius = 40f
        pieChart.centerText = "Spending"
        pieChart.setCenterTextSize(18f)
        pieChart.description.isEnabled = false

        val legend = pieChart.legend
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.textSize = 12f
        legend.form = Legend.LegendForm.CIRCLE

        pieChart.animateY(1400)
        pieChart.invalidate()
    }
}
