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
        override fun onPackageAdded(packageName: String, user: UserHandle) = refresh()
        override fun onPackageChanged(packageName: String, user: UserHandle) = refresh()
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = refresh()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = refresh()
    }

    init {
        launcherApps.registerCallback(packageCallback, Handler(Looper.getMainLooper()))
        refresh()
    }

    override fun onCleared() {
        launcherApps.unregisterCallback(packageCallback)
    }

    fun refresh() {
        viewModelScope.launch {
            _apps.value = withContext(Dispatchers.IO) { loadApps() }
        }
    }

    private fun loadApps(): List<AppEntry> {
        val ctx = getApplication<Application>()
        val iconPx = (48 * ctx.resources.displayMetrics.density).toInt()
        // Every profile, so work-profile apps show up too (with a badge).
        return launcherApps.profiles.flatMap { user ->
            launcherApps.getActivityList(null, user).map { info ->
                val label = info.label.toString()
                AppEntry(
                    key = "${info.componentName.flattenToString()}#${user.hashCode()}",
                    label = label,
                    component = info.componentName,
                    user = user,
                    icon = info.getBadgedIcon(0).toBitmap(iconPx, iconPx).asImageBitmap(),
                    letter = letterFor(label),
                )
            }
        }
            .filter { it.component.packageName != ctx.packageName }
            .sortedWith(
                compareBy<AppEntry> { if (it.letter == '#') 0 else 1 }
                    .thenBy { it.letter }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label }
            )
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
        _homeEvents.value++
    }

    fun setQuery(q: String) {
        _query.value = q
    }

    // --- App actions ------------------------------------------------------

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
    }
}
