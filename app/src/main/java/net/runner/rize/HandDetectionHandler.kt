package net.runner.rize

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

@Composable
fun drawHandLandmarks(canvas: Canvas, handResults: HandLandmarkerResult, rectangle: RectF) {
    val paint = Paint().apply {
        color = Color.BLUE
        strokeWidth = 5f
        style = Paint.Style.FILL
    }

    handResults.landmarks().forEach { handLandmarks ->
        val indexFingerTip = handLandmarks[8]

        if (indexFingerTip != null) {
            val x = indexFingerTip.x() * canvas.width
            val y = indexFingerTip.y() * canvas.height

            canvas.drawCircle(x, y, 10f, paint)

            val centerX = (rectangle.left + rectangle.right) / 2
            val centerY = (rectangle.top + rectangle.bottom) / 2

            println("x: $x, y: $y, centerX: $centerX, centerY: $centerY")


        }
    }
}