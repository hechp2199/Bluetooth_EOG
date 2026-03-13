package com.example.bluetootheog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.bluetootheog.bluetooth.BluetoothManager
import com.example.bluetootheog.repository.EOGRepository
import com.example.bluetootheog.ui.EOGApp
import com.example.bluetootheog.ui.theme.BluetoothEOGTheme


class MainActivity : ComponentActivity() {

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var repository: EOGRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        bluetoothManager = BluetoothManager(this)
        repository = EOGRepository(bluetoothManager)
        bluetoothManager.checkBluetoothPermissions()
        setContent {
            BluetoothEOGTheme {
                EOGApp(bluetoothManager, repository)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothManager.disconnectFromHC05()
    }
}