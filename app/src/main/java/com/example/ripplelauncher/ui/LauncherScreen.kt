package com.example.ripplelauncher.ui

import android.content.Intent
import android.provider.AlarmClock
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ripplelauncher.AppEntry
import com.example.ripplelauncher.LauncherViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date

/** The scroller's first entry: jumps back to the favorites at the top. */
private const val FAVORITES_MARK = '•'

/** Everything in the single Niagara-style list, top to bottom. */
private sealed interface Item {
    val key: String

    data object Header : Item { override val key = "header" }
    data object Hint : Item { override val key = "hint" }
    data object Gap : Item { override val key = "gap" }
    /** One row of the favorites grid. Rows alternate 5 / 4 / 5 icons. */
    data class FavoriteRow(val row: Int, val apps: List<AppEntry>, val start: Int, val total: Int) : Item {
        override val key = "favrow_$row"
    }
    data class App(val app: AppEntry, val letter: Char?) : Item {
        override val key = "app_${app.key}"
    }
}

@Composable
fun LauncherScreen(vm: LauncherViewModel) {
    val apps by vm.apps.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val favoriteKeys by vm.favoriteKeys.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val homeEvents by vm.homeEvents.collectAsStateWithLifecycle()

    val favoriteSet = remember(favoriteKeys) { favoriteKeys.toSet() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current

    // Build the list: clock, favorites, then every app grouped by letter.
    // letterIndex maps each scroller letter to the list position it jumps to.
    val (items, letterIndex) = remember(apps, favorites, query) {
        val list = mutableListOf<Item>(Item.Header)
        val index = mutableMapOf(FAVORITES_MARK to 0)
        if (query.isBlank()) {
            if (favorites.isEmpty()) list += Item.Hint
            var start = 0
            var row = 0
            while (start < favorites.size) {
                val size = if (row % 2 == 0) 5 else 4
                val end = minOf(start + size, favorites.size)
                list += Item.FavoriteRow(row, favorites.subList(start, end), start, favorites.size)
                start = end
                row++
            }
            list += Item.Gap
            var last: Char? = null
            for (app in apps) {
                val first = app.letter != last
                if (first) index[app.letter] = list.size
                list += Item.App(app, if (first) app.letter else null)
                last = app.letter
            }
        } else {
            val q = query.trim()
            apps.filter { it.label.contains(q, ignoreCase = true) }
                .sortedBy { !it.label.startsWith(q, ignoreCase = true) } // prefix matches first
                .forEach { list += Item.App(it, null) }
        }
        list.toList() to index.toMap()
    }
    val letters = remember(apps) { listOf(FAVORITES_MARK) + apps.map { it.letter }.distinct() }

    // Home pressed or an app launched: glide back to the top.
    LaunchedEffect(homeEvents) {
        if (homeEvents > 0) {
            focus.clearFocus()
            listState.animateScrollToItem(0)
        }
    }

    val scrolledDown by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    BackHandler(enabled = scrolledDown || query.isNotEmpty()) { vm.goHome() }

    // Pull down at the very top (past the clock) to open notifications.
    val pullDown = remember {
        object : NestedScrollConnection {
            var pulled = 0f
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) pulled += available.y
                if (consumed.y < 0f) pulled = 0f
                return Offset.Zero
            }
            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pulled > 160f) vm.expandNotifications()
                pulled = 0f
                return Velocity.Zero
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(pullDown)
                .systemBarsPadding()
                .imePadding(),
            contentPadding = PaddingValues(end = 48.dp, bottom = 96.dp),
        ) {
            items(items, key = { it.key }) { item ->
                when (item) {
                    Item.Header -> Header(query, vm, onGo = {
                        (items.firstOrNull { it is Item.App } as? Item.App)?.let { vm.launch(it.app) }
                    })
                    Item.Hint -> Text(
                        "Long-press any app below to add it to favorites.",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyLarge.copy(shadow = TextShadow),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    )
                    Item.Gap -> Spacer(Modifier.height(28.dp))
                    is Item.FavoriteRow -> FavoriteRow(item, vm)
                    is Item.App -> AppRow(
                        app = item.app,
                        letter = item.letter,
                        isFavorite = item.app.key in favoriteSet,
                        vm = vm,
                        big = false,
                    )
                }
            }
        }

        if (query.isBlank() && apps.isNotEmpty()) {
            AlphabetScroller(
                letters = letters,
                onLetterSelected = { letter ->
                    focus.clearFocus()
                    letterIndex[letter]?.let { i -> scope.launch { listState.scrollToItem(i) } }
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

// --- Top of the list: clock and search ----------------------------------------

@Composable
private fun Header(query: String, vm: LauncherViewModel, onGo: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 96.dp, bottom = 20.dp)) {
        Clock(Modifier.padding(start = 24.dp))
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = query,
            onValueChange = vm::setQuery,
            placeholder = { Text("Search") },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onGo() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White.copy(alpha = 0.7f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                cursorColor = Color.White,
                focusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
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
            fontSize = 56.sp,
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

// --- Favorites grid: icons only, 5 / 4 / 5 like a honeycomb ----------------

@Composable
private fun FavoriteRow(item: Item.FavoriteRow, vm: LauncherViewModel) {
    val narrow = item.row % 2 == 1
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // A 4-icon row is shifted half a cell so it sits between the icons above and below.
        if (narrow) Spacer(Modifier.weight(0.5f))
        val slots = if (narrow) 4 else 5
        for (slot in 0 until slots) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                val app = item.apps.getOrNull(slot)
                if (app != null) {
                    val index = item.start + slot
                    FavoriteIcon(app, vm, canMoveLeft = index > 0, canMoveRight = index < item.total - 1)
                }
            }
        }
        if (narrow) Spacer(Modifier.weight(0.5f))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteIcon(app: AppEntry, vm: LauncherViewModel, canMoveLeft: Boolean, canMoveRight: Boolean) {
    var menuOpen by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    Box {
        Image(
            app.icon,
            contentDescription = app.label,
            modifier = Modifier
                .size(40.dp)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { vm.launch(app) },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuOpen = true
                    },
                ),
        )
        AppMenu(
            app, isFavorite = true, vm = vm,
            expanded = menuOpen, onDismiss = { menuOpen = false },
            canMoveUp = canMoveLeft, canMoveDown = canMoveRight,
            moveUpLabel = "Move left", moveDownLabel = "Move right",
        )
    }
}

// --- One app row: favorites are big, the A–Z list has a letter column ---------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRow(
    app: AppEntry,
    letter: Char?,
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
                .padding(
                    start = if (big) 24.dp else 12.dp,
                    end = 12.dp,
                    top = if (big) 9.dp else 7.dp,
                    bottom = if (big) 9.dp else 7.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!big) {
                // Niagara-style: the group's letter sits beside its first app.
                Box(Modifier.width(32.dp), contentAlignment = Alignment.Center) {
                    if (letter != null) {
                        Text(
                            letter.toString(),
                            color = Accent,
                            style = MaterialTheme.typography.titleMedium.copy(shadow = TextShadow),
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
            }
            Image(app.icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(14.dp))
            Text(
                app.label,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = (if (big) MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp) else MaterialTheme.typography.bodyLarge)
                    .copy(shadow = TextShadow),
            )
        }
        AppMenu(app, isFavorite, vm, menuOpen, { menuOpen = false }, canMoveUp, canMoveDown)
    }
}

// --- Long-press menu ------------------------------------------------------------

@Composable
private fun AppMenu(
    app: AppEntry,
    isFavorite: Boolean,
    vm: LauncherViewModel,
    expanded: Boolean,
    onDismiss: () -> Unit,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    moveUpLabel: String = "Move up",
    moveDownLabel: String = "Move down",
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(if (isFavorite) "Remove from favorites" else "Add to favorites") },
            onClick = { onDismiss(); vm.toggleFavorite(app) },
        )
        if (canMoveUp) DropdownMenuItem(
            text = { Text(moveUpLabel) },
            onClick = { onDismiss(); vm.moveFavorite(app, -1) },
        )
        if (canMoveDown) DropdownMenuItem(
            text = { Text(moveDownLabel) },
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
