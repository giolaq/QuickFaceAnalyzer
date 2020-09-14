package com.huawei.quickfaceanalyzer.graphic

import android.graphics.Bitmap
import android.graphics.Canvas
import com.huawei.quickfaceanalyzer.graphic.GraphicOverlay.Graphic

class CameraImageGraphic(overlay: GraphicOverlay, private val bitmap: Bitmap) : Graphic(overlay) {
    override fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, getTransformationMatrix(), null)
    }
}
