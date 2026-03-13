package com.example.bluetootheog.utils

import android.content.Context
import android.os.Environment
import com.example.bluetootheog.model.EOGReading
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CSVExporter {
    fun saveToCSV(context: Context, readings: List<EOGReading>) {
        // Creates a file like: EOG_2024_01_15_10_30_00.csv
        val fileName = "EOG_${
            SimpleDateFormat(
                "yyyy_MM_dd_HH_mm_ss", Locale.getDefault()
            ).format(Date())
        }.csv"

        val file = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS
            ), fileName
        )

        file.bufferedWriter().use { writer ->
            // Header
            writer.write("timestamp_ms,h_signal,v_signal\n")
            // Data rows
            readings.forEach { reading ->
                writer.write("${reading.timestamp},${reading.h},${reading.v}\n")
            }
        }
    }
}