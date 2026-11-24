package com.grey.puzzleai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class OverlayControlService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var autoJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        showFloatingControls()
    }

    private fun startForegroundNotification() {
        val channelId = "grey_puzzle_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "GREY Puzzle AI Overlay",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
        val notif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("GREY Puzzle AI Aktif")
                .setContentText("Kontrol melayang siap di atas game.")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("GREY Puzzle AI Aktif")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build()
        }
        startForeground(101, notif)
    }

    private fun showFloatingControls() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val view = LayoutInflater.from(this).inflate(R.layout.overlay_controls, null)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 140
        }

        val btnStepOnce = view.findViewById<Button>(R.id.btnStepOnce)
        val btnAutoToggle = view.findViewById<Button>(R.id.btnAutoToggle)
        val btnClose = view.findViewById<Button>(R.id.btnCloseOverlay)

        btnStepOnce.setOnClickListener {
            serviceScope.launch {
                executeSingleTurn()
            }
        }

        btnAutoToggle.setOnClickListener {
            if (autoJob?.isActive == true) {
                autoJob?.cancel()
                autoJob = null
                btnAutoToggle.text = "Auto: OFF"
            } else {
                btnAutoToggle.text = "Auto: ON"
                autoJob = serviceScope.launch {
                    while (isActive) {
                        val ok = executeSingleTurn()
                        if (!ok) {
                            btnAutoToggle.text = "Auto: OFF"
                            break
                        }
                        delay(1400L)
                    }
                }
            }
        }

        btnClose.setOnClickListener {
            stopSelf()
        }

        overlayView = view
        windowManager.addView(view, params)
    }

    private suspend fun executeSingleTurn(): Boolean {
        val acc = PuzzleAccessibilityService.instance
        if (acc == null) {
            Toast.makeText(this, "Aktifkan Accessibility Service GREY dulu!", Toast.LENGTH_SHORT).show()
            return false
        }
        val apiKey = AppPrefs.getApiKey(this)
        if (apiKey.isBlank()) {
            Toast.makeText(this, "Isi Gemini API Key di aplikasi dulu!", Toast.LENGTH_SHORT).show()
            return false
        }

        return try {
            val png = acc.captureScreenPng()
                ?: throw IllegalStateException("Gagal ambil screenshot (butuh Android 11+).")
            val board = AppPrefs.getBoardConfig(this)
            val plan = GeminiPuzzlePlanner.planNextMove(
                apiKey = apiKey,
                model = AppPrefs.getModel(this),
                pngBytes = png,
                board = board
            )
            acc.dragPieceToCell(plan, board)
        } catch (e: Exception) {
            Toast.makeText(this, "Error AI: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    override fun onDestroy() {
        autoJob?.cancel()
        serviceScope.cancel()
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }
        super.onDestroy()
    }
}
