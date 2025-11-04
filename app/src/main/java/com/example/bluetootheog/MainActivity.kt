package com.example.bluetootheog

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
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
    //private var bluetoothSocket: BluetoothSocket? = null

//    private fun initializeBluetooth() {
//        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
//        if (bluetoothAdapter == null) {
//            Log.e("BluetoothEOG", "Bluetooth not supported on this device.")
//            Toast.makeText(this, "Bluetooth not supported on this device.", Toast.LENGTH_SHORT)
//                .show()
//            return
//        }
//
//        if (!bluetoothAdapter!!.isEnabled) {
//            Log.e("BluetoothEOG", "Bluetooth is disabled. Please enable it.")
//            Toast.makeText(this, "Bluetooth is disabled. Please enable it.", Toast.LENGTH_SHORT)
//                .show()
//        } else {
//            Log.d("BluetoothEOG", "Bluetooth is ON.")
//        }
//    }

    fun connectToBleDevice() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
            return
        }

        if (!bluetoothAdapter!!.isEnabled) {
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ Permission check
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                1001
            )
            return
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                2001
            )
            return
        }


        val scanner = bluetoothAdapter!!.bluetoothLeScanner ?: run {
            Toast.makeText(this, "BLE Scanner not available", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Scanning for BLE devices...", Toast.LENGTH_SHORT).show()

//        val scanCallback = object : ScanCallback() {
//            override fun onScanResult(callbackType: Int, result: ScanResult?) {
//                result?.let {
//                    val device = it.device
//
//                    // ✅ Ensure permission before accessing device.name
//                    if (ActivityCompat.checkSelfPermission(
//                            this@MainActivity,
//                            Manifest.permission.BLUETOOTH_SCAN
//                        ) != PackageManager.PERMISSION_GRANTED
//                    ) {
//                        ActivityCompat.requestPermissions(
//                            this@MainActivity,
//                            arrayOf(Manifest.permission.BLUETOOTH_SCAN),
//                            1001
//                        )
//                        return
//                    }
//                    Log.d("SCAN", "Device found: name=${device.name}, address=${device.address}")
//
////                    if (device.name != null && device.name.contains("HC", ignoreCase = true)) {
////                        Toast.makeText(
////                            this@MainActivity,
////                            "Found ${device.name}, connecting...",
////                            Toast.LENGTH_SHORT
////                        ).show()
////
////                        // ✅ Ensure permission before stopScan
////                        if (ActivityCompat.checkSelfPermission(
////                                this@MainActivity,
////                                Manifest.permission.BLUETOOTH_SCAN
////                            ) == PackageManager.PERMISSION_GRANTED
////                        ) {
////                            scanner.stopScan(this)
////                        }
////
////                        connectGattDevice(device)
////                    }
//                }
//            }
//        }

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device

                // ✅ Permission check before accessing name/address
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) != PackageManager.PERMISSION_GRANTED
                ) return

                Log.d("SCAN", "Device found: name=${device.name}, address=${device.address}")

                Toast.makeText(
                    this@MainActivity,
                    "Found ${device.address}",
                    Toast.LENGTH_SHORT
                ).show()

                // For now, do NOT auto-connect — just list devices
            }
        }



        scanner.startScan(scanCallback)
    }

    private fun connectGattDevice(device: BluetoothDevice) {
        // ✅ Permission check
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                1002
            )
            return
        }

        Toast.makeText(this, "Connecting to ${device.name}...", Toast.LENGTH_SHORT).show()

        device.connectGatt(this, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "✅ BLE connected!", Toast.LENGTH_SHORT).show()
                }
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    gatt.discoverServices()
                } else {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                        1002
                    )
                    return
                }

            } else {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "❌ BLE disconnected", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            for (service in gatt.services) {
                Log.d("BLE", "Service: ${service.uuid}")
                for (char in service.characteristics) {
                    Log.d("BLE", "Characteristic: ${char.uuid}")
                }
            }

            runOnUiThread {
                Toast.makeText(this@MainActivity, "✅ Services discovered", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }


//    fun connectToHC05() {
//        initializeBluetooth()
//
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
//            != PackageManager.PERMISSION_GRANTED
//        ) {
//            Toast.makeText(this, "Permission not granted for Bluetooth connect.", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        val deviceName = "HC-05"
//        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
//        if (pairedDevices.isNullOrEmpty()) {
//            Toast.makeText(this, "No paired devices found", Toast.LENGTH_LONG).show()
//            return
//        }
//
//        val hc05Device = pairedDevices.find { it.name.equals(deviceName, ignoreCase = true) }
//        if (hc05Device == null) {
//            Toast.makeText(this, "HC-05 not found. Pair it first.", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        thread {
//            try {
//                runOnUiThread {
//                    Toast.makeText(this, "Connecting to HC-05...", Toast.LENGTH_SHORT).show()
//                }
//
//                // Try normal SPP UUID first
//                val uuid = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
//                var socket = hc05Device.createInsecureRfcommSocketToServiceRecord(uuid)
//
//                try {
//                    bluetoothAdapter?.cancelDiscovery()
//                    socket.connect()
//                } catch (e: Exception) {
//                    // Fallback using reflection (some clones need this)
//                    Log.e("BluetoothEOG", "Standard connect failed, retrying via reflection...")
//                    val method = hc05Device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
//                    socket = method.invoke(hc05Device, 1) as BluetoothSocket
//                    socket.connect()
//                }
//
//                bluetoothSocket = socket
//                runOnUiThread {
//                    Toast.makeText(this, "✅ Connected to HC-05!", Toast.LENGTH_SHORT).show()
//                }
//            } catch (e: Exception) {
//                Log.e("BluetoothEOG", "Connection failed: ${e.message}")
//                runOnUiThread {
//                    Toast.makeText(this, "Connection failed: ${e.message}", Toast.LENGTH_LONG).show()
//                }
//            }
//        }
//    }


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
                    activity?.connectToBleDevice()
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