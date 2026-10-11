package com.dailerx.app

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telephony.PhoneNumberUtils
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * User-selected Android call-screening integration.
 *
 * Local allow/block rules are evaluated immediately, with allow rules taking priority.
 * Backend lookups run on a bounded background executor; the Android callback thread never
 * performs network I/O. Server-based blocking is opt-in and is based on unverified reports.
 * Overloaded workers, timeouts, malformed data and backend failures all fail open.
 * This service does not record call audio.
 */
class DailerXCallScreeningService : CallScreeningService() {
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(800, TimeUnit.MILLISECONDS)
            .readTimeout(900, TimeUnit.MILLISECONDS)
            .callTimeout(1500, TimeUnit.MILLISECONDS)
            .build()
    }

    /**
     * A SynchronousQueue prevents incoming calls from waiting behind older lookups.
     * If both workers are occupied, auto-block fails open instead of delaying the Telecom response.
     */
    private val lookupExecutor by lazy {
        ThreadPoolExecutor(
            0,
            2,
            30L,
            TimeUnit.SECONDS,
            SynchronousQueue<Runnable>(),
            ThreadFactory { task -> Thread(task, "DailerX-CallerLookup").apply { isDaemon = true } },
            ThreadPoolExecutor.AbortPolicy()
        )
    }

    override fun onScreenCall(callDetails: Call.Details) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            callDetails.callDirection != Call.Details.DIRECTION_INCOMING
        ) {
            // Always answer the screening callback. Dailer X only applies caller-ID rules to incoming calls.
            deliverResponse(callDetails, allow = true)
            return
        }

        val number = callDetails.handle
            ?.takeIf { it.scheme == "tel" }
            ?.schemeSpecificPart

        if (number.isNullOrBlank()) {
            deliverResponse(callDetails, allow = true)
            return
        }

        val prefs = getSharedPreferences("dailer_x_local", MODE_PRIVATE)
        val region = (
            prefs.getString("phone_region", Locale.getDefault().country.ifBlank { "US" }) ?: "US"
        ).uppercase(Locale.ROOT)
        val lookupNumber = PhoneNumberUtils.formatNumberToE164(number, region) ?: number
        val digits = canonicalDigits(lookupNumber)

        val allowlist = parseNumberRules(prefs.getString("allowed_numbers", ""), region)
        val blocklist = parseNumberRules(prefs.getString("blocked_numbers", ""), region)

        // Local allow rules always win, including when the server labels a number as spam.
        if (digits in allowlist) {
            deliverResponse(callDetails, allow = true)
            return
        }

        if (digits in blocklist) {
            runCatching {
                DailerXNotifications.postCallerAlert(this, number, "Blocked number", 0, true)
            }
            deliverResponse(callDetails, allow = false)
            return
        }

        val backend = (
            prefs.getString("backend", "http://10.0.2.2:8000") ?: "http://10.0.2.2:8000"
        ).trimEnd('/')
        val autoBlock = prefs.getBoolean("auto_block_spam", false)

        if (!autoBlock) {
            // Never delay a call merely to display caller-ID information.
            deliverResponse(callDetails, allow = true)
            enqueueLookup(lookupNumber, backend) { info ->
                if (!info.name.isNullOrBlank() || info.reports > 0) {
                    runCatching {
                        DailerXNotifications.postCallerAlert(this, number, info.name, info.reports, false)
                    }.onFailure { Log.w(TAG, "Could not post caller alert", it) }
                }
            }
            return
        }

        // When the user explicitly enables automatic blocking, the backend response is needed
        // before deciding. The worker is bounded to 1.5 s and a saturated executor fails open.
        try {
            lookupExecutor.execute {
                val info = lookup(lookupNumber, backend)
                val shouldBlock = info.reports >= SPAM_BLOCK_THRESHOLD

                if (shouldBlock || !info.name.isNullOrBlank() || info.reports > 0) {
                    runCatching {
                        DailerXNotifications.postCallerAlert(this, number, info.name, info.reports, shouldBlock)
                    }.onFailure { Log.w(TAG, "Could not post caller alert", it) }
                }

                deliverResponse(callDetails, allow = !shouldBlock)
            }
        } catch (rejected: RejectedExecutionException) {
            Log.w(TAG, "Lookup capacity reached; allowing call rather than delaying Telecom", rejected)
            deliverResponse(callDetails, allow = true)
        }
    }

    private fun enqueueLookup(number: String, backend: String, onResult: (CallerInfo) -> Unit) {
        try {
            lookupExecutor.execute {
                val result = lookup(number, backend)
                runCatching { onResult(result) }
                    .onFailure { Log.w(TAG, "Caller lookup result handler failed", it) }
            }
        } catch (rejected: RejectedExecutionException) {
            // Caller ID is best-effort while auto-block is off; omit the notification under load.
            Log.w(TAG, "Lookup capacity reached; skipping caller-ID notification", rejected)
        }
    }

    private fun lookup(number: String, backend: String): CallerInfo {
        return runCatching {
            val encoded = URLEncoder.encode(number, Charsets.UTF_8.name())
            val request = Request.Builder()
                .url("${backend.trimEnd('/')}/v1/lookup?number=$encoded")
                .get()
                .build()

            client.newCall(request).execute().use { http ->
                if (!http.isSuccessful) return CallerInfo()
                val payload = JSONObject(http.body?.string() ?: "{}")
                val spamReports = payload.optJSONObject("spam")?.optInt("reports", 0) ?: 0
                val matches = payload.optJSONArray("matches")
                var name: String? = null

                if (matches != null && matches.length() > 0) {
                    val candidate = matches.getJSONObject(0)
                    val candidateName = candidate.optString("name").takeIf { it.isNotBlank() }
                    name = if (candidate.optBoolean("verified", false)) {
                        candidateName
                    } else {
                        candidateName?.let { "Possible: $it" }
                    }
                }

                CallerInfo(name, spamReports.coerceAtLeast(0))
            }
        }.onFailure {
            Log.w(TAG, "Caller lookup failed; allowing call", it)
        }.getOrDefault(CallerInfo())
    }

    private fun deliverResponse(callDetails: Call.Details, allow: Boolean) {
        val action = Runnable {
            runCatching { respondToCall(callDetails, response(allow)) }
                .onFailure { Log.w(TAG, "Could not respond to call screening request", it) }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run()
        } else {
            mainHandler.post(action)
        }
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
        private const val TAG = "DailerXScreening"
        private const val SPAM_BLOCK_THRESHOLD = 5
    }
}
