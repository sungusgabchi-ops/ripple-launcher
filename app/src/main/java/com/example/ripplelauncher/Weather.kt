package com.example.ripplelauncher

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/** Current conditions, as shown above the clock. */
data class WeatherInfo(
    val temp: Int,
    val high: Int,
    val low: Int,
    val code: Int,      // WMO weather code from Open-Meteo
    val isDay: Boolean,
    val place: String?,
    val fetchedAt: Long,
) {
    val summary: String get() = describe(code)
    val symbol: String get() = symbolFor(code, isDay)

    fun toJson(): String = JSONObject()
        .put("temp", temp).put("high", high).put("low", low).put("code", code)
        .put("isDay", isDay).put("place", place ?: "").put("fetchedAt", fetchedAt)
        .toString()

    companion object {
        fun fromJson(s: String?): WeatherInfo? = try {
            val o = JSONObject(s ?: return null)
            WeatherInfo(
                o.getInt("temp"), o.getInt("high"), o.getInt("low"), o.getInt("code"),
                o.getBoolean("isDay"), o.optString("place").ifBlank { null }, o.getLong("fetchedAt"),
            )
        } catch (_: Exception) {
            null
        }
    }
}

object Weather {

    /** Fahrenheit in the US and a couple of other places, Celsius everywhere else. */
    val useFahrenheit: Boolean
        get() = Locale.getDefault().country in setOf("US", "LR", "MM", "BS", "BZ", "KY", "PW")

    /** Finds the phone's approximate location and fetches the weather there. Null if anything fails. */
    suspend fun fetch(context: Context): WeatherInfo? {
        val location = approximateLocation(context) ?: return null
        return withContext(Dispatchers.IO) {
            try {
                val unit = if (useFahrenheit) "fahrenheit" else "celsius"
                val url = URL(
                    "https://api.open-meteo.com/v1/forecast" +
                        "?latitude=${"%.3f".format(Locale.US, location.latitude)}" +
                        "&longitude=${"%.3f".format(Locale.US, location.longitude)}" +
                        "&current=temperature_2m,weather_code,is_day" +
                        "&daily=temperature_2m_max,temperature_2m_min" +
                        "&temperature_unit=$unit&timezone=auto&forecast_days=1"
                )
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                }
                val body = try {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } finally {
                    conn.disconnect()
                }
                val json = JSONObject(body)
                val current = json.getJSONObject("current")
                val daily = json.getJSONObject("daily")
                WeatherInfo(
                    temp = current.getDouble("temperature_2m").roundToInt(),
                    high = daily.getJSONArray("temperature_2m_max").getDouble(0).roundToInt(),
                    low = daily.getJSONArray("temperature_2m_min").getDouble(0).roundToInt(),
                    code = current.getInt("weather_code"),
                    isDay = current.optInt("is_day", 1) == 1,
                    place = placeName(context, location),
                    fetchedAt = System.currentTimeMillis(),
                )
            } catch (_: Exception) {
                null
            }
        }
    }

    @SuppressLint("MissingPermission") // checked by the caller
    private suspend fun approximateLocation(context: Context): Location? {
        val lm = context.getSystemService(LocationManager::class.java) ?: return null
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER)

        // A recent cached fix is plenty for weather.
        val cached = providers
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
        if (cached != null && System.currentTimeMillis() - cached.time < 3 * 60 * 60 * 1000L) return cached

        // Otherwise ask for a fresh one (Android 11+), waiting at most 10 seconds.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val provider = providers.firstOrNull { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
            if (provider != null) {
                val fresh = withTimeoutOrNull(10_000) {
                    suspendCancellableCoroutine { cont ->
                        val signal = android.os.CancellationSignal()
                        cont.invokeOnCancellation { signal.cancel() }
                        try {
                            lm.getCurrentLocation(provider, signal, context.mainExecutor) { loc -> cont.resume(loc) }
                        } catch (_: Exception) {
                            cont.resume(null)
                        }
                    }
                }
                if (fresh != null) return fresh
            }
        }
        return cached
    }

    private fun placeName(context: Context, location: Location): String? = try {
        if (!Geocoder.isPresent()) null else {
            @Suppress("DEPRECATION")
            val a = Geocoder(context, Locale.getDefault()).getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
            a?.locality ?: a?.subAdminArea ?: a?.adminArea
        }
    } catch (_: Exception) {
        null
    }

    private fun describe(code: Int): String = when (code) {
        0 -> "Clear"
        1 -> "Mostly clear"
        2 -> "Partly cloudy"
        3 -> "Cloudy"
        45, 48 -> "Fog"
        51, 53, 55, 56, 57 -> "Drizzle"
        61, 63, 66 -> "Rain"
        65, 67 -> "Heavy rain"
        71, 73, 77 -> "Snow"
        75 -> "Heavy snow"
        80, 81 -> "Showers"
        82 -> "Heavy showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm, hail"
        else -> "—"
    }

    private fun symbolFor(code: Int, isDay: Boolean): String = when (code) {
        0, 1 -> if (isDay) "☀️" else "🌙"
        2 -> if (isDay) "⛅" else "☁️"
        3 -> "☁️"
        45, 48 -> "🌫️"
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> "🌧️"
        71, 73, 75, 77, 85, 86 -> "❄️"
        95, 96, 99 -> "⛈️"
        else -> "🌡️"
    }
}
