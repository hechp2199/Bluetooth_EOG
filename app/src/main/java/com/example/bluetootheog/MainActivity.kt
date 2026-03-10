package com.example.bluetootheog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.bluetootheog.bluetooth.BluetoothManager
import com.example.bluetootheog.ui.EOGApp
import com.example.bluetootheog.ui.theme.BluetoothEOGTheme


class MainActivity : ComponentActivity() {

    val bluetoothManager = BluetoothManager(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        bluetoothManager.checkBluetoothPermissions()
        setContent {
            BluetoothEOGTheme {
                EOGApp(bluetoothManager)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothManager.disconnectFromHC05()
    }
}