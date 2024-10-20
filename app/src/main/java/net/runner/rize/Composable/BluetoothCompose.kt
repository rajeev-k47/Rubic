package net.runner.rize.Composable

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@SuppressLint("MissingPermission")
@Composable
fun SelectBluetoothDeviceDialog(devices: List<BluetoothDevice>, onDeviceSelected: (BluetoothDevice) -> Unit) {
    var openDialog by remember { mutableStateOf(true) }

    if (openDialog) {
        AlertDialog(
            onDismissRequest = { openDialog = false },
            title = { Text("Select Bluetooth Device") },
            confirmButton = {
                TextButton(onClick = { openDialog = false }) {
                    Text("Cancel")
                }
            },
            text = {
                Column {
                    devices.forEach { device ->
                        TextButton(onClick = {
                            onDeviceSelected(device)
                            openDialog = false
                        }) {
                            Text(device.name)
                        }
                    }
                    if (devices.isEmpty()) {
                        Text("No devices available")
                    }
                }
            }
        )
    }
}