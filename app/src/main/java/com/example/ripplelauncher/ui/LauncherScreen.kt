package com.example.ripplelauncher.ui

import android.content.Intent
import android.provider.AlarmClock
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ripplelauncher.AppEntry
import com.example.ripplelauncher.LauncherViewModel
import com.example.ripplelauncher.Screen
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date

/** A request to jump the app list to a letter. The id makes repeat requests fire again. */
private data class ScrollRequest(val letter: Char, val id: Long = System.nanoTime())

@Composable
fun LauncherScreen(vm: LauncherViewModel) {
    val apps by vm.apps.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val favoriteKeys by vm.favoriteKeys.collectAsStateWithLifecycle()
    val screen by vm.screen.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()

    val letters = remember(apps) { apps.map { it.letter }.distinct() }
    val favoriteSet = remember(favoriteKeys) { favoriteKeys.toSet() }
    var scrollRequest by remember { mutableStateOf<ScrollRequest?>(null) }
    val focus = LocalFocusManager.current

    // Back closes the app list; on the home screen it does nothing.
    BackHandler(enabled = screen == Screen.Drawer) { vm.goHome() }
    LaunchedEffect(screen) { if (screen == Screen.Home) scrollRequest = null }

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
            label = "screen",
        ) { s ->
            when (s) {
                Screen.Home -> HomeContent(favorites, vm)
                Screen.Drawer -> DrawerContent(apps, favoriteSet, query, scrollRequest, vm)
            }
        }

        if (query.isBlank() && letters.isNotEmpty()) {
            AlphabetScroller(
                letters = letters,
                onLetterSelected = { letter ->
                    focus.clearFocus()
                    if (screen == Screen.Home) vm.openDrawer()
                    scrollRequest = ScrollRequest(letter)
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(0.72f)
                    .width(36.dp)
                    .padding(end = 6.dp),
            )
        }
    }
}

// --- Home screen --------------------------------------------------------------

@Composable
private fun HomeContent(favorites: List<AppEntry>, vm: LauncherViewModel) {
    var dragTotal by remember { mutableFloatStateOf(0f) }

    Column(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { dragTotal = 0f },
                    onDragEnd = {
                        when {
                            dragTotal < -120f -> vm.openDrawer()          // swipe up
                            dragTotal > 120f -> vm.expandNotifications()  // swipe down
                        }
                    },
                    onVerticalDrag = { change, amount ->
                        change.consume()
                        dragTotal += amount
                    },
                )
            }
            .systemBarsPadding()
            .padding(end = 48.dp),
    ) {
        Clock(Modifier.padding(start = 24.dp, top = 32.dp))
        Spacer(Modifier.weight(1f))

        if (favorites.isEmpty()) {
            Text(
                text = "Swipe up or drag the letters on the right to find apps.\n\n" +
                    "Long-press any app to pin it here.",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyLarge.copy(shadow = TextShadow),
                modifier = Modifier.padding(24.dp),
            )
        } else {
            favorites.forEachIndexed { i, app ->
                AppRow(app, isFavorite = true, vm = vm, big = true, canMoveUp = i > 0, canMoveDown = i < favorites.lastIndex)
            }
        }
        Spacer(Modifier.height(56.dp))
    }
}

@Composable
private fun Clock(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // Ticks exactly on each minute boundary.
    val now by produceState(Date()) {
        while (true) {
            value = Date()
            delay(60_000L - System.currentTimeMillis() % 60_000L)
        }
    }
    val time = DateFormat.getTimeFormat(context).format(now) // respects the 12/24h setting
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))

    Column(
        modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        ) {
            try {
                context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Exception) {
            }
        },
    ) {
        Text(
            time,
            color = Color.White,
            fontSize = 64.sp,
            fontWeight = FontWeight.Light,
            style = MaterialTheme.typography.displayLarge.copy(shadow = TextShadow),
        )
        Text(
            date,
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.titleMedium.copy(shadow = TextShadow),
        )
    }
}

// --- All apps: honeycomb ------------------------------------------------------

@Composable
private fun DrawerContent(
    apps: List<AppEntry>,
    favoriteKeys: Set<String>,
    query: String,
    scrollRequest: ScrollRequest?,
    vm: LauncherViewModel,
) {
    val shown = remember(apps, query) {
        if (query.isBlank()) apps else {
            val q = query.trim()
            apps.filter { it.label.contains(q, ignoreCase = true) }
                .sortedBy { !it.label.startsWith(q, ignoreCase = true) } // prefix matches first
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .systemBarsPadding()
            .imePadding(),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = vm::setQuery,
            placeholder = { Text("Search apps") },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { shown.firstOrNull()?.let { vm.launch(it) } }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White.copy(alpha = 0.8f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                cursorColor = Color.White,
                focusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 56.dp, top = 12.dp, bottom = 8.dp),
        )

        HoneycombGrid(
            apps = shown,
            favoriteKeys = favoriteKeys,
            vm = vm,
            jumpLetter = if (query.isBlank()) scrollRequest?.letter else null,
            jumpId = scrollRequest?.id ?: 0L,
            resetKey = query,
            grouped = query.isBlank(),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 8.dp, end = 44.dp), // room for the alphabet scroller
        )
    }
}

// --- Home screen row ------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRow(
    app: AppEntry,
    isFavorite: Boolean,
    vm: LauncherViewModel,
    big: Boolean,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { vm.launch(app) },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuOpen = true
                    },
                )
                .padding(horizontal = 24.dp, vertical = if (big) 10.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(app.icon, contentDescription = null, modifier = Modifier.size(if (big) 40.dp else 34.dp))
            Spacer(Modifier.width(16.dp))
            Text(
                app.label,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = (if (big) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium)
                    .copy(shadow = TextShadow),
            )
        }
        AppMenu(app, isFavorite, vm, menuOpen, { menuOpen = false }, canMoveUp, canMoveDown)
    }
}

// --- Long-press menu, shared by the home list and the honeycomb ---------------

@Composable
internal fun AppMenu(
    app: AppEntry,
    isFavorite: Boolean,
    vm: LauncherViewModel,
    expanded: Boolean,
    onDismiss: () -> Unit,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(if (isFavorite) "Remove from home" else "Add to home") },
            onClick = { onDismiss(); vm.toggleFavorite(app) },
        )
        if (canMoveUp) DropdownMenuItem(
            text = { Text("Move up") },
            onClick = { onDismiss(); vm.moveFavorite(app, -1) },
        )
        if (canMoveDown) DropdownMenuItem(
            text = { Text("Move down") },
            onClick = { onDismiss(); vm.moveFavorite(app, 1) },
        )
        DropdownMenuItem(
            text = { Text("App info") },
            onClick = { onDismiss(); vm.openAppInfo(app) },
        )
        DropdownMenuItem(
            text = { Text("Uninstall") },
            onClick = { onDismiss(); vm.uninstall(app) },
        )
    }
}
