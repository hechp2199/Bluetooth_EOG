package com.example.bluetootheog.repository

import com.example.bluetootheog.bluetooth.BluetoothManager
import com.example.bluetootheog.model.EOGReading

class EOGRepository(private val bluetoothManager: BluetoothManager) {

    // Recording state
    var isRecording = false
        private set

    private val recordingBuffer = mutableListOf<EOGReading>()

    fun startRecording() {
        recordingBuffer.clear()
        isRecording = true
    }

    fun stopRecording(): List<EOGReading> {
        isRecording = false
        return recordingBuffer.toList()  // Return a copy
    }

    fun onNewData(h: Float, v: Float) {
        if (isRecording) {
            recordingBuffer.add(
                EOGReading(
                    timestamp = System.currentTimeMillis(), h = h, v = v
                )
            )
        }
    }
}