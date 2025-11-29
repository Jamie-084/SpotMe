package com.example.spotme

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.presage.physiology.proto.MetricsProto

object PresageGraphs {


    fun handleMetricsBuffer(metrics: MetricsProto.MetricsBuffer, chartContainer : LinearLayout, context : Context) {
        chartContainer.removeAllViews()

        if (metrics.hasMetadata()) {
            val idTextView = TextView(chartContainer.context).apply {
                text = "ID: ${metrics.metadata.id}"
                textSize = 16f
                setPadding(16, 8, 16, 8)
            }

            val timestampTextView = TextView(chartContainer.context).apply {
                text = "Upload Timestamp: ${metrics.metadata.uploadTimestamp}"
                textSize = 16f
                setPadding(16, 8, 16, 8)
            }

            // Add the TextViews to the chart container
            chartContainer.addView(idTextView)
            chartContainer.addView(timestampTextView)
        }

        // get the relevant metrics
        val pulse = metrics.pulse
        val breathing = metrics.breathing
        val bloodPressure = metrics.bloodPressure
        val face = metrics.face

        // Pulse plots
        if (pulse.traceCount > 0) {
            addChart(pulse.traceList.map { Entry(it.time, it.value) }, "Pulse Pleth", false, chartContainer, context)
        }
        if (pulse.rateCount > 0) {
            addChart(pulse.rateList.map { Entry(it.time, it.value) }, "Pulse Rates", true, chartContainer, context)
            addChart(
                pulse.rateList.map { Entry(it.time, it.confidence) },
                "Pulse Rate Confidence",
                true, chartContainer, context
            )
        }

        if (breathing.upperTraceCount > 0) {
            addChart(
                breathing.upperTraceList.map { Entry(it.time, it.value) },
                "Breathing Pleth",
                false, chartContainer, context
            )
        }
        if (breathing.rateCount > 0) {
            addChart(breathing.rateList.map { Entry(it.time, it.value) }, "Breathing Rates", true, chartContainer, context)
            addChart(
                breathing.rateList.map { Entry(it.time, it.confidence) },
                "Breathing Rate Confidence",
                true, chartContainer, context
            )
        }
        if (breathing.amplitudeCount > 0) {
            addChart(
                breathing.amplitudeList.map { Entry(it.time, it.value) },
                "Breathing Amplitude",
                true, chartContainer, context
            )
        }
        if (breathing.apneaCount > 0) {
            addChart(
                breathing.apneaList.map { Entry(it.time, if (it.detected) 1f else 0f) },
                "Apnea",
                true, chartContainer, context
            )
        }
        if (breathing.baselineCount > 0) {
            addChart(
                breathing.baselineList.map { Entry(it.time, it.value) },
                "Breathing Baseline",
                true, chartContainer, context
            )
        }
        if (breathing.respiratoryLineLengthCount > 0) {
            addChart(
                breathing.respiratoryLineLengthList.map { Entry(it.time, it.value) },
                "Respiratory Line Length",
                true, chartContainer, context
            )
        }
        if (breathing.inhaleExhaleRatioCount > 0) {
            addChart(
                breathing.inhaleExhaleRatioList.map { Entry(it.time, it.value) },
                "Inhale-Exhale Ratio",
                true, chartContainer, context
            )
        }

        // Blood pressure plots
        if (bloodPressure.phasicCount > 0) {
            addChart(bloodPressure.phasicList.map { Entry(it.time, it.value) }, "Phasic", true, chartContainer, context)
        }

        // Face plots
        if (face.blinkingCount > 0) {
            addChart(
                face.blinkingList.map { Entry(it.time, if (it.detected) 1f else 0f) },
                "Blinking",
                true, chartContainer, context
            )
        }
        if (face.talkingCount > 0) {
            addChart(
                face.talkingList.map { Entry(it.time, if (it.detected) 1f else 0f) },
                "Talking",
                true, chartContainer, context
            )
        }
    }


    private fun addChart(entries: List<Entry>, title: String, showYTicks: Boolean, chartContainer : LinearLayout, context: Context) {
        val chart = LineChart(context)

        val density = context.resources.displayMetrics.density
        val heightInPx = (200 * density).toInt()

        chart.layoutParams = LinearLayout.LayoutParams (
            LinearLayout.LayoutParams.MATCH_PARENT,
            heightInPx
        )


        val titleView = TextView(context)
        titleView.text = title
        titleView.textSize = 18f
        titleView.gravity = Gravity.CENTER
        titleView.setTypeface(null, Typeface.BOLD)

        val xLabelView = TextView(context)
        xLabelView.setText(R.string.api_xLabel)
        xLabelView.gravity = Gravity.CENTER
        xLabelView.setPadding(0, 0, 0, 20)

        chartContainer.addView(titleView)
        chartContainer.addView(chart)
        chartContainer.addView(xLabelView)

        dataPlotting(chart, entries, showYTicks)
    }


    /**
     * Configures and displays a line chart with the provided data entries.
     * This function sets up the line chart to show a simplified and clean visualization,
     * removing unnecessary visual elements like grid lines, axis lines, labels, and legends.
     * It sets the line color to red and ensures that no markers or value texts are shown.
     *
     * @param chart The LineChart object to configure and display data on.
     * @param entries The list of Entry objects representing the data points to be plotted.
     * @param showYTicks Whether to show the Y axis ticks
     */
    private fun dataPlotting(chart: LineChart, entries: List<Entry>, showYTicks: Boolean) {
        val dataSet = LineDataSet(entries, "Data")

        dataSet.setDrawValues(false)
        dataSet.setDrawCircles(false)
        dataSet.color = Color.RED

        chart.data = LineData(dataSet)

        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.setDrawAxisLine(true)
        chart.xAxis.granularity = 1.0f

        chart.axisLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
        chart.axisLeft.setDrawZeroLine(false)
        chart.axisLeft.setDrawGridLines(false)
        chart.axisLeft.setDrawAxisLine(true)
        chart.axisLeft.setDrawLabels(showYTicks)

        chart.axisRight.isEnabled = false
        chart.legend.isEnabled = false
        chart.description.isEnabled = false
        chart.onTouchListener = null
        chart.invalidate()
    }


}