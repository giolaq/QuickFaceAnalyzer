package com.huawei.quickfaceanalyzer.processor.face

import android.content.Context
import android.util.Log
import com.huawei.hmf.tasks.Task
import com.huawei.hms.mlsdk.MLAnalyzerFactory
import com.huawei.hms.mlsdk.common.MLFrame
import com.huawei.hms.mlsdk.face.MLFace
import com.huawei.hms.mlsdk.face.MLFaceAnalyzer
import com.huawei.hms.mlsdk.face.MLFaceAnalyzerSetting
import com.huawei.hms.mlsdk.face.MLFaceKeyPoint
import com.huawei.quickfaceanalyzer.graphic.GraphicOverlay
import com.huawei.quickfaceanalyzer.graphic.face.FaceGraphic
import com.huawei.quickfaceanalyzer.processor.VisionProcessorBase
import java.util.*

/** Face Detector Demo.  */
class FaceDetectorProcessor(context: Context, detectorOptions: MLFaceAnalyzerSetting?) :
    VisionProcessorBase<List<MLFace>>(context) {

    private val detector: MLFaceAnalyzer

    init {

        val options = detectorOptions
            ?: MLFaceAnalyzerSetting.Factory()
                .setFeatureType(MLFaceAnalyzerSetting.TYPE_SPEED)
                .setShapeType(MLFaceAnalyzerSetting.TYPE_SHAPES)
                .setKeyPointType(MLFaceAnalyzerSetting.TYPE_KEYPOINTS)
                .allowTracing()
                .create()

        detector = MLAnalyzerFactory.getInstance().getFaceAnalyzer(options)

        Log.v(MANUAL_TESTING_LOG, "Face detector options: $options")
    }

    override fun stop() {
        super.stop()
        detector.stop()
    }

    override fun detectInImage(image: MLFrame): Task<List<MLFace>> {
        return detector.asyncAnalyseFrame(image)
    }

    override fun onSuccess(faces: List<MLFace>, graphicOverlay: GraphicOverlay) {
        for (face in faces) {
            graphicOverlay.add(FaceGraphic(graphicOverlay, face))
            logExtrasForTesting(face)
        }
    }

    override fun onFailure(e: Exception) {
        Log.e(TAG, "Face detection failed $e")
    }

    companion object {
        private const val TAG = "FaceDetectorProcessor"
        private fun logExtrasForTesting(face: MLFace?) {
            if (face != null) {
                Log.v(
                    MANUAL_TESTING_LOG,
                    "face bounding box: " + face.border.flattenToString()
                )
                Log.v(
                    MANUAL_TESTING_LOG,
                    "face Euler Angle X: " + face.rotationAngleX
                )
                Log.v(
                    MANUAL_TESTING_LOG,
                    "face Euler Angle Y: " + face.rotationAngleY
                )
                Log.v(
                    MANUAL_TESTING_LOG,
                    "face Euler Angle Z: " + face.rotationAngleZ
                )
                // All landmarks
                val landMarkTypes = intArrayOf(
                    MLFaceKeyPoint.TYPE_BOTTOM_OF_MOUTH,
                    MLFaceKeyPoint.TYPE_RIGHT_SIDE_OF_MOUTH,
                    MLFaceKeyPoint.TYPE_LEFT_SIDE_OF_MOUTH,
                    MLFaceKeyPoint.TYPE_RIGHT_EYE,
                    MLFaceKeyPoint.TYPE_LEFT_EYE,
                    MLFaceKeyPoint.TYPE_RIGHT_EAR,
                    MLFaceKeyPoint.TYPE_LEFT_EAR,
                    MLFaceKeyPoint.TYPE_RIGHT_CHEEK,
                    MLFaceKeyPoint.TYPE_LEFT_CHEEK,
                    MLFaceKeyPoint.TYPE_TIP_OF_NOSE
                )
                val landMarkTypesStrings = arrayOf(
                    "MOUTH_BOTTOM",
                    "MOUTH_RIGHT",
                    "MOUTH_LEFT",
                    "RIGHT_EYE",
                    "LEFT_EYE",
                    "RIGHT_EAR",
                    "LEFT_EAR",
                    "RIGHT_CHEEK",
                    "LEFT_CHEEK",
                    "NOSE_BASE"
                )
                for (i in landMarkTypes.indices) {
                    val landmark = face.getFaceKeyPoint(landMarkTypes[i])
                    if (landmark == null) {
                        Log.v(
                            MANUAL_TESTING_LOG,
                            "No landmark of type: " + landMarkTypesStrings[i] + " has been detected"
                        )
                    } else {
                        val landmarkPosition = landmark.point
                        val landmarkPositionStr =
                            String.format(Locale.US, "x: %f , y: %f", landmarkPosition.x, landmarkPosition.y)
                        Log.v(
                            MANUAL_TESTING_LOG,
                            "Position for face landmark: " +
                                landMarkTypesStrings[i] +
                                " is :" +
                                landmarkPositionStr
                        )
                    }
                }
                Log.v(
                    MANUAL_TESTING_LOG,
                    "face left eye open probability: " + face.opennessOfLeftEye()
                )
                Log.v(
                    MANUAL_TESTING_LOG,
                    "face right eye open probability: " + face.opennessOfRightEye()
                )
                Log.v(
                    MANUAL_TESTING_LOG,
                    "face smiling probability: " + face.possibilityOfSmiling()
                )
                Log.v(
                    MANUAL_TESTING_LOG,
                    "face tracking id: " + face.tracingIdentity
                )
            }
        }
    }
}
