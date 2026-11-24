package com.grey.puzzleai

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.os.Build
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume

class PuzzleAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: PuzzleAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Tidak perlu memproses event UI secara terus-menerus; bekerja saat dipicu
    }

    override fun onInterrupt() {
        // Layanan dihentikan sementara oleh sistem
    }

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    suspend fun captureScreenPng(): ByteArray? = suspendCancellableCoroutine { cont ->
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        takeScreenshot(
            Display.DEFAULT_DISPLAY,
            mainExecutor,
            object : TakeScreenshotCallback {
                override fun onSuccess(screenshot: ScreenshotResult) {
                    try {
                        val hwBuffer = screenshot.hardwareBuffer
                        val colorSpace = screenshot.colorSpace
                        val hwBitmap = Bitmap.wrapHardwareBuffer(hwBuffer, colorSpace)
                        val softBitmap = hwBitmap?.copy(Bitmap.Config.ARGB_8888, false)
                        hwBuffer.close()

                        if (softBitmap == null) {
                            cont.resume(null)
                            return
                        }
                        val out = ByteArrayOutputStream()
                        softBitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                        softBitmap.recycle()
                        cont.resume(out.toByteArray())
                    } catch (_: Exception) {
                        cont.resume(null)
                    }
                }

                override fun onFailure(errorCode: Int) {
                    cont.resume(null)
                }
            }
        )
    }

    suspend fun dragPieceToCell(plan: MovePlan, board: BoardConfig): Boolean {
        val metrics = resources.displayMetrics
        val w = metrics.widthPixels.toFloat()
        val h = metrics.heightPixels.toFloat()

        val startX = (plan.pieceCenterXNorm.coerceIn(0.02f, 0.98f)) * w
        val startY = (plan.pieceCenterYNorm.coerceIn(0.02f, 0.98f)) * h

        val bx1 = board.x1 * w
        val by1 = board.y1 * h
        val bx2 = board.x2 * w
        val by2 = board.y2 * h

        val cellW = (bx2 - bx1) / board.cols.coerceAtLeast(1)
        val cellH = (by2 - by1) / board.rows.coerceAtLeast(1)

        val endX = bx1 + (plan.targetCol + 0.5f) * cellW
        val endY = by1 + (plan.targetRow + 0.5f) * cellH

        return dispatchDrag(startX, startY, endX, endY, 240L)
    }

    private suspend fun dispatchDrag(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        durationMs: Long
    ): Boolean = suspendCancellableCoroutine { cont ->
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val accepted = dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    cont.resume(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    cont.resume(false)
                }
            },
            null
        )
        if (!accepted && cont.isActive) {
            cont.resume(false)
        }
    }
}
