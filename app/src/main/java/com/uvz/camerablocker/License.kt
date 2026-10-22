package com.uvz.camerablocker

import android.content.Context
import android.os.Build
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Лицензирование (заложено заранее, сейчас у всех бесплатный период).
 *
 * Модель: приложение работает до вшитой даты FREE_UNTIL. После — просит код продления.
 * Код выдаёт владелец (Алексей): код для даты D = первые 8 hex-символов
 * HMAC-SHA256(SECRET, "extend:yyyy-MM-dd"). Ввод кода продлевает срок до D.
 * Всё офлайн, без сервера.
 */
object License {

    // Бесплатный период до 2027-07-01 00:00 UTC (~9 месяцев, потом ключ продления)
    private const val FREE_UNTIL = 1814400000000L

    // Соль для генерации кодов. Генератор на стороне владельца:
    // python: hmac.new(SECRET, b"extend:YYYY-MM-DD", sha256).hex()[:8]
    private const val SECRET = "uvz-cb-2026-slushar-biz"

    private const val PREFS = "license"
    private const val KEY_EXPIRY = "expiry"

    fun expiryAt(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_EXPIRY, FREE_UNTIL)
    }

    fun isValid(context: Context): Boolean =
        System.currentTimeMillis() < expiryAt(context)

    /** Применить код продления. true = срок продлён до даты, закодированной в коде. */
    fun applyCode(context: Context, code: String): Boolean {
        val clean = code.trim().uppercase(Locale.US).replace(" ", "")
        if (clean.length < 8) return false

        val utc = TimeZone.getTimeZone("UTC")
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = utc }
        val cal = Calendar.getInstance(utc).apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }

        repeat(365 * 3) { // ищем дату до 3 лет вперёд
            val day = cal.timeInMillis
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(SECRET.toByteArray(), "HmacSHA256"))
            val hex = mac.doFinal("extend:${fmt.format(Date(day))}".toByteArray())
                .joinToString("") { "%02x".format(it) }
            if (hex.startsWith(clean)) {
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putLong(KEY_EXPIRY, day + 86_400_000L).apply()
                return true
            }
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return false
    }

    /** Дата окончания строкой для UI */
    fun expiryText(context: Context): String {
        val utc = TimeZone.getTimeZone("UTC")
        val fmt = SimpleDateFormat("dd.MM.yyyy", Locale.US).apply { timeZone = utc }
        return fmt.format(Date(expiryAt(context)))
    }
}
