package com.example.ripplelauncher

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer

/** Launcher background over the wallpaper. */
enum class Backdrop(val label: String) {
    Wallpaper("Wallpaper"),
    Dim("Dimmed wallpaper"),
    Dark("Dark"),
    Black("Black"),
}

data class AppEntry(
    val key: String,
    val label: String,
    val component: ComponentName,
    val user: UserHandle,
    val icon: ImageBitmap,
    val letter: Char,
)

class LauncherViewModel(app: Application) : AndroidViewModel(app) {

    private val launcherApps = app.getSystemService(LauncherApps::class.java)
    private val prefs = app.getSharedPreferences("launcher", Context.MODE_PRIVATE)
    private val iconPx = (48 * app.resources.displayMetrics.density).toInt()
    private val iconCache = IconCache(app, iconPx)
    private val loadLock = Mutex()

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    private val _favoriteKeys = MutableStateFlow(loadFavorites())
    val favoriteKeys: StateFlow<List<String>> = _favoriteKeys.asStateFlow()

    val favorites: StateFlow<List<AppEntry>> =
        combine(_apps, _favoriteKeys) { apps, keys ->
            val byKey = apps.associateBy { it.key }
            keys.mapNotNull { byKey[it] }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Bumped whenever the list should jump back to the top (Home pressed, app launched).
    private val _homeEvents = MutableStateFlow(0)
    val homeEvents: StateFlow<Int> = _homeEvents.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    // Keeps the app list current when apps are installed, removed or updated.
    private val packageCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = refresh()
        override fun onPackageAdded(packageName: String, user: UserHandle) = refresh(packageName)
        override fun onPackageChanged(packageName: String, user: UserHandle) = refresh(packageName)
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = refresh()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = refresh()
    }

    init {
        launcherApps.registerCallback(packageCallback, Handler(Looper.getMainLooper()))
        // Show the saved list right away, then check for changes in the background.
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { loadSaved() }
            if (_apps.value.isEmpty() && saved.isNotEmpty()) _apps.value = saved
            refresh()
        }
    }

    override fun onCleared() {
        launcherApps.unregisterCallback(packageCallback)
    }

    /** Reloads the app list. [changedPackage] gets its icon redrawn even if its version didn't change. */
    fun refresh(changedPackage: String? = null) {
        viewModelScope.launch {
            if (changedPackage != null) iconCache.evictPackage(changedPackage)
            val list = loadLock.withLock { withContext(Dispatchers.IO) { loadApps() } }
            _apps.value = list
        }
    }

    private fun loadSaved(): List<AppEntry> =
        iconCache.loadFromDisk().mapNotNull { c ->
            val user = iconCache.userFor(c.userSerial) ?: return@mapNotNull null
            AppEntry(c.key, c.label, c.component, user, c.bitmap.asImageBitmap(), letterFor(c.label))
        }.sortedWith(appOrder)

    private suspend fun loadApps(): List<AppEntry> = coroutineScope {
        val ctx = getApplication<Application>()
        // Every profile, so work-profile apps show up too (with a badge).
        val activities = launcherApps.profiles.flatMap { user ->
            launcherApps.getActivityList(null, user).map { user to it }
        }.filter { it.second.componentName.packageName != ctx.packageName }

        // Reuse cached icons; draw only new or updated apps, several at a time.
        val cached = activities.map { (user, info) ->
            async(Dispatchers.Default) {
                val key = "${info.componentName.flattenToString()}#${user.hashCode()}"
                val version = versionOf(info.componentName.packageName, user, info.firstInstallTime)
                val bitmap = iconCache.get(key, version)
                    ?: info.getBadgedIcon(0).toBitmap(iconPx, iconPx)
                IconCache.Cached(
                    key, info.label.toString(), info.componentName,
                    iconCache.serialFor(user), version, bitmap,
                ) to user
            }
        }.awaitAll()

        iconCache.save(cached.map { it.first })

        cached.map { (c, user) ->
            AppEntry(c.key, c.label, c.component, user, c.bitmap.asImageBitmap(), letterFor(c.label))
        }.sortedWith(appOrder)
    }

    /** Changes whenever the app is updated, so its icon gets redrawn. */
    private fun versionOf(packageName: String, user: UserHandle, firstInstall: Long): Long {
        val updated = if (user == android.os.Process.myUserHandle()) {
            runCatching {
                getApplication<Application>().packageManager.getPackageInfo(packageName, 0).lastUpdateTime
            }.getOrDefault(0L)
        } else 0L
        return if (updated != 0L) updated else firstInstall
    }


    private fun letterFor(label: String): Char {
        // Strip accents so "Éclair" files under E.
        val first = Normalizer.normalize(label.trim().take(1), Normalizer.Form.NFD)
            .firstOrNull()?.uppercaseChar() ?: return '#'
        return if (first in 'A'..'Z') first else '#'
    }

    // --- Navigation -------------------------------------------------------

    fun goHome() {
        _query.value = ""
        _searchOpen.value = false
        _homeEvents.value++
    }

    // The search box is hidden behind a search icon until tapped.
    private val _searchOpen = MutableStateFlow(false)
    val searchOpen: StateFlow<Boolean> = _searchOpen.asStateFlow()

    fun openSearch() {
        _searchOpen.value = true
    }

    fun closeSearch() {
        _query.value = ""
        _searchOpen.value = false
    }

    fun setQuery(q: String) {
        _query.value = q
    }

    // --- Appearance -------------------------------------------------------

    private val _backdrop = MutableStateFlow(
        runCatching { Backdrop.valueOf(prefs.getString(KEY_BACKDROP, null) ?: "") }.getOrDefault(Backdrop.Dim)
    )
    val backdrop: StateFlow<Backdrop> = _backdrop.asStateFlow()

    // --- Weather ----------------------------------------------------------

    private val _weather = MutableStateFlow(WeatherInfo.fromJson(prefs.getString(KEY_WEATHER, null)))
    val weather: StateFlow<WeatherInfo?> = _weather.asStateFlow()
    private var weatherLoading = false

    fun hasLocationPermission(): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(
            getApplication(), android.Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    /** Refreshes when the cached weather is over 30 minutes old (or always, if forced). */
    fun refreshWeather(force: Boolean = false) {
        if (weatherLoading || !hasLocationPermission()) return
        val age = System.currentTimeMillis() - (_weather.value?.fetchedAt ?: 0L)
        if (!force && age < 30 * 60 * 1000L) return
        weatherLoading = true
        viewModelScope.launch {
            try {
                Weather.fetch(getApplication())?.let {
                    _weather.value = it
                    prefs.edit().putString(KEY_WEATHER, it.toJson()).apply()
                } ?: run { if (force) toast("Couldn't update the weather") }
            } finally {
                weatherLoading = false
            }
        }
    }

    fun setBackdrop(b: Backdrop) {
        _backdrop.value = b
        prefs.edit().putString(KEY_BACKDROP, b.name).apply()
    }

    // --- Brightness -------------------------------------------------------

    /** Screen brightness as 0..1 on a perceptual scale (feels even along the slider). */
    fun readBrightness(): Float {
        val raw = try {
            android.provider.Settings.System.getInt(
                getApplication<Application>().contentResolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS,
            )
        } catch (_: Exception) {
            128
        }
        return kotlin.math.sqrt(raw.coerceIn(0, 255) / 255f)
    }

    fun canChangeBrightness(): Boolean =
        android.provider.Settings.System.canWrite(getApplication())

    /** Opens the "Modify system settings" screen for this launcher. */
    fun requestBrightnessPermission() {
        val ctx = getApplication<Application>()
        try {
            ctx.startActivity(
                Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            toast("Turn on \"Allow modifying system settings\" for Ripple")
        } catch (_: Exception) {
            toast("Couldn't open the permission screen")
        }
    }

    fun setBrightness(level: Float) {
        val resolver = getApplication<Application>().contentResolver
        val raw = (level.coerceIn(0f, 1f).let { it * it } * 255f).toInt().coerceIn(1, 255)
        try {
            // Dragging the slider means "manual": switch off adaptive brightness first.
            android.provider.Settings.System.putInt(
                resolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE,
                android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
            )
            android.provider.Settings.System.putInt(resolver, android.provider.Settings.System.SCREEN_BRIGHTNESS, raw)
        } catch (_: Exception) {
        }
    }

    fun openCamera() {
        val ctx = getApplication<Application>()
        val tries = listOf(
            Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
            Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE),
        )
        for (intent in tries) {
            try {
                ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (_: Exception) {
            }
        }
        toast("Couldn't open the camera")
    }

    fun openSystemSettings() {
        try {
            getApplication<Application>().startActivity(
                Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            toast("Couldn't open Settings")
        }
    }

    // --- App actions ------------------------------------------------------

    // --- Weather and clock shortcuts --------------------------------------

    private fun launchPackage(pkg: String): Boolean {
        val app = _apps.value.firstOrNull { it.component.packageName == pkg } ?: return false
        launch(app)
        return true
    }

    private fun startQuietly(intent: Intent): Boolean = try {
        getApplication<Application>().startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: Exception) {
        false
    }

    /** Opens the phone's clock app on its main screen. */
    fun openClockApp() {
        val pm = getApplication<Application>().packageManager
        // Whichever app handles alarms is the clock app.
        val handler = runCatching {
            pm.resolveActivity(Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS), 0)?.activityInfo?.packageName
        }.getOrNull()
        val known = listOf(
            "com.google.android.deskclock", "com.sec.android.app.clockpackage", "com.android.deskclock",
            "com.oneplus.deskclock", "com.coloros.alarmclock", "com.oplus.alarmclock",
            "com.motorola.timeweatherwidget", "com.huawei.deskclock", "com.miui.clock", "com.lge.clock",
        )
        for (pkg in listOfNotNull(handler) + known) if (launchPackage(pkg)) return
        _apps.value.firstOrNull { it.label.equals("Clock", ignoreCase = true) }?.let { launch(it); return }
        if (!startQuietly(Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS))) toast("No clock app found")
    }

    /** Opens a weather app: an installed one if possible, otherwise Google's weather page. */
    fun openWeatherApp() {
        val known = listOf(
            "com.sec.android.daemonapp",              // Samsung Weather
            "com.google.android.apps.weather",        // Google Weather
            "com.motorola.timeweatherwidget",
            "com.oneplus.weather", "com.coloros.weather2", "com.oplus.weather", "com.miui.weather2",
            "com.huawei.android.totemweather",
            "com.weather.Weather",                    // The Weather Channel
            "com.accuweather.android",
            "com.yahoo.mobile.client.android.weather",
            "com.wunderground.android.weather",
            "com.handmark.expressweather",
            "com.windyty.android",
        )
        for (pkg in known) if (launchPackage(pkg)) return
        _apps.value.firstOrNull { it.label.contains("weather", ignoreCase = true) }?.let { launch(it); return }

        // Pixel phones: Google's weather screen lives inside the Google app.
        val pixelWeather = Intent().setClassName(
            "com.google.android.googlequicksearchbox",
            "com.google.android.apps.search.weather.WeatherExportedActivity",
        )
        if (startQuietly(pixelWeather)) return
        if (!startQuietly(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=weather")))) {
            toast("No weather app found")
        }
    }

    fun launch(app: AppEntry) {
        try {
            launcherApps.startMainActivity(app.component, app.user, null, null)
        } catch (e: Exception) {
            toast("Couldn't open ${app.label}")
        }
        // Return to home behind the app, after the open animation has started.
        viewModelScope.launch {
            delay(350)
            goHome()
        }
    }

    fun openAppInfo(app: AppEntry) {
        try {
            launcherApps.startAppDetailsActivity(app.component, app.user, null, null)
        } catch (e: Exception) {
            toast("Couldn't open app info")
        }
    }

    fun uninstall(app: AppEntry) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.component.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            toast("Couldn't uninstall ${app.label}")
        }
    }

    @SuppressLint("WrongConstant")
    fun expandNotifications() {
        // Not a public API, but every major launcher uses it. Fails silently if blocked.
        try {
            val service = getApplication<Application>().getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager")
                .getMethod("expandNotificationsPanel")
                .invoke(service)
        } catch (_: Exception) {
        }
    }

    // --- Favorites --------------------------------------------------------

    // --- Less used ----------------------------------------------------------
    // Apps moved out of the A–Z list into a collapsible section at the end.

    private val _lessUsedKeys = MutableStateFlow(prefs.getStringSet(KEY_LESS_USED, emptySet())!!.toSet())
    val lessUsedKeys: StateFlow<Set<String>> = _lessUsedKeys.asStateFlow()

    fun toggleLessUsed(app: AppEntry) {
        val current = _lessUsedKeys.value
        val next = if (app.key in current) current - app.key else current + app.key
        _lessUsedKeys.value = next
        prefs.edit().putStringSet(KEY_LESS_USED, next).apply()
    }

    fun toggleFavorite(app: AppEntry) {
        val current = _favoriteKeys.value
        saveFavorites(if (app.key in current) current - app.key else current + app.key)
    }

    fun moveFavorite(app: AppEntry, direction: Int) {
        val list = _favoriteKeys.value.toMutableList()
        val from = list.indexOf(app.key)
        val to = from + direction
        if (from == -1 || to !in list.indices) return
        list.add(to, list.removeAt(from))
        saveFavorites(list)
    }

    /** Saves a new favorites order from dragging. Keeps any favorites not currently shown. */
    fun setFavoriteOrder(keys: List<String>) {
        val hidden = _favoriteKeys.value.filter { it !in keys }
        saveFavorites(keys + hidden)
    }

    private fun loadFavorites(): List<String> =
        prefs.getString(KEY_FAVORITES, null)
            ?.split('\n')
            ?.filter { it.isNotBlank() }
            ?: emptyList()

    private fun saveFavorites(list: List<String>) {
        _favoriteKeys.value = list
        prefs.edit().putString(KEY_FAVORITES, list.joinToString("\n")).apply()
    }

    private fun toast(msg: String) =
        Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()

    private companion object {
        const val KEY_FAVORITES = "favorites"
        const val KEY_BACKDROP = "backdrop"
        const val KEY_WEATHER = "weather"
        const val KEY_LESS_USED = "less_used"

        // In the companion so it's ready before init{} reads the saved list.
        val appOrder = compareBy<AppEntry> { if (it.letter == '#') 0 else 1 }
            .thenBy { it.letter }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label }
    }
}
