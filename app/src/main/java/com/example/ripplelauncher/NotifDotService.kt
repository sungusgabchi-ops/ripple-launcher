package com.example.ripplelauncher

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Watches notifications so the launcher can show a dot on apps that have unread ones.
 * Only package names are kept; notification content is never read or stored.
 * Runs only after the user allows notification access in Android settings.
 */
class NotifDotService : NotificationListenerService() {

    companion object {
        private val _active = MutableStateFlow<Set<String>>(emptySet())
        val active: StateFlow<Set<String>> = _active.asStateFlow()
    }

    override fun onListenerConnected() = update()
    override fun onListenerDisconnected() { _active.value = emptySet() }
    override fun onNotificationPosted(sbn: StatusBarNotification?) = update()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = update()

    private fun update() {
        _active.value = try {
            activeNotifications.orEmpty()
                .filter { it.isClearable && it.packageName != packageName }
                .map { it.packageName }
                .toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }
}
