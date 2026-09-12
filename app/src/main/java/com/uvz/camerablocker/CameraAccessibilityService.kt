package com.uvz.camerablocker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo

class CameraAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "CameraBlockerService"
        // Пакеты камерных приложений — расширяй под конкретные телефоны на заводе
        private val CAMERA_PACKAGES = setOf(
            "com.android.camera",
            "com.google.android.GoogleCamera",
            "com.sec.android.app.camera",
            "com.mi.android.camera",
            "com.oneplus.camera",
            "com.coloros.camera",
            "com.oppo.camera",
            "com.huawei.camera",
            "com.motorola.camera",
            "com.sonyericsson.android.camera",
            "com.asus.camera",
            "com.nokia.camera",
            "com.htc.camera"
        )
    }

    private var isOverlayActive = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Accessibility service created")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val app = application as CameraBlockerApp
        if (!app.isBlockerEnabled()) {
            hideOverlay()
            return
        }

        event?.let {
            when (it.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                    val packageName = it.packageName?.toString() ?: ""
                    checkAndToggleOverlay(packageName)
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
        hideOverlay()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Service connected")
        // На Android 12+ нужно запросить разрешение на оверлей вручную
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestOverlayPermission()
        }
    }

    private fun checkAndToggleOverlay(packageName: String) {
        val shouldShow = CAMERA_PACKAGES.contains(packageName) ||
                isCameraIntentPackage(packageName)

        if (shouldShow && !isOverlayActive) {
            showOverlay()
        } else if (!shouldShow && isOverlayActive) {
            hideOverlay()
        }
    }

    private fun isCameraIntentPackage(packageName: String): Boolean {
        // Дополнительная проверка: если приложение объявило ACTION_IMAGE_CAPTURE
        val pm = packageManager
        try {
            val info = pm.getPackageInfo(packageName, 0)
            val activities = info.activities ?: return false
            for (activity in activities) {
                val filters = activity.intentFilters
                if (filters != null) {
                    for (filter in filters) {
                        if (filter.hasAction("android.media.action.IMAGE_CAPTURE") ||
                            filter.hasAction("android.media.action.VIDEO_CAPTURE")) {
                            return true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return false
    }

    private fun showOverlay() {
        Log.d(TAG, "SHOWING BLACK OVERLAY")
        isOverlayActive = true
        val intent = Intent(this, OverlayService::class.java)
        intent.action = OverlayService.ACTION_SHOW
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun hideOverlay() {
        Log.d(TAG, "HIDING BLACK OVERLAY")
        isOverlayActive = false
        val intent = Intent(this, OverlayService::class.java)
        intent.action = OverlayService.ACTION_HIDE
        startService(intent)
    }

    private fun requestOverlayPermission() {
        // На Android 12+ SYSTEM_ALERT_WINDOW требует специального запроса
        if (!android.provider.Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Overlay permission not granted")
        }
    }
}