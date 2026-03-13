package com.example.bluetootheog.model

data class EOGReading(
    val timestamp: Long,    // System.currentTimeMillis()
    val h: Float,
    val v: Float
)
