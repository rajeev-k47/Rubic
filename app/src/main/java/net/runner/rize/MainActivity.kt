package net.runner.rize

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import net.runner.rize.Composable.CameraPreview
import net.runner.rize.Composable.SelectBluetoothDeviceDialog
import net.runner.rize.ml.Model
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private lateinit var model: Model
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var handLandmarker: HandLandmarker
    private lateinit var cameraManager: CameraManager
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private val REQUEST_ENABLE_BT = 1
    private var pairedDevices: Set<BluetoothDevice>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = Model.newInstance(this)
        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        getPermissions()


        val baseOptionsBuilder = BaseOptions.builder().setModelAssetPath("hand_landmarker.task")
        val baseOptions = baseOptionsBuilder.build()

        val optionsBuilder =
            HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinHandDetectionConfidence(0.7f)
                .setMinTrackingConfidence(0.8f)
                .setMinHandPresenceConfidence(0.8f)
                .setNumHands(1)
                .setRunningMode(RunningMode.VIDEO)

        val options = optionsBuilder.build()
        handLandmarker = HandLandmarker.createFromOptions(this, options)


        setContent {
            MyCameraApp()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        model.close()
        handLandmarker.close()
        cameraExecutor.shutdown()
        bluetoothSocket?.close()
    }

    @SuppressLint("MissingPermission")
    private fun setupBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Log.e("Bluetooth", "Device Error")
        } else {
            if (!bluetoothAdapter!!.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
            pairedDevices = bluetoothAdapter?.bondedDevices
        }
    }

    @SuppressLint("MissingPermission")
    @Composable
    fun MyCameraApp() {
        val context = LocalContext.current
        var bitmap by remember { mutableStateOf<Bitmap?>(null) }
        var pairedDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
        var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }

        // Update paired devices when Bluetooth is set up
        LaunchedEffect(Unit) {
            setupBluetooth()
            pairedDevices = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
            Log.d("Bluetooth", "Paired devices: ${pairedDevices.joinToString(", ") { it.name } ?: "None"}")

        }

        Box(modifier = Modifier.fillMaxSize()) {
            CameraPreview { bmp ->
                bitmap = bmp
            }
            bitmap?.let {
                ObjectDetectionOverlay(it)
            }
            if (pairedDevices.isNotEmpty()) {
                SelectBluetoothDeviceDialog(pairedDevices) { device ->
                    selectedDevice = device
                    BluetoothServiceController().connect(device,context)
                }
            } else {
                Text("No paired devices found", modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    @Composable
    fun ObjectDetectionOverlay(bitmap: Bitmap) {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val paint = Paint().apply {
            color = Color.RED
            strokeWidth = 4f
            style = Paint.Style.STROKE
        }

        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        val image = TensorImage.fromBitmap(bitmap)
        val processedImage = imageProcessor.process(image)

        val outputs = model.process(processedImage)
        val locations = outputs.locationsAsTensorBuffer.floatArray
        val classes = outputs.classesAsTensorBuffer.floatArray
        val scores = outputs.scoresAsTensorBuffer.floatArray
        val labels = FileUtil.loadLabels(LocalContext.current, "labels.txt")

        var colors = listOf(
            Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY, Color.BLACK,
            Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED)
        val h = mutableBitmap.height
        val w = mutableBitmap.width
        paint.textSize = h/15f
        paint.strokeWidth = h/85f
        var x = 0

        scores.forEachIndexed { index, fl ->
            if (fl > 0.5f && (labels.get(classes.get(index).toInt()) =="laptop"||labels.get(classes.get(index).toInt()) =="tv")) {
                x = index
                x *= 4
                paint.setColor(colors.get(index))
                paint.style = Paint.Style.STROKE
                canvas.drawRect(RectF((locations.get(x+1)*w).toInt().toFloat(), locations.get(x)* h.toFloat(), locations.get(x+3)* w.toFloat(), locations.get(x+2)*h.toInt().toFloat()), paint)
                paint.style = Paint.Style.FILL
//                Log.d("paint","${locations.get(x)*h.toInt().toFloat()}" )
//                canvas.drawText(labels.get(classes.get(index).toInt())+" "+fl.toString(), locations.get(x+1)*w, locations.get(x)*h, paint)
                canvas.drawText("${(locations.get(x+1)*w-locations.get(x+3)*w).toInt()}, ${(locations.get(x)*h-locations.get(x+2)*h).toInt()}", locations.get(x+1)*w, locations.get(x)*h, paint)
            }
        }
        val argb8888Frame = if (mutableBitmap.config == Bitmap.Config.ARGB_8888) mutableBitmap else mutableBitmap.copy(Bitmap.Config.ARGB_8888, false)
        val mpImage = BitmapImageBuilder(argb8888Frame).build()
        val handResults = handLandmarker.detectForVideo(mpImage, System.currentTimeMillis())
        drawHandLandmarks(canvas, handResults,RectF((locations.get(x+1)*w).toInt().toFloat(), locations.get(x)*h.toInt().toFloat(), locations.get(x+3)*w.toInt().toFloat(), locations.get(x+2)*h.toInt().toFloat()))

        Image(
            bitmap = mutableBitmap.asImageBitmap(),
            contentDescription = "Detected Objects",
            modifier = Modifier.fillMaxSize()
        )
    }

    private fun getPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
        if (permissions.all{ checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED}) {
            setupBluetooth()
            return
        }
        val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionsMap ->
            if (permissionsMap.all { it.value }) {
                setupBluetooth()
            } else {
//                Log.e("Permissions", "permissions are not granted.")
            }
        }
        permissionLauncher.launch(permissions)
    }
}
