package com.uvz.camerablocker

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var app: CameraBlockerApp
    private lateinit var switchEnable: Switch
    private lateinit var seekAlpha: SeekBar
    private lateinit var txtAlpha: TextView
    private lateinit var txtStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_main)

        app = application as CameraBlockerApp

        switchEnable = findViewById(R.id.switchEnable)
        seekAlpha = findViewById(R.id.seekAlpha)
        txtAlpha = findViewById(R.id.txtAlpha)
        txtStatus = findViewById(R.id.txtStatus)

        updateUI()

        switchEnable.setOnCheckedChangeListener { _, isChecked ->
            app.setBlockerEnabled(isChecked)
            updateUI()
            if (isChecked) requestOverlayPermission()
        }

        seekAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                app.setOverlayAlpha(progress)
                txtAlpha.text = "Прозрачность оверлея: $progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        findViewById<android.widget.Button>(R.id.btnAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        findViewById<android.widget.Button>(R.id.btnOverlay).setOnClickListener {
            requestOverlayPermission()
        }

        findViewById<android.widget.Button>(R.id.btnTest).setOnClickListener {
            testOverlay()
        }

        // Автозапуск сервиса при запуске из BootReceiver
        if (intent.getBooleanExtra("auto_start", false)) {
            if (app.isBlockerEnabled()) requestOverlayPermission()
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val enabled = app.isBlockerEnabled()
        switchEnable.isChecked = enabled
        seekAlpha.progress = app.getOverlayAlpha()
        txtAlpha.text = "Прозрачность оверлея: ${app.getOverlayAlpha()}%"
        txtStatus.text = if (enabled) "Статус: АКТИВЕН (чёрный экран при камере)" else "Статус: ОТКЛЮЧЕН"
        txtStatus.setTextColor(ContextCompat.getColor(this, if (enabled) R.color.active else R.color.inactive))
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivityForResult(intent, 1001)
            }
        }
    }

    private fun testOverlay() {
        val intent = Intent(this, OverlayService::class.java)
        intent.action = OverlayService.ACTION_SHOW
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        // Скроем через 3 секунды для теста
        findViewById<android.view.View>(android.R.id.content).postDelayed({
            val hideIntent = Intent(this, OverlayService::class.java)
            hideIntent.action = OverlayService.ACTION_HIDE
            startService(hideIntent)
        }, 3000)
    }
}