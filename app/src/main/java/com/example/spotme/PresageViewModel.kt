package com.example.spotme

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.presage.physiology.proto.MetricsProto

class PresageViewModel : ViewModel() {

    private val _pulsesLD = MutableLiveData<List<Int>>()
    val pulsesLD: LiveData<List<Int>> = _pulsesLD

    private val _breathingsLD = MutableLiveData<List<Int>>()
    val breathingsLD: LiveData<List<Int>> = _breathingsLD

    private val _bloodPressuresLD = MutableLiveData<List<Int>>()
    val bloodPressuresLD: LiveData<List<Int>> = _bloodPressuresLD

    private val _faceBlinksLD = MutableLiveData<List<Int>>()
    val faceBlinksLD: LiveData<List<Int>> = _faceBlinksLD

    private val _talkingLD = MutableLiveData<List<Int>>()
    val talkingLD: LiveData<List<Int>> = _talkingLD

    private val _metricsDataLD = MutableLiveData<MetricsProto.MetricsBuffer>()
    val metricsDataLD: LiveData<MetricsProto.MetricsBuffer> = _metricsDataLD

    fun updateMetrics(metricsBuffer: MetricsProto.MetricsBuffer) {
        _metricsDataLD.value = metricsBuffer
    }

    fun updateAllMetrics(
        pulses: List<Int>,
        breathings: List<Int>,
        bloodPressures: List<Int>,
        faceBlinks: List<Int>
    ) {
        _pulsesLD.value = pulses
        _breathingsLD.value = breathings
        _bloodPressuresLD.value = bloodPressures
        _faceBlinksLD.value = faceBlinks
    }

    fun getAverages(time : Int ): String {
        val pulseAvg = _pulsesLD.value?.map { it }?.average() ?: 0.0
        val breathingAvg = _breathingsLD.value?.map { it }?.average() ?: 0.0
        val systolicAvg = _bloodPressuresLD.value?.map { it }?.average() ?: 0.0
        val diastolicAvg = _bloodPressuresLD.value?.map { it }?.average() ?: 0.0
//        val faceBlinkAvg = _faceBlinksLD.value?.map { it.value }?.average() ?: 0.0
        val faceBlinkAvg = if (time > 0) {
            val totalBlinks = _faceBlinksLD.value?.size ?: 0
            (totalBlinks.toDouble() / time) * 60.0
        } else { 0.0 }
        val talkingAvg = {
            var totalTalking = _talkingLD.value?.size ?: 0
            (totalTalking.toDouble() / time) * 60.0
        }

        return """ 
            Pulse Average: ${pulseAvg} bpm
            Breathing Average: ${breathingAvg} bpm
            Face Blink Average: ${faceBlinkAvg} blinks per minute
            Talking Average: ${talkingAvg()} seconds per minute
        """.trimIndent()
    }
//
//    class Pulse(
//        val timestamp: Long,
//        val value: Int
//    )
//
//    class Breathing(
//        val timestamp: Long,
//        val value: Int
//    )
//
//    class BloodPressure(
//        val timestamp: Long,
//        val systolic: Int,
//        val diastolic: Int
//    )
//
//    class FaceBlink(
//        val timestamp: Long,
//        val value: Int
//    )

}

//    val pulse = metrics.pulse
//    val breathing = metrics.breathing
//    val bloodPressure = metrics.bloodPressure
//    val face = metrics.face