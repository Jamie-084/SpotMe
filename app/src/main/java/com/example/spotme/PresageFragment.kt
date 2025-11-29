package com.example.spotme

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector

import com.presage.physiology.proto.MetricsProto
import com.presagetech.smartspectra.SmartSpectraMode
import com.presagetech.smartspectra.SmartSpectraView
import com.presagetech.smartspectra.SmartSpectraSdk
import com.example.spotme.databinding.FragmentPresageBinding

class PresageFragment : Fragment() {

    companion object {
        fun newInstance() = PresageFragment()
    }

    private val viewModel: PresageViewModel by viewModels()
    private var _binding: FragmentPresageBinding? = null
    private val binding get() = _binding!!

    private lateinit var smartSpectraView: SmartSpectraView
    private var smartSpectraMode = SmartSpectraMode.CONTINUOUS
    // define front or back camera to use
    private var cameraPosition = CameraSelector.LENS_FACING_FRONT
    // measurement duration (valid ranges are between 20.0 and 120.0) Defaults to 30.0 when not set
    // For continuous SmartSpectra mode currently defaults to infinite
    private var measurementDuration = 30.0

    // Replace with your API key from https://physiology.presagetech.com
    private val smartSpectraSdk = SmartSpectraSdk.getInstance().apply {
        setApiKey("PYy29upqBq8iks2pf67TQ1b4LQmhcQq71s0slv11")
        setMeasurementDuration(measurementDuration)
        setShowFps(false)
        //Recording delay defaults to 3 if not provided
        setRecordingDelay(3)
        // smartSpectra mode (SPOT or CONTINUOUS. Defaults to CONTINUOUS when not set)
        setSmartSpectraMode(smartSpectraMode)
        // select camera (front or back, defaults to front when not set)
        setCameraPosition(cameraPosition)
        // Optional: Only need to set it if you want to access metrics to do any processing
        setMetricsBufferObserver { metricsBuffer ->
            handleMetricsBuffer(metricsBuffer)
        }
        // Optional: Only need to set it if you want to access edge metrics and dense face landmarks
        setEdgeMetricsObserver { edgeMetrics ->
            handleEdgeMetrics(edgeMetrics)
        }
    }

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        smartSpectraView = findViewById(R.id.smart_spectra_view)
//        // TODO: Use the ViewModel
//    }

//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        return inflater.inflate(R.layout.fragment_presage, container, false)
//    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPresageBinding.inflate(inflater, container, false)
        return binding.root
    }


    private fun handleMetricsBuffer(metrics: MetricsProto.MetricsBuffer) {
        // get the relevant metricsx
        val pulse = metrics.pulse
        val breathing = metrics.breathing

        // Plot the results

//        // Pulse plots
//        if (pulse.traceCount > 0) {
//            addChart(pulse.traceList.map { Entry(it.time, it.value) },  "Pulse Pleth", false)
//        }
//        // Breathing plots
//        if (breathing.upperTraceCount > 0) {
//            addChart(breathing.upperTraceList.map { Entry(it.time, it.value) }, "Breathing Pleth", false)
//        }
        println("Pulse: ${pulse} bpm")
        println("Breathing: ${breathing} bpm")
        // TODO: See examples of plotting other metrics in MainActivity.kt
    }

    private fun handleEdgeMetrics(edgeMetrics: MetricsProto.Metrics) {
        // Handle dense face landmarks from edge metrics
        if (edgeMetrics.hasFace() && edgeMetrics.face.landmarksCount > 0) {
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
        //Timber.d("Observed mesh points: ${meshPoints.size}")
        // TODO: Update UI or handle the points as needed. See examples of plotting in MainActivity.kt
    }

}

