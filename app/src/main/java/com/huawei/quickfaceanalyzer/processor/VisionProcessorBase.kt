package com.huawei.quickfaceanalyzer.processor

import android.app.ActivityManager
import android.content.Context
import android.os.Build.VERSION_CODES
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.huawei.hmf.tasks.OnSuccessListener
import com.huawei.hmf.tasks.Task
import com.huawei.hmf.tasks.TaskExecutors
import com.huawei.hms.mlsdk.common.MLFrame
import com.huawei.hms.mlsdk.common.MLFrame.*
import com.huawei.quickfaceanalyzer.utils.ScopedExecutor
import com.huawei.quickfaceanalyzer.data.FrameMetadata
import com.huawei.quickfaceanalyzer.graphic.GraphicOverlay
import com.huawei.quickfaceanalyzer.graphic.InferenceInfoGraphic
import java.nio.ByteBuffer
import java.util.*

abstract class VisionProcessorBase<T>(context: Context) : VisionImageProcessor {

    companion object {
        const val MANUAL_TESTING_LOG = "LogTagForTest"
        private const val TAG = "VisionProcessorBase"
    }

    private var activityManager: ActivityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val fpsTimer = Timer()
    private val executor = ScopedExecutor(TaskExecutors.uiThread())

    // Whether this processor is already shut down
    private var isShutdown = false

    // Used to calculate latency, running in the same thread, no sync needed.
    private var numRuns = 0
    private var totalRunMs: Long = 0
    private var maxRunMs: Long = 0
    private var minRunMs = Long.MAX_VALUE

    // Frame count that have been processed so far in an one second interval to calculate FPS.
    private var frameProcessedInOneSecondInterval = 0
    private var framesPerSecond = 0

    // To keep the latest images and its metadata.
    @GuardedBy("this")
    private var latestImage: ByteBuffer? = null

    @GuardedBy("this")
    private var latestImageMetaData: FrameMetadata? = null

    // To keep the images and metadata in process.
    @GuardedBy("this")
    private var processingImage: ByteBuffer? = null

    @GuardedBy("this")
    private var processingMetaData: FrameMetadata? = null

    init {
        fpsTimer.scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                    framesPerSecond = frameProcessedInOneSecondInterval
                    frameProcessedInOneSecondInterval = 0
                }
            },
            0,
            1000
        )
    }

    // -----------------Code for processing live preview frame from CameraX API-----------------------
    @RequiresApi(VERSION_CODES.KITKAT)
    @ExperimentalGetImage
    override fun processImageProxy(image: ImageProxy, graphicOverlay: GraphicOverlay) {
        if (isShutdown) {
            return
        }

        val quadrant = convertRotationFormatToQuadrant(image.imageInfo.rotationDegrees)

        val frame = MLFrame.fromMediaImage(image.image, quadrant)
        frame.byteBuffer

        requestDetectInImage(
            MLFrame.fromMediaImage(
                image.image,
                quadrant
            ),
            graphicOverlay,
            true
        )
            // When the image is from CameraX analysis use case, must call image.close() on received
            // images when finished using them. Otherwise, new images may not be received or the camera
            // may stall.
            .addOnCompleteListener { image.close() }
    }


    private fun convertRotationFormatToQuadrant(rotationDegrees: Int) = when (rotationDegrees) {
        0 -> SCREEN_FIRST_QUADRANT
        90 -> SCREEN_SECOND_QUADRANT
        180 -> SCREEN_THIRD_QUADRANT
        270 -> SCREEN_FOURTH_QUADRANT
        else -> -1
    }

    // -----------------Common processing logic-------------------------------------------------------
    private fun requestDetectInImage(
        image: MLFrame,
        graphicOverlay: GraphicOverlay,
        shouldShowFps: Boolean
    ): Task<T> {
        val startMs = SystemClock.elapsedRealtime()
        return detectInImage(image).addOnSuccessListener(executor, OnSuccessListener { results: T ->
            val currentLatencyMs = SystemClock.elapsedRealtime() - startMs
            numRuns++
            frameProcessedInOneSecondInterval++
            totalRunMs += currentLatencyMs
            maxRunMs = currentLatencyMs.coerceAtLeast(maxRunMs)
            minRunMs = currentLatencyMs.coerceAtMost(minRunMs)
            // Only log inference info once per second. When frameProcessedInOneSecondInterval is
            // equal to 1, it means this is the first frame processed during the current second.
            if (frameProcessedInOneSecondInterval == 1) {
                Log.d(TAG, "Max latency is: $maxRunMs")
                Log.d(TAG, "Min latency is: $minRunMs")
                Log.d(
                    TAG,
                    "Num of Runs: " + numRuns + ", Avg latency is: " + totalRunMs / numRuns
                )
                val mi = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(mi)
                val availableMegs = mi.availMem / 0x100000L
                Log.d(
                    TAG,
                    "Memory available in system: $availableMegs MB"
                )
            }
            graphicOverlay.clear()

            this@VisionProcessorBase.onSuccess(results, graphicOverlay)
            graphicOverlay.add(
                InferenceInfoGraphic(
                    graphicOverlay,
                    currentLatencyMs.toDouble(),
                    if (shouldShowFps) framesPerSecond else null
                )
            )
            graphicOverlay.postInvalidate()
        })
            .addOnFailureListener(executor, { e: Exception ->
                graphicOverlay.clear()
                graphicOverlay.postInvalidate()
                Toast.makeText(
                    graphicOverlay.context,
                    "Failed to process.\nError: " +
                            e.localizedMessage +
                            "\nCause: " +
                            e.cause,
                    Toast.LENGTH_LONG
                )
                    .show()
                e.printStackTrace()
                this@VisionProcessorBase.onFailure(e)
            })
    }

    override fun stop() {
        executor.shutdown()
        isShutdown = true
        numRuns = 0
        totalRunMs = 0
        fpsTimer.cancel()
    }

    protected abstract fun detectInImage(image: MLFrame): Task<T>

    protected abstract fun onSuccess(results: T, graphicOverlay: GraphicOverlay)

    protected abstract fun onFailure(e: Exception)
}
