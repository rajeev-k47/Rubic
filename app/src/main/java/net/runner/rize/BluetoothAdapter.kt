package net.runner.rize

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class BluetoothServiceController (){
    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice, context: Context) {
        val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        socket = device.createRfcommSocketToServiceRecord(uuid)
        try {
            socket?.connect()
            outputStream = socket?.outputStream
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    fun sendCoordinates(x: Float, y: Float) {
        val message = "$x,$y"
        try {
            outputStream?.write(message.toByteArray())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun disconnect() {
        try {
            outputStream?.close()
            socket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
