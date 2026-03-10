package com.example.bluetootheog.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bluetootheog.bluetooth.BluetoothManager
import com.example.bluetootheog.model.CircularBuffer
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EOGApp(bluetoothManager: BluetoothManager) {

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
    LaunchedEffect(bluetoothManager) {
        bluetoothManager.onDataReceived = { h, v ->

            hBuffer.add(h)
            vBuffer.add(v)

            hValues.value = hBuffer.toList()
            vValues.value = vBuffer.toList()
        }

        // Observe connection status
        bluetoothManager.onConnectionChanged = { connected ->
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
        ExtendedFloatingActionButton(
            onClick = {
                if (isConnected.value) bluetoothManager.disconnectFromHC05()
                else bluetoothManager.connectToHC05()
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