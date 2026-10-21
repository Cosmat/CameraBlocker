package com.uvz.camerablocker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("CameraBlocker", "Boot received: $action")
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            // Device Admin политика переживает ребут сама (её держит система),
            // но переподтверждаем на случай сбоя
            val app = context.applicationContext as CameraBlockerApp
            if (app.isBlockerEnabled() && app.isDeviceAdminActive()) {
                val ok = app.setCameraDisabled(true)
                Log.d("CameraBlocker", "Re-assert camera disabled: $ok")
            }
        }
    }
}