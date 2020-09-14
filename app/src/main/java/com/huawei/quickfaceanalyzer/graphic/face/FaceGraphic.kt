package com.huawei.quickfaceanalyzer.graphic.face

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

import com.huawei.hms.mlsdk.face.MLFace
import com.huawei.hms.mlsdk.face.MLFaceKeyPoint
import com.huawei.quickfaceanalyzer.graphic.GraphicOverlay
import java.util.*
import kotlin.math.abs

/**
 * Graphic instance for rendering face position, contour, and landmarks within the associated
 * graphic overlay view.
 */
class FaceGraphic constructor(overlay: GraphicOverlay, private val face: MLFace) : GraphicOverlay.Graphic(overlay) {
    private val facePositionPaint: Paint
    private val numColors = COLORS.size
    private val idPaints = Array(numColors) { Paint() }
    private val boxPaints = Array(numColors) { Paint() }
    private val labelPaints = Array(numColors) { Paint() }

    init {
        val selectedColor = Color.WHITE
        facePositionPaint = Paint()
        facePositionPaint.color = selectedColor
        for (i in 0 until numColors) {
            idPaints[i] = Paint()
            idPaints[i].color = COLORS[i][0]
            idPaints[i].textSize = ID_TEXT_SIZE
            boxPaints[i] = Paint()
            boxPaints[i].color = COLORS[i][1]
            boxPaints[i].style = Paint.Style.STROKE
            boxPaints[i].strokeWidth = BOX_STROKE_WIDTH
            labelPaints[i] = Paint()
            labelPaints[i].color = COLORS[i][1]
            labelPaints[i].style = Paint.Style.FILL
        }
    }

    /** Draws the face annotations for position on the supplied canvas.  */
    override fun draw(canvas: Canvas) {
        val face = face ?: return
        // Draws a circle at the position of the detected face, with the face's track id below.
        val x = translateX(face.border.centerX().toFloat())
        val y = translateY(face.border.centerY().toFloat())
        canvas.drawCircle(x, y, FACE_POSITION_RADIUS, facePositionPaint)

        // Calculate positions.
        val left = x - scale(face.border.width() / 2.0f)
        val top = y - scale(face.border.height() / 2.0f)
        val right = x + scale(face.border.width() / 2.0f)
        val bottom = y + scale(face.border.height() / 2.0f)
        val lineHeight = ID_TEXT_SIZE + BOX_STROKE_WIDTH
        var yLabelOffset = -lineHeight

        // Decide color based on face ID
        val colorID = abs(face.tracingIdentity % NUM_COLORS)

        // Calculate width and height of label box
        var textWidth = idPaints[colorID].measureText("ID: " + face.tracingIdentity)
        yLabelOffset -= lineHeight
        textWidth =
            textWidth.coerceAtLeast(
                idPaints[colorID].measureText(
                    String.format(
                        Locale.US,
                        "Happiness: %.2f",
                        face.possibilityOfSmiling()
                    )
                )
            )

        if (face.opennessOfLeftEye() > 0) {

            yLabelOffset -= lineHeight
            textWidth =
                textWidth.coerceAtLeast(
                    idPaints[colorID].measureText(
                        String.format(
                            Locale.US,
                            "Left eye: %.2f",
                            face.opennessOfLeftEye()
                        )
                    )
                )
        }

        if (face.opennessOfRightEye() > 0) {
            yLabelOffset -= lineHeight
            textWidth =
                Math.max(
                    textWidth,
                    idPaints[colorID].measureText(
                        String.format(
                            Locale.US,
                            "Right eye: %.2f",
                            face.opennessOfRightEye()
                        )
                    )
                )
        }

        // Draw labels
        canvas.drawRect(
            left - BOX_STROKE_WIDTH,
            top + yLabelOffset,
            left + textWidth + 2 * BOX_STROKE_WIDTH,
            top,
            labelPaints[colorID]
        )
        yLabelOffset += ID_TEXT_SIZE
        canvas.drawRect(left, top, right, bottom, boxPaints[colorID])
        canvas.drawText(
            "ID: " + face.tracingIdentity, left, top + yLabelOffset,
            idPaints[colorID]
        )
        yLabelOffset += lineHeight

        // Draws all face contours.
        for (contour in face.faceShapeList) {
            for (point in contour.coordinatePoints) {
                canvas.drawCircle(
                    translateX(point.x),
                    translateY(point.y),
                    FACE_POSITION_RADIUS,
                    facePositionPaint
                )
            }
        }

        // Draws smiling and left/right eye open probabilities.
        if (face.emotions.smilingProbability > 0) {
            canvas.drawText(
                "Smiling: " + String.format(Locale.US, "%.2f", face.emotions.smilingProbability),
                left,
                top + yLabelOffset,
                idPaints[colorID]
            )
        }

        yLabelOffset += lineHeight

        val leftEye = face.getFaceKeyPoint(MLFaceKeyPoint.TYPE_LEFT_EYE)
        if (leftEye != null && face.opennessOfLeftEye() > 0) {
            canvas.drawText(
                "Left eye open: " + String.format(Locale.US, "%.2f", face.opennessOfLeftEye()),
                translateX(leftEye.point.x) + ID_X_OFFSET,
                translateY(leftEye.point.y) + ID_Y_OFFSET,
                idPaints[colorID]
            )
        } else if (leftEye != null && face.opennessOfLeftEye() < 0) {
            canvas.drawText(
                "Left eye",
                left,
                top + yLabelOffset,
                idPaints[colorID]
            )
            yLabelOffset += lineHeight
        } else if (leftEye == null && face.opennessOfLeftEye() > 0) {
            canvas.drawText(
                "Left eye open: " + String.format(Locale.US, "%.2f", face.opennessOfLeftEye()),
                left,
                top + yLabelOffset,
                idPaints[colorID]
            )
            yLabelOffset += lineHeight
        }
        val rightEye = face.getFaceKeyPoint(MLFaceKeyPoint.TYPE_RIGHT_EYE)
        if (rightEye != null && face.opennessOfRightEye() > 0) {
            canvas.drawText(
                "Right eye open: " + String.format(Locale.US, "%.2f", face.opennessOfRightEye()),
                translateX(rightEye.point.x) + ID_X_OFFSET,
                translateY(rightEye.point.y) + ID_Y_OFFSET,
                idPaints[colorID]
            )
        } else if (rightEye != null && face.opennessOfRightEye() < 0) {
            canvas.drawText(
                "Right eye",
                left,
                top + yLabelOffset,
                idPaints[colorID]
            )
            yLabelOffset += lineHeight
        } else if (rightEye == null && face.opennessOfRightEye() > 0) {
            canvas.drawText(
                "Right eye open: " + String.format(Locale.US, "%.2f", face.opennessOfRightEye()),
                left,
                top + yLabelOffset,
                idPaints[colorID]
            )
        }

        // Draw facial landmarks
        drawFaceLandmark(canvas, MLFaceKeyPoint.TYPE_LEFT_EYE)
        drawFaceLandmark(canvas, MLFaceKeyPoint.TYPE_RIGHT_EYE)
        drawFaceLandmark(canvas, MLFaceKeyPoint.TYPE_LEFT_CHEEK)
        drawFaceLandmark(canvas, MLFaceKeyPoint.TYPE_RIGHT_CHEEK)
    }

    private fun drawFaceLandmark(canvas: Canvas, @MLFaceKeyPoint.Type landmarkType: Int) {
        val faceLandmark = face.getFaceKeyPoint(landmarkType)
        if (faceLandmark != null) {
            canvas.drawCircle(
                translateX(faceLandmark.point.x),
                translateY(faceLandmark.point.y),
                FACE_POSITION_RADIUS,
                facePositionPaint
            )
        }
    }

    companion object {
        private const val FACE_POSITION_RADIUS = 4.0f
        private const val ID_TEXT_SIZE = 30.0f
        private const val ID_Y_OFFSET = 40.0f
        private const val ID_X_OFFSET = -40.0f
        private const val BOX_STROKE_WIDTH = 5.0f
        private const val NUM_COLORS = 10
        private val COLORS =
            arrayOf(
                intArrayOf(Color.BLACK, Color.WHITE),
                intArrayOf(Color.WHITE, Color.MAGENTA),
                intArrayOf(Color.BLACK, Color.LTGRAY),
                intArrayOf(Color.WHITE, Color.RED),
                intArrayOf(Color.WHITE, Color.BLUE),
                intArrayOf(Color.WHITE, Color.DKGRAY),
                intArrayOf(Color.BLACK, Color.CYAN),
                intArrayOf(Color.BLACK, Color.YELLOW),
                intArrayOf(Color.WHITE, Color.BLACK),
                intArrayOf(Color.BLACK, Color.GREEN)
            )
    }
}
