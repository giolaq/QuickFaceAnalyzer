package com.huawei.quickfaceanalyzer.processor

import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageProxy
import com.huawei.hms.mlsdk.common.MLException
import com.huawei.quickfaceanalyzer.graphic.GraphicOverlay

/**
 * An interface to process the images
 */
interface VisionImageProcessor {

    /**
     * Processes ImageProxy image data, e.g. used for CameraX live preview case.
     */
    @RequiresApi(VERSION_CODES.KITKAT)
    @Throws(MLException::class)
    fun processImageProxy(image: ImageProxy, graphicOverlay: GraphicOverlay)

    /**
     * Stops the underlying machine learning model and release resources.
     */
    fun stop()
}
