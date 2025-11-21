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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
    var onDataReceived: ((Float) -> Unit)? = null
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
            val buffer = ByteArray(1024)
            var raw = ""

            while (isReading) {
                try {
                    val bytes = inputStream!!.read(buffer)
                    raw += String(buffer, 0, bytes)

                    val lines = raw.split("\n")
                    raw = lines.last()

                    for (line in lines.dropLast(1)) {
                        line.trim().toFloatOrNull()?.let { value ->
                            onDataReceived?.invoke(value)
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
    val eogValues = remember { mutableStateListOf<Float>() }
    val isConnected = remember { mutableStateOf(false) }

    val graphTick = remember { mutableStateOf(0) }

    // Refresh compose every 16ms
    LaunchedEffect(Unit) {
        while (true) {
            delay(16) // 60 FPS refresh
            graphTick.value++ // Force redraw of compose
        }
    }


// assign callback to collect data
    activity?.onDataReceived = { value ->
        eogValues.add(value)
        if (eogValues.size > 300) { // keep last ~3 seconds @100Hz
            eogValues.removeAt(0)
        }
    }

    // Observe connection status
    activity?.onConnectionChanged = { connected ->
        isConnected.value = connected
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
                text = if (eogValues.isNotEmpty()) "EOG Amplitude: ${eogValues.last()}" else "Waiting for connection...",
                modifier = Modifier.padding(8.dp)
            )

            // Live Graph
            LabeledGraph(
                values = eogValues,
                tick = graphTick.value,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .padding(2.dp),
                yLabel = "EOG (a.u.)",
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
            values = values,
            tick = tick,
            modifier = Modifier.weight(1f)
        )
    }
}


@Composable
fun EOGGraph(values: List<Float>, tick: Int, modifier: Modifier = Modifier) {

    val safeValues = values.toList() // Taking fresh snapshot of EOG values

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

        // Adding grid lines to the graph
        val gridColor = Color(0xFFCCCCCC)
        val textColor = Color.White
        val labelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 32f
        }

        // Horizontal grid lines: 5 partitions
        val rows = 5
        for (i in 0..rows) {
            val y = h * i / rows
            drawLine(
                start = Offset(0f, y), end = Offset(w, y), color = gridColor, strokeWidth = 1f
            )

            // Y-axis labels
            val value = maxVal - (range / rows) * i
            drawContext.canvas.nativeCanvas.drawText(
                String.format("%.0f", (value / 10).roundToInt() * 10f), 10f, y - 5f, labelPaint
            )
        }

        // Vertical grid lines: 8 partitions
        val cols = 8
        for (i in 0..cols) {
            val x = w * i / cols
            drawLine(
                start = Offset(x, 0f), end = Offset(x, h), color = gridColor, strokeWidth = 1f
            )
        }

        // Drawing the signal line
        var prevX = 0f
        var prevY = h - ((safeValues[0] - minVal) / range) * h

        for (i in 1 until safeValues.size) {
            val x = i * xStep
            val y = h - ((safeValues[i] - minVal) / range) * h

            drawLine(
                color = Color.Green,
                start = Offset(prevX, prevY),
                end = Offset(x, y),
                strokeWidth = 3f
            )

            prevX = x
            prevY = y
        }
    }
}


@Preview(showBackground = true)
@Composable
fun EOGAppPreview() {
    BluetoothEOGTheme {
        EOGApp()
    }
}