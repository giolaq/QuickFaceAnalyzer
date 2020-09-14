package com.huawei.quickfaceanalyzer.graphic

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.huawei.quickfaceanalyzer.graphic.GraphicOverlay.Graphic

class InferenceInfoGraphic(
    private val overlay: GraphicOverlay,
    private val latency: Double, // Only valid when a stream of input images is being processed. Null for single image mode.
    private val framesPerSecond: Int?
) : Graphic(overlay) {
    private val textPaint: Paint = Paint()

    init {
        textPaint.color = TEXT_COLOR
        textPaint.textSize = TEXT_SIZE
        postInvalidate()
    }

    @Synchronized
    override fun draw(canvas: Canvas) {
        val x = TEXT_SIZE * 0.5f
        val y = TEXT_SIZE * 1.5f
        canvas.drawText(
            "InputImage size: " + overlay.imageWidth + "x" + overlay.imageHeight,
            x,
            y,
            textPaint
        )

        // Draw FPS (if valid) and inference latency
        if (framesPerSecond != null) {
            canvas.drawText(
                "FPS: $framesPerSecond, latency: $latency ms", x, y + TEXT_SIZE, textPaint
            )
        } else {
            canvas.drawText("Latency: $latency ms", x, y + TEXT_SIZE, textPaint)
        }
    }

    companion object {
        private const val TEXT_COLOR = Color.WHITE
        private const val TEXT_SIZE = 60.0f
    }

}
