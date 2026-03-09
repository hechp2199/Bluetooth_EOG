package com.example.bluetootheog

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.bluetootheog.model.CircularBuffer
import com.example.bluetootheog.ui.theme.BluetoothEOGTheme
import kotlinx.coroutines.delay
import java.io.InputStream
import kotlin.concurrent.thread
import kotlin.math.roundToInt


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkBluetoothPermissions()
        setContent {
            BluetoothEOGTheme {
                EOGApp()
            }
        }
    }

    // Variable declaration
    private var inputStream: InputStream? = null
    var onDataReceived: ((Float, Float) -> Unit)? = null
    var onConnectionChanged: ((Boolean) -> Unit)? = null
    private var isReading = false
    private val REQUEST_BLUETOOTH_PERMISSIONS = 1
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    var isConnected = false
        private set

    // Bluetooth permission object
    private val bluetoothPermissions = arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN
    )

    // Function to check whether bluetooth permission is granted
    private fun checkBluetoothPermissions() {
        val missingPermissions = bluetoothPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this, missingPermissions.toTypedArray(), REQUEST_BLUETOOTH_PERMISSIONS
            )
        } else {
            Log.d("BluetoothEOG", "All Bluetooth permissions granted.")
            Toast.makeText(this, "All Bluetooth permissions granted.", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to initialize Bluetooth Adapter object
    // Checks whether bluetooth is supported and enabled
    private fun initializeBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Log.e("BluetoothEOG", "Bluetooth not supported on this device.")
            Toast.makeText(this, "Bluetooth not supported on this device.", Toast.LENGTH_SHORT)
                .show()
            return
        }

        if (!bluetoothAdapter!!.isEnabled) {
            Log.e("BluetoothEOG", "Bluetooth is disabled. Please enable it.")
            Toast.makeText(this, "Bluetooth is disabled. Please enable it.", Toast.LENGTH_SHORT)
                .show()
        } else {
            Log.d("BluetoothEOG", "Bluetooth is ON.")
        }
    }


    fun connectToHC05() {

        if (isConnected) {
            disconnectFromHC05()
            return
        }

        initializeBluetooth()

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(
                this, "Permission not granted for Bluetooth connect.", Toast.LENGTH_SHORT
            ).show()
            return
        }

        val deviceName = "HC-05"
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        if (pairedDevices.isNullOrEmpty()) {
            Toast.makeText(this, "No paired devices found", Toast.LENGTH_LONG).show()
            return
        }

        val hc05Device = pairedDevices.find { it.name.equals(deviceName, ignoreCase = true) }
        if (hc05Device == null) {
            Toast.makeText(this, "HC-05 not found. Pair it first.", Toast.LENGTH_SHORT).show()
            return
        }

        thread {
            try {
                runOnUiThread {
                    Toast.makeText(this, "Connecting to HC-05...", Toast.LENGTH_SHORT).show()
                }

                // Trying normal SPP UUID first
                val uuid = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                var socket = hc05Device.createInsecureRfcommSocketToServiceRecord(uuid)

                try {
                    bluetoothAdapter?.cancelDiscovery()
                    socket.connect()
                } catch (e: Exception) {
                    // Fallback using reflection- Manually connecting socket
                    Log.e("BluetoothEOG", "Standard connect failed, retrying via reflection...")
                    val method = hc05Device.javaClass.getMethod(
                        "createRfcommSocket", Int::class.javaPrimitiveType
                    )
                    socket = method.invoke(hc05Device, 1) as BluetoothSocket
                    socket.connect()
                }

                bluetoothSocket = socket
                isConnected = true
                runOnUiThread {
                    Toast.makeText(this, "Connected to HC-05!", Toast.LENGTH_SHORT).show()
                    onConnectionChanged?.invoke(true)
                }
                startReading()
            } catch (e: Exception) {
                Log.e("BluetoothEOG", "Connection failed: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this, "Connection failed: ${e.message}", Toast.LENGTH_LONG)
                        .show()
                    onConnectionChanged?.invoke(false)
                }
            }
        }
    }

    // Bluetooth disconnect function
    fun disconnectFromHC05() {
        thread {
            try {
                isReading = false
                inputStream?.close()
                bluetoothSocket?.close()
                bluetoothSocket = null
                isConnected = false

                runOnUiThread {
                    Toast.makeText(this, "Disconnected from HC-05", Toast.LENGTH_SHORT).show()
                    onConnectionChanged?.invoke(false)
                }
            } catch (e: Exception) {
                Log.e("BluetoothEOG", "Disconnection error: ${e.message}")
            }
        }
    }

    private fun startReading() {
        val socket = bluetoothSocket ?: return
        inputStream = socket.inputStream
        isReading = true

        thread {
            val reader = inputStream?.bufferedReader()

            while (isReading) {
                try {
                    val line = reader?.readLine() ?: break

                    val parts = line.trim().split(",")

                    if (parts.size == 2) {
                        val hVal = parts[0].toFloatOrNull()
                        val vVal = parts[1].toFloatOrNull()

                        if (hVal != null && vVal != null) {
                            runOnUiThread {
                                onDataReceived?.invoke(hVal, vVal)
                            }
                        }
                    }

                } catch (e: Exception) {
                    Log.e("BluetoothEOG", "Read error: ${e.message}")
                    isReading = false
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectFromHC05()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EOGApp() {

    val context = LocalContext.current
    val activity = context as? MainActivity
    val hBuffer = remember { CircularBuffer(400) }
    val vBuffer = remember { CircularBuffer(400) }
    val hValues = remember { mutableStateOf(listOf<Float>()) }
    val vValues = remember { mutableStateOf(listOf<Float>()) }
    val isConnected = remember { mutableStateOf(false) }

    val graphTick = remember { mutableStateOf(0) }

    // Refresh compose every 16ms
    LaunchedEffect(Unit) {
        while (true) {
            delay(16) // 60 FPS refresh
            graphTick.value++ // Force redraw of compose
        }
    }


// Assigning callback to collect data
    LaunchedEffect(activity) {
        activity?.onDataReceived = { h, v ->

            hBuffer.add(h)
            vBuffer.add(v)

            hValues.value = hBuffer.toList()
            vValues.value = vBuffer.toList()
        }

        // Observe connection status
        activity?.onConnectionChanged = { connected ->
            isConnected.value = connected
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            colors = topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.primary,
            ), title = {
                Text("Bluetooth EOG")
            })
    }, bottomBar = {
        BottomAppBar(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = "Neural Engineering Lab | IITG",
            )
        }
    }, floatingActionButton = {
        val context = LocalContext.current
        ExtendedFloatingActionButton(
            onClick = {
                if (isConnected.value) activity?.disconnectFromHC05()
                else activity?.connectToHC05()
            },
            icon = {
                Icon(
                    if (isConnected.value) Icons.Filled.Close else Icons.Filled.Add,
                    "Connect Bluetooth Button"
                )
            },
            text = { Text(if (isConnected.value) "Disconnect" else "Connect") },
        )
    }) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // Live EOG text
            Text(
                text = if (hValues.value.isNotEmpty() && vValues.value.isNotEmpty()) {
                    "H: ${hValues.value.last().toInt()}   V: ${vValues.value.last().toInt()}"
                } else {
                    "Waiting for connection..."
                }, modifier = Modifier.padding(8.dp)
            )

            // Live Graph
            Text("Horizontal Channel")
            LabeledGraph(
                values = hValues.value,
                tick = graphTick.value,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                yLabel = "H (a.u.)",
                isConnected = isConnected.value
            )

            Text("Vertical Channel")
            LabeledGraph(
                values = vValues.value,
                tick = graphTick.value,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                yLabel = "V (a.u.)",
                isConnected = isConnected.value
            )
        }
    }
}

@Composable
fun LabeledGraph(
    values: List<Float>,
    tick: Int,
    modifier: Modifier = Modifier,
    yLabel: String = "EOG (a.u.)",
    isConnected: Boolean = false
) {
    Row(
        modifier = modifier
    ) {
        if (isConnected) {
            // Y-Axis Label
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = yLabel,
                    modifier = Modifier.graphicsLayer(rotationZ = -90f),
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }

        // Graph
        EOGGraph(
            values = values, tick = tick, modifier = Modifier.weight(1f)
        )
    }
}


@Composable
fun EOGGraph(values: List<Float>, tick: Int, modifier: Modifier = Modifier) {

    val safeValues = values.toList()
    val path = remember { Path() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
    ) {

        if (safeValues.size < 2) return@Canvas

        val w = size.width
        val h = size.height

        // Scaling
        val maxVal = safeValues.maxOrNull() ?: 1f
        val minVal = safeValues.minOrNull() ?: -1f
        val range = (maxVal - minVal).takeIf { it != 0f } ?: 1f

        val xStep = w / (safeValues.size - 1)

        // Grid lines
        val gridColor = Color(0xFFCCCCCC)

        val labelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 32f
        }

        // Horizontal grid lines: 5 partitions
        val rows = 5
        for (i in 0..rows) {
            val y = h * i / rows

            drawLine(
                start = Offset(0f, y),
                end = Offset(w, y),
                color = gridColor,
                strokeWidth = 1f
            )

            val value = maxVal - (range / rows) * i
            drawContext.canvas.nativeCanvas.drawText(
                String.format("%.0f", (value / 10).roundToInt() * 10f),
                10f,
                y - 5f,
                labelPaint
            )
        }

        // Vertical grid lines: 8 partitions
        val cols = 8
        for (i in 0..cols) {
            val x = w * i / cols
            drawLine(
                start = Offset(x, 0f),
                end = Offset(x, h),
                color = gridColor,
                strokeWidth = 1f
            )
        }

        // Build waveform path
        path.reset()

        val firstY = h - ((safeValues[0] - minVal) / range) * h
        path.moveTo(0f, firstY)

        for (i in 1 until safeValues.size) {
            val x = i * xStep
            val y = h - ((safeValues[i] - minVal) / range) * h
            path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = Color.Green,
            style = Stroke(
                width = 3f,
                cap = StrokeCap.Round
            )
        )
    }
}


@Preview(showBackground = true)
@Composable
fun EOGAppPreview() {
    BluetoothEOGTheme {
        EOGApp()
    }
}