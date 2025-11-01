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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.bluetootheog.ui.theme.BluetoothEOGTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkBluetoothPermissions()
        setContent {
            BluetoothEOGTheme {
                EOGApp(
                    name = "Android",
                    modifier = Modifier
                )
            }
        }
    }

    private val bluetoothPermissions = arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN
    )

    private val REQUEST_BLUETOOTH_PERMISSIONS = 1

    private fun checkBluetoothPermissions() {
        val missingPermissions = bluetoothPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                REQUEST_BLUETOOTH_PERMISSIONS
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("BluetoothEOG", "Permission not granted for Bluetooth connect.")
            Toast.makeText(
                this,
                "Permission not granted for Bluetooth connect.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val deviceName = "HC-05" // your module’s Bluetooth name
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        if (pairedDevices.isNullOrEmpty()) {
            Toast.makeText(this, "No paired devices found", Toast.LENGTH_LONG).show()
            return
        }

        // 🔹 Display all paired devices in Toast + Logcat
        val deviceList = pairedDevices.joinToString("\n") { "${it.name} (${it.address})" }
        Log.d("BluetoothEOG", "Paired devices:\n$deviceList")
        Toast.makeText(this, "Paired devices:\n$deviceList", Toast.LENGTH_LONG).show()

        val hc05Device = pairedDevices.find { it.name.equals(deviceName, ignoreCase = true) }
        if (hc05Device == null) {
            Log.e("BluetoothEOG", "HC-05 device not found. Pair it first.")
            Toast.makeText(this, "HC-05 device not found. Pair it first.", Toast.LENGTH_SHORT)
                .show()
            return
        }

        // Standard SPP UUID for serial devices
        val uuid = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        try {
            bluetoothSocket = hc05Device.createRfcommSocketToServiceRecord(uuid)
            bluetoothSocket?.connect()
            Log.i("BluetoothEOG", "Connected to HC-05!")
            Toast.makeText(this, "Connected to HC-05!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("BluetoothEOG", "Connection failed: ${e.message}")
            Toast.makeText(this, "Connection failed!", Toast.LENGTH_SHORT).show()
            try {
                bluetoothSocket?.close()
            } catch (closeException: Exception) {
                Log.e("BluetoothEOG", "Error closing socket: ${closeException.message}")
                Toast.makeText(this, "Error closing socket", Toast.LENGTH_SHORT).show()
            }
        }
    }


}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EOGApp(name: String, modifier: Modifier = Modifier) {
    Scaffold(
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text("Bluetooth EOG")
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = "Neural Engineering Lab | IITG",
                )
            }
        },
        floatingActionButton = {
            val context = LocalContext.current
            ExtendedFloatingActionButton(
                onClick = {
                    val activity = context as? MainActivity
                    activity?.connectToHC05()
                },
                icon = { Icon(Icons.Filled.Add, "Connect Bluetooth Button") },
                text = { Text(text = "Connect") },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                modifier = Modifier.padding(8.dp),
                text =
                    """
                    This is an example of a scaffold. It uses the Scaffold composable's parameters to create a screen with a simple top app bar, bottom app bar, and floating action button.

                    It also contains some basic inner content, such as this text.

                    You have pressed the floating action button times.
                """.trimIndent(),
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