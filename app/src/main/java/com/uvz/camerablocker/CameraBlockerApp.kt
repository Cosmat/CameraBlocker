package com.uvz.camerablocker

import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class CameraBlockerApp : Application() {

    companion object {
        private const val PREFS_NAME = "camera_blocker_prefs"
        private const val KEY_BLOCKER_ENABLED = "blocker_enabled"
        private const val KEY_OVERLAY_ALPHA = "overlay_alpha" // 0-255
    }

    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        sharedPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
    }

    fun isBlockerEnabled(): Boolean = sharedPrefs.getBoolean(KEY_BLOCKER_ENABLED, true)

    fun setBlockerEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean(KEY_BLOCKER_ENABLED, enabled).apply()
    }

    fun getOverlayAlpha(): Int = sharedPrefs.getInt(KEY_OVERLAY_ALPHA, 255)

    fun setOverlayAlpha(alpha: Int) {
        sharedPrefs.edit().putInt(KEY_OVERLAY_ALPHA, alpha.coerceIn(0, 255)).apply()
    }

    // ---------- Device Admin: системное отключение камеры ----------

    private fun adminComponent(): ComponentName =
        ComponentName(this, CameraDeviceAdminReceiver::class.java)

    private fun dpm(): DevicePolicyManager =
        getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    fun isDeviceAdminActive(): Boolean = dpm().isAdminActive(adminComponent())

    /** true = камера реально отключена на уровне ОС (ни одно приложение не откроет её) */
    fun isCameraSystemDisabled(): Boolean =
        isDeviceAdminActive() && dpm().getCameraDisabled(adminComponent())

    fun setCameraDisabled(disabled: Boolean): Boolean {
        if (!isDeviceAdminActive()) return false
        dpm().setCameraDisabled(adminComponent(), disabled)
        return true
    }
}