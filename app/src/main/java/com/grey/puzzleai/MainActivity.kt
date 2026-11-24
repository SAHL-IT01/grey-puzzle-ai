package com.grey.puzzleai

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etApiKey = findViewById<EditText>(R.id.etApiKey)
        val etModel = findViewById<EditText>(R.id.etModel)
        val etBoardBbox = findViewById<EditText>(R.id.etBoardBbox)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)

        etApiKey.setText(AppPrefs.getApiKey(this))
        etModel.setText(AppPrefs.getModel(this))
        etBoardBbox.setText(AppPrefs.getBoardRaw(this))

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            AppPrefs.save(
                this,
                etApiKey.text.toString(),
                etModel.text.toString(),
                etBoardBbox.text.toString()
            )
            tvStatus.text = "Status: Pengaturan tersimpan di HP."
        }

        findViewById<Button>(R.id.btnAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            tvStatus.text = "Status: Cari & aktifkan 'GREY Puzzle AI' di menu Aksesibilitas."
        }

        findViewById<Button>(R.id.btnOverlayPermission).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
                tvStatus.text = "Status: Izinkan tampil di atas aplikasi lain."
            } else {
                tvStatus.text = "Status: Izin Overlay sudah aktif."
            }
        }

        findViewById<Button>(R.id.btnLaunchOverlay).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                tvStatus.text = "Status: Izinkan Overlay dulu pada langkah 3."
                return@setOnClickListener
            }
            val intent = Intent(this, OverlayControlService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            tvStatus.text = "Status: Kontrol melayang aktif. Silakan buka game block puzzle Anda!"
        }
    }
}
