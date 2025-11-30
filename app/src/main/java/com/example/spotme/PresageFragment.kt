package com.example.spotme

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels

import com.presage.physiology.proto.MetricsProto
import com.presagetech.smartspectra.SmartSpectraMode
import com.presagetech.smartspectra.SmartSpectraView
import com.presagetech.smartspectra.SmartSpectraSdk
import com.example.spotme.databinding.FragmentPresageBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.ScatterChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.ScatterData
import com.github.mikephil.charting.data.ScatterDataSet
import com.google.android.material.button.MaterialButton
import java.io.File
import java.util.Locale


class PresageFragment : Fragment() {

    companion object {
        fun newInstance() = PresageFragment()
    }

    private val viewModel: PresageViewModel by activityViewModels()
    private var _binding: FragmentPresageBinding? = null
    private val binding get() = _binding!!
    private lateinit var main : MainActivity

    private lateinit var smartSpectraView: SmartSpectraView
    private var smartSpectraMode = SmartSpectraMode.CONTINUOUS
    private var cameraPosition = CameraSelector.LENS_FACING_FRONT
    private var measurementDuration = 20.0

    private lateinit var buttonContainer: LinearLayout
    private lateinit var chartContainer: LinearLayout
    private lateinit var faceMeshContainer: ScatterChart

    private lateinit var globalMetricsBuffer : MetricsProto.MetricsBuffer

    private val isCustomizationEnabled: Boolean = true
    private val isFaceMeshEnabled: Boolean = true

    private var textSpeech: String = ""

    private val smartSpectraSdk = SmartSpectraSdk.getInstance().apply {
//        setApiKey(R.string.PRESAGE_API_KEY.toString())
        setApiKey("PYy29upqBq8iks2pf67TQ1b4LQmhcQq71s0slv11")
        setMeasurementDuration(measurementDuration)
        setShowFps(false)
        setRecordingDelay(3)
        setSmartSpectraMode(smartSpectraMode)
        setCameraPosition(cameraPosition)
        setMetricsBufferObserver { metricsBuffer ->
            handleMetricsBuffer(metricsBuffer)
//            updateViewModel(metricsBuffer)
            globalMetricsBuffer = metricsBuffer
        }
        setEdgeMetricsObserver { edgeMetrics ->
            handleEdgeMetrics(edgeMetrics)
        }
    }

    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == android.app.Activity.RESULT_OK && data != null) {
            val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.getOrNull(0) ?: ""
            textSpeech = results
            binding.textSpeech.text = results
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPresageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        main = requireActivity() as MainActivity


        smartSpectraView = binding.smartSpectraView
        buttonContainer = binding.buttonContainer
        chartContainer = binding.chartContainer
        faceMeshContainer = binding.meshContainer

        smartSpectraSdk.showControlsInScreeningView(isCustomizationEnabled)

        if (isCustomizationEnabled) {
            val smartSpectraModeButton = MaterialButton(
                requireContext(),
                null,
                com.google.android.material.R.attr.materialIconButtonStyle
            ).apply {
                text =
                    "Switch SmartSpectra Mode to ${if (smartSpectraMode == SmartSpectraMode.SPOT) "CONTINUOUS" else "SPOT"}"
                setIconResource(if (smartSpectraMode == SmartSpectraMode.SPOT) R.drawable.ic_line_chart else R.drawable.ic_scatter_plot)
                iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
                iconPadding = 10
            }
            smartSpectraModeButton.setOnClickListener {
                if (smartSpectraMode == SmartSpectraMode.CONTINUOUS) {
                    smartSpectraMode = SmartSpectraMode.SPOT
                    smartSpectraModeButton.text = "Switch SmartSpectra Mode to CONTINUOUS"
                    smartSpectraModeButton.setIconResource(R.drawable.ic_line_chart)
                } else {
                    smartSpectraMode = SmartSpectraMode.CONTINUOUS
                    smartSpectraModeButton.text = "Switch SmartSpectra Mode to SPOT"
                    smartSpectraModeButton.setIconResource(R.drawable.ic_scatter_plot)
                }
                smartSpectraSdk.setSmartSpectraMode(smartSpectraMode)
            }
            buttonContainer.addView(smartSpectraModeButton)

            val cameraPositionButton = MaterialButton(
                requireContext(),
                null,
                com.google.android.material.R.attr.materialIconButtonStyle
            ).apply {
                text =
                    "Switch Camera to ${if (cameraPosition == CameraSelector.LENS_FACING_FRONT) "BACK" else "FRONT"}"
                setIconResource(R.drawable.ic_flip_camera)
                iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
                iconPadding = 10
            }
            cameraPositionButton.setOnClickListener {
                if (cameraPosition == CameraSelector.LENS_FACING_FRONT) {
                    cameraPosition = CameraSelector.LENS_FACING_BACK
                    cameraPositionButton.text = "Switch Camera to FRONT"
                } else {
                    cameraPosition = CameraSelector.LENS_FACING_FRONT
                    cameraPositionButton.text = "Switch Camera to BACK"
                }
                smartSpectraSdk.setCameraPosition(cameraPosition)
            }

            // Add the button to the layout
            buttonContainer.addView(cameraPositionButton)

            // Example buttons to change measurement duration
            val measurementDurationButtonRow = createMeasurementButtonRow { newDuration ->
                smartSpectraSdk.setMeasurementDuration(newDuration)
            }
            buttonContainer.addView(measurementDurationButtonRow)
        }

        val file = File(context?.filesDir, "metrics_data.txt")
        if (!file.exists()) {
            Log.i("Metrics", "Metrics file not found.")
            Toast.makeText(context, "Metrics file not found.", Toast.LENGTH_LONG).show()
        }
        try {
            val data: ByteArray = file.readBytes()
            globalMetricsBuffer = MetricsProto.MetricsBuffer.parseFrom(data)
            handleMetricsBuffer(globalMetricsBuffer)
            Log.i("Metrics", "Successfully decoded metrics from file.")
            Toast.makeText(context, "Successfully decoded metrics from file.", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            e.printStackTrace()
        }

        binding.submitBtn.setOnClickListener {
            Toast.makeText(context, "Submitting metrics", Toast.LENGTH_LONG).show()
            if (textSpeech.isNotEmpty()) { viewModel.updateSpeechText(textSpeech) }
            updateMetricsLD(globalMetricsBuffer)
            main.switchFragment(main.chatbotFragment)
        }

        binding.saveBtn.setOnClickListener {
            val data : ByteArray
            try {
                data = globalMetricsBuffer.toByteArray()
                try {
                    context?.openFileOutput("metrics_data.txt", Context.MODE_PRIVATE).use { outputStream ->
                        outputStream?.write(data)
                    }
                    Log.i("DataSaver", "Metrics saved to internal storage.")
                    Toast.makeText(context, "Metrics saved to internal storage.", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Log.e("DataSaver", "Failed to save metrics internally: ${e.message}")
                    Toast.makeText(context, "Failed to save metrics internally.", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        binding.btnHide.setOnClickListener {
            if (binding.layoutOther.isVisible ) {
                binding.layoutOther.visibility = View.GONE
            } else {
                binding.layoutOther.visibility = View.VISIBLE
            }
        }

        binding.btnSpeech.setOnClickListener {
            startSpeechToText()
        }

        viewModel.questionLD.observe( viewLifecycleOwner ) { question ->
            binding.textQuestion.text = question
        }

    }

    private fun startSpeechToText() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to text")
        }
        speechRecognizerLauncher.launch(intent)
    }



    private fun updateMetricsLD(metrics: MetricsProto.MetricsBuffer) {
        if (metrics.hasMetadata()) {
            // "ID: ${metrics.metadata.id}"
            // "Upload Timestamp: ${metrics.metadata.uploadTimestamp}"
        }

        val pulse = metrics.pulse
        val breathing = metrics.breathing
        val bloodPressure = metrics.bloodPressure
        val face = metrics.face

        viewModel.updateAllMetrics(

            pulses = pulse.rateList.map {
                it.value.toInt()
            },
            breathings = breathing.rateList.map {
                it.value.toInt()
            },
            bloodPressures = bloodPressure.phasicList.map {
                it.value.toInt()
            },
            faceBlinks = face.blinkingList
                .filter { it.detected }
                .map {
                    1
                },
            talkings = face.talkingList
                .filter { it.detected }
                .map {
                    1
                }
        )

        viewModel.updateMetrics(metrics)

    }


    private fun handleEdgeMetrics(edgeMetrics: MetricsProto.Metrics) {
        // Handle dense face landmarks from edge metrics if face mesh is enabled
        if (isFaceMeshEnabled && edgeMetrics.hasFace() && edgeMetrics.face.landmarksCount > 0) {
            // Get the latest landmarks from edge metrics
            val latestLandmarks = edgeMetrics.face.landmarksList.lastOrNull()
            latestLandmarks?.let { landmarks ->
                val meshPoints = landmarks.valueList.map { landmark ->
                    Pair(landmark.x.toInt(), landmark.y.toInt())
                }
                handleMeshPoints(meshPoints)
            }
        }
    }

    private fun handleMeshPoints(meshPoints: List<Pair<Int, Int>>) {

        val chart = faceMeshContainer
        chart.isVisible = true

        val scaledPoints = meshPoints.map { Entry(1f - it.first / 720f, 1f - it.second / 720f) }
            .sortedBy { it.x }

        val dataSet = ScatterDataSet(scaledPoints, "Mesh Points").apply {
            setDrawValues(false)
            scatterShapeSize = 15f
            setScatterShape(ScatterChart.ScatterShape.CIRCLE)
        }

        val scatterData = ScatterData(dataSet)

        // Customize the chart
        chart.apply {
            data = scatterData
            axisLeft.isEnabled = false
            axisRight.isEnabled = false
            xAxis.isEnabled = false
            setTouchEnabled(false)
            description.isEnabled = false
            legend.isEnabled = false

            // Set visible range to make x and y axis have the same range

            setVisibleXRange(0f, 1f)
            setVisibleYRange(0f, 1f, YAxis.AxisDependency.LEFT)

            // Move view to the data
            moveViewTo(0f, 0f, YAxis.AxisDependency.LEFT)
        }

        // Refresh the chart
        chart.invalidate()
    }


    private fun createMeasurementButtonRow(
        onMeasurementDurationChanged: (Double) -> Unit
    ): LinearLayout {
        val buttonRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 0, 16, 0) // left, top, right, bottom margins in pixels
            }
        }

        val measurementDurationTextView = TextView(requireContext()).apply {
            text = "Measurement Duration: ${measurementDuration.toInt()}s"
            textSize = 18f
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f // Ensures even spacing
            )
        }

        val decreaseDurationButton = MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialIconButtonStyle).apply {
            text = "-"
            textSize = 24f // Make the button text bigger
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        decreaseDurationButton.setOnClickListener {
            if (measurementDuration > 20.0) {
                measurementDuration -= 5.0
                onMeasurementDurationChanged(measurementDuration)
                measurementDurationTextView.text = "Measurement Duration: ${measurementDuration.toInt()}s"
            }
        }

        val increaseDurationButton = MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialIconButtonStyle).apply {
            text = "+"
            textSize = 24f // Make the button text bigger
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        increaseDurationButton.setOnClickListener {
            if (measurementDuration < 120.0) {
                measurementDuration += 5.0
                onMeasurementDurationChanged(measurementDuration)
                measurementDurationTextView.text = "Measurement Duration: ${measurementDuration.toInt()}s"
            }
        }

        buttonRow.addView(measurementDurationTextView)
        buttonRow.addView(decreaseDurationButton)
        buttonRow.addView(increaseDurationButton)

        return buttonRow
    }




    fun handleMetricsBuffer(metrics: MetricsProto.MetricsBuffer) {
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
            addChart(pulse.traceList.map { Entry(it.time, it.value) }, "Pulse Pleth", false)
        }
        if (pulse.rateCount > 0) {
            addChart(pulse.rateList.map { Entry(it.time, it.value) }, "Pulse Rates", true)
            addChart(
                pulse.rateList.map { Entry(it.time, it.confidence) },
                "Pulse Rate Confidence",
                true
            )
        }

        if (breathing.upperTraceCount > 0) {
            addChart(
                breathing.upperTraceList.map { Entry(it.time, it.value) },
                "Breathing Pleth",
                false
            )
        }
        if (breathing.rateCount > 0) {
            addChart(breathing.rateList.map { Entry(it.time, it.value) }, "Breathing Rates", true)
            addChart(
                breathing.rateList.map { Entry(it.time, it.confidence) },
                "Breathing Rate Confidence",
                true
            )
        }
        if (breathing.amplitudeCount > 0) {
            addChart(
                breathing.amplitudeList.map { Entry(it.time, it.value) },
                "Breathing Amplitude",
                true
            )
        }
        if (breathing.apneaCount > 0) {
            addChart(
                breathing.apneaList.map { Entry(it.time, if (it.detected) 1f else 0f) },
                "Apnea",
                true
            )
        }
        if (breathing.baselineCount > 0) {
            addChart(
                breathing.baselineList.map { Entry(it.time, it.value) },
                "Breathing Baseline",
                true
            )
        }
        if (breathing.respiratoryLineLengthCount > 0) {
            addChart(
                breathing.respiratoryLineLengthList.map { Entry(it.time, it.value) },
                "Respiratory Line Length",
                true
            )
        }
        if (breathing.inhaleExhaleRatioCount > 0) {
            addChart(
                breathing.inhaleExhaleRatioList.map { Entry(it.time, it.value) },
                "Inhale-Exhale Ratio",
                true
            )
        }

        // Blood pressure plots
        if (bloodPressure.phasicCount > 0) {
            addChart(bloodPressure.phasicList.map { Entry(it.time, it.value) }, "Phasic", true)
        }

        // Face plots
        if (face.blinkingCount > 0) {
            addChart(
                face.blinkingList.map { Entry(it.time, if (it.detected) 1f else 0f) },
                "Blinking",
                true
            )
        }
        if (face.talkingCount > 0) {
            addChart(
                face.talkingList.map { Entry(it.time, if (it.detected) 1f else 0f) },
                "Talking",
                true
            )
        }
    }


    private fun addChart(entries: List<Entry>, title: String, showYTicks: Boolean) {
        val chart = LineChart(context)

        val density = resources.displayMetrics.density
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

