package net.runner.rize

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView

@SuppressLint("MissingPermission")
fun openCamera(context: Context, textureView: TextureView, onImageCaptured: (Bitmap) -> Unit) {
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val cameraId = cameraManager.cameraIdList[0]
    val handlerThread = HandlerThread("CameraThread").apply { start() }
    val handler = Handler(handlerThread.looper)

    cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            val surfaceTexture = textureView.surfaceTexture
            val surface = Surface(surfaceTexture)
            val requestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            requestBuilder.addTarget(surface)

            camera.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    session.setRepeatingRequest(requestBuilder.build(), null, handler)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}
            }, handler)
        }

        override fun onDisconnected(camera: CameraDevice) {}

        override fun onError(camera: CameraDevice, error: Int) {}
    }, handler)
}