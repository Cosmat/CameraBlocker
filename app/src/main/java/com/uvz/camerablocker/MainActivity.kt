package com.uvz.camerablocker

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
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
            if (isChecked) {
                if (app.isDeviceAdminActive()) {
                    app.setCameraDisabled(true)
                } else {
                    // Разовое исключение: системный экран активации админа
                    requestAdminActivation()
                }
            } else {
                app.setCameraDisabled(false)
            }
            updateUI()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1002) {
            if (resultCode == RESULT_OK) {
                // Админ активирован — сразу отключаем камеру
                app.setCameraDisabled(true)
            } else {
                // Отказ — возвращаем переключатель
                app.setBlockerEnabled(false)
            }
            updateUI()
        }
    }

    private fun requestAdminActivation() {
        val comp = ComponentName(this, CameraDeviceAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, comp)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Активация отключает камеру на уровне системы (проходная УВЗ). Выключить обратно можно тем же переключателем."
            )
        }
        startActivityForResult(intent, 1002)
    }

    private fun updateUI() {
        val enabled = app.isBlockerEnabled()
        switchEnable.isChecked = enabled
        seekAlpha.progress = app.getOverlayAlpha()
        txtAlpha.text = "Прозрачность оверлея: ${app.getOverlayAlpha()}%"
        val camDisabled = app.isCameraSystemDisabled()
        txtStatus.text = when {
            camDisabled -> "Камера ОТКЛЮЧЕНА системой (для проходной)"
            app.isDeviceAdminActive() -> "Админ активен, камера разрешена"
            enabled -> "Включи переключатель и подтверди администратора"
            else -> "Статус: ОТКЛЮЧЕН (камера работает как обычно)"
        }
        txtStatus.setTextColor(
            ContextCompat.getColor(
                this,
                if (camDisabled) R.color.active else R.color.inactive
            )
        )
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