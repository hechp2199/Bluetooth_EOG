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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.bluetootheog.ui.theme.BluetoothEOGTheme
import java.io.InputStream
import kotlin.concurrent.thread


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkBluetoothPermissions()
        setContent {
            BluetoothEOGTheme {
                EOGApp(
                    name = "Android", modifier = Modifier
                )
            }
        }
    }

    private var inputStream: InputStream? = null
    var onDataReceived: ((Float) -> Unit)? = null
    private var isReading = false

    private val bluetoothPermissions = arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN
    )

    private val REQUEST_BLUETOOTH_PERMISSIONS = 1

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

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null

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

                // Try normal SPP UUID first
                val uuid = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                var socket = hc05Device.createInsecureRfcommSocketToServiceRecord(uuid)

                try {
                    bluetoothAdapter?.cancelDiscovery()
                    socket.connect()
                } catch (e: Exception) {
                    // Fallback using reflection (some clones need this)
                    Log.e("BluetoothEOG", "Standard connect failed, retrying via reflection...")
                    val method = hc05Device.javaClass.getMethod(
                        "createRfcommSocket", Int::class.javaPrimitiveType
                    )
                    socket = method.invoke(hc05Device, 1) as BluetoothSocket
                    socket.connect()
                }

                bluetoothSocket = socket
                runOnUiThread {
                    Toast.makeText(this, "✅ Connected to HC-05!", Toast.LENGTH_SHORT).show()
                }
                startReading()
            } catch (e: Exception) {
                Log.e("BluetoothEOG", "Connection failed: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this, "Connection failed: ${e.message}", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
    }

    fun startReading() {
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

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EOGApp(name: String, modifier: Modifier = Modifier) {

    val context = LocalContext.current
    val activity = context as? MainActivity

    val eogValues = remember { mutableStateListOf<Float>() }

// assign callback to collect data
    activity?.onDataReceived = { value ->
        eogValues.add(value)
        if (eogValues.size > 300) { // keep last ~3 seconds @100Hz
            eogValues.removeAt(0)
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
                val activity = context as? MainActivity
                activity?.connectToHC05()
            },
            icon = { Icon(Icons.Filled.Add, "Connect Bluetooth Button") },
            text = { Text(text = "Connect") },
        )
    }) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = if (eogValues.isNotEmpty()) "EOG: ${eogValues.last()}" else "Waiting...",
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EOGAppPreview() {
    BluetoothEOGTheme {
        EOGApp("Android")
    }
}