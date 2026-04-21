package com.example.bluetootheog.repository

import com.example.bluetootheog.bluetooth.BluetoothManager
import com.example.bluetootheog.model.EOGReading

class EOGRepository(private val bluetoothManager: BluetoothManager) {

    // Recording state
    var isRecording = false
        private set

    private val recordingBuffer = mutableListOf<EOGReading>()
    private var firstSampleIndex: Long = -1

    fun startRecording() {
        recordingBuffer.clear()
        firstSampleIndex = -1
        isRecording = true
    }

    fun stopRecording(): List<EOGReading> {
        isRecording = false
        return recordingBuffer.toList()  // Return a copy
    }

    companion object {
        const val SAMPLE_RATE = 128
    }

    fun onNewData(sampleIndex: Long, h: Float, v: Float) {
        if (isRecording) {
            // Capturing first sample index when recording starts
            if (firstSampleIndex == -1L) {
                firstSampleIndex = sampleIndex
            }

            // Relative indexing from recording start
            val relativeSampleIndex = sampleIndex - firstSampleIndex
            val timeMs = relativeSampleIndex * (1000.0 / SAMPLE_RATE)

            recordingBuffer.add(
                EOGReading(
                    sampleIndex = relativeSampleIndex,
                    timeMs = timeMs,
                    h = h,
                    v = v
                )
            )
        }
    }
}