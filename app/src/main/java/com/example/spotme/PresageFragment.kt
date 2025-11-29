package com.example.spotme

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.camera.core.CameraSelector
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.example.spotme.PresageGraphs.handleMetricsBuffer

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
import kotlin.text.toInt


class PresageFragment : Fragment() {

    companion object {
        fun newInstance() = PresageFragment()
    }

    private val viewModel: PresageViewModel by activityViewModels()
    private var _binding: FragmentPresageBinding? = null
    private val binding get() = _binding!!

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

    private val smartSpectraSdk = SmartSpectraSdk.getInstance().apply {
        setApiKey("PYy29upqBq8iks2pf67TQ1b4LQmhcQq71s0slv11")
        setMeasurementDuration(measurementDuration)
        setShowFps(false)
        setRecordingDelay(3)
        setSmartSpectraMode(smartSpectraMode)
        setCameraPosition(cameraPosition)
        // Optional: Only need to set it if you want to access metrics to do any processing
        setMetricsBufferObserver { metricsBuffer ->
            handleMetricsBuffer(metricsBuffer, chartContainer, requireContext())
//            updateViewModel(metricsBuffer)
            globalMetricsBuffer = metricsBuffer
        }
        // Optional: Only need to set it if you want to access edge metrics and dense face landmarks
        setEdgeMetricsObserver { edgeMetrics ->
            handleEdgeMetrics(edgeMetrics)
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

        smartSpectraView = binding.smartSpectraView
        buttonContainer = binding.buttonContainer
        chartContainer = binding.chartContainer
        faceMeshContainer = binding.meshContainer

        // (optional) toggle display of camera and smartspectra mode controls in screening view
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
            return
        }
        try {
            val data: ByteArray = file.readBytes()
            globalMetricsBuffer = MetricsProto.MetricsBuffer.parseFrom(data)
            handleMetricsBuffer(globalMetricsBuffer, chartContainer, requireContext())
            Log.i("Metrics", "Successfully decoded metrics from file.")

        } catch (e: Exception) {
            e.printStackTrace()
        }

        binding.demoBtn.setOnClickListener {
            updateViewModel(globalMetricsBuffer)
        }

        binding.saveBtn.setOnClickListener {
            val data : ByteArray
            try {
                data = globalMetricsBuffer.toByteArray()
                try {
                    context?.openFileOutput("metrics_data.txt", Context.MODE_PRIVATE).use { outputStream ->
                        outputStream?.write(data)
                    }
                    // Data saved to /data/data/your.package.name/files/filename
                    Log.i("DataSaver", "Metrics saved to internal storage.")
                } catch (e: Exception) {
                    Log.e("DataSaver", "Failed to save metrics internally: ${e.message}")
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }



    private fun updateViewModel(metrics: MetricsProto.MetricsBuffer) {
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
            talking = face.talkingList
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

}

