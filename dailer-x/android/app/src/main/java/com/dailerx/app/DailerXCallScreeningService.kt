package com.dailerx.app

import android.os.Build
import android.telecom.Call
import android.telephony.PhoneNumberUtils
import android.telecom.CallScreeningService
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * User-selected Android call-screening integration.
 * Explicit local block rules are applied first; allow rules always win.
 * Server-based blocking is opt-in and requires a threshold of independent reports.
 * Any network/parsing failure fails open. This service never records audio.
 */
class DailerXCallScreeningService : CallScreeningService() {
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(800, TimeUnit.MILLISECONDS)
            .readTimeout(900, TimeUnit.MILLISECONDS)
            .callTimeout(1500, TimeUnit.MILLISECONDS)
            .build()
    }

    override fun onScreenCall(callDetails: Call.Details) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && callDetails.callDirection != Call.Details.DIRECTION_INCOMING) return
        val handle = callDetails.handle
        val number = handle?.takeIf { it.scheme == "tel" }?.schemeSpecificPart
        if (number.isNullOrBlank()) {
            respondToCall(callDetails, response(allow = true))
            return
        }

        val prefs = getSharedPreferences("dailer_x_local", MODE_PRIVATE)
        val region = (prefs.getString("phone_region", Locale.getDefault().country.ifBlank { "US" }) ?: "US").uppercase(Locale.ROOT)
        val lookupNumber = PhoneNumberUtils.formatNumberToE164(number, region) ?: number
        val digits = canonicalDigits(lookupNumber)
        val allowlist = parseNumberRules(prefs.getString("allowed_numbers", ""), region)
        val blocklist = parseNumberRules(prefs.getString("blocked_numbers", ""), region)
        if (digits in allowlist) {
            respondToCall(callDetails, response(allow = true))
            return
        }
        if (digits in blocklist) {
            DailerXNotifications.postCallerAlert(this, number, "Blocked number", 0, true)
            respondToCall(callDetails, response(allow = false))
            return
        }

        val autoBlock = prefs.getBoolean("auto_block_spam", false)
        if (!autoBlock) {
            // Even with server-based auto-blocking off, caller-ID lookup is still useful.
            val info = lookup(lookupNumber, prefs.getString("backend", "http://10.0.2.2:8000") ?: "http://10.0.2.2:8000")
            if (!info.name.isNullOrBlank() || info.reports > 0) {
                DailerXNotifications.postCallerAlert(this, number, info.name, info.reports, false)
            }
            respondToCall(callDetails, response(allow = true))
            return
        }

        val base = (prefs.getString("backend", "http://10.0.2.2:8000") ?: "http://10.0.2.2:8000").trimEnd('/')
        val info = lookup(lookupNumber, base)
        val shouldBlock = info.reports >= SPAM_BLOCK_THRESHOLD
        if (shouldBlock || !info.name.isNullOrBlank() || info.reports > 0) {
            DailerXNotifications.postCallerAlert(this, number, info.name, info.reports, shouldBlock)
        }
        respondToCall(callDetails, response(allow = !shouldBlock))
    }

    private fun lookup(number: String, backend: String): CallerInfo {
        return runCatching {
            val encoded = URLEncoder.encode(number, Charsets.UTF_8.name())
            val request = Request.Builder().url("${backend.trimEnd('/')}/v1/lookup?number=$encoded").get().build()
            client.newCall(request).execute().use { http ->
                if (!http.isSuccessful) return CallerInfo()
                val payload = JSONObject(http.body?.string() ?: "{}")
                val spamReports = payload.optJSONObject("spam")?.optInt("reports", 0) ?: 0
                val matches = payload.optJSONArray("matches")
                var name: String? = null
                if (matches != null && matches.length() > 0) {
                    val candidate = matches.getJSONObject(0)
                    val candidateName = candidate.optString("name").takeIf { it.isNotBlank() }
                    name = if (candidate.optBoolean("verified", false)) candidateName else candidateName?.let { "Possible: $it" }
                }
                CallerInfo(name, spamReports)
            }
        }.onFailure { Log.w("DailerXScreening", "Caller lookup failed; allowing call", it) }
            .getOrDefault(CallerInfo())
    }

    private fun response(allow: Boolean): CallScreeningService.CallResponse =
        CallScreeningService.CallResponse.Builder()
            .setDisallowCall(!allow)
            .setRejectCall(!allow)
            .setSilenceCall(false)
            .build()

    private fun parseNumberRules(raw: String?, region: String): Set<String> = raw.orEmpty()
        .split(',', ';', '\n', '\r')
        .map { rule -> PhoneNumberUtils.formatNumberToE164(rule.trim(), region) ?: rule }
        .map { canonicalDigits(it) }
        .filter { it.length >= 3 }
        .toSet()

    private fun canonicalDigits(value: String): String = value.filter(Char::isDigit).takeLast(15)

    private data class CallerInfo(val name: String? = null, val reports: Int = 0)

    companion object {
        private const val SPAM_BLOCK_THRESHOLD = 5
    }
}
