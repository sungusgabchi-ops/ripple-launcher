package com.example.ripplelauncher.ui

import android.content.Intent
import android.provider.AlarmClock
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import com.example.ripplelauncher.Backdrop
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.mutableFloatStateOf
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.ripplelauncher.R
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Arrangement
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
    /** The favorites grid. Rows alternate 5 / 4 / 5 icons. */
    data class Favorites(val apps: List<AppEntry>) : Item {
        override val key = "favorites"
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
            else list += Item.Favorites(favorites)
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

    val backdrop by vm.backdrop.collectAsStateWithLifecycle()
    val scrim = when (backdrop) {
        Backdrop.Wallpaper -> Color.Transparent
        Backdrop.Dim -> Color.Black.copy(alpha = 0.35f)
        Backdrop.Dark -> Color.Black.copy(alpha = 0.7f)
        Backdrop.Black -> Color.Black
    }
    val animatedScrim by animateColorAsState(scrim, tween(300), label = "backdrop")

    Box(Modifier.fillMaxSize().background(animatedScrim)) {
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
                    is Item.Favorites -> FavoriteGrid(item.apps, vm)
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
    // Top row: camera, theme and settings. Next line: time and date.
    Column(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp), horizontalArrangement = Arrangement.Start) {
            TopActions(vm)
        }
        Clock(Modifier.padding(start = 24.dp, top = 8.dp))
        Spacer(Modifier.height(28.dp))
        BrightnessBar(vm)
        Spacer(Modifier.height(24.dp))
        SearchBar(query, vm, onGo)
    }
}

/** Screen brightness slider. Asks for the system permission on first use. */
@Composable
private fun BrightnessBar(vm: LauncherViewModel) {
    var level by remember { mutableFloatStateOf(vm.readBrightness()) }
    var allowed by remember { mutableStateOf(vm.canChangeBrightness()) }

    // Coming back from Settings (or after brightness changed elsewhere): refresh.
    LifecycleResumeEffect(Unit) {
        allowed = vm.canChangeBrightness()
        level = vm.readBrightness()
        onPauseOrDispose { }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(R.drawable.ic_brightness),
            contentDescription = "Brightness",
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(12.dp))
        Slider(
            value = level,
            onValueChange = {
                if (allowed) {
                    level = it
                    vm.setBrightness(it)
                } else {
                    vm.requestBrightnessPermission()
                }
            },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White.copy(alpha = 0.85f),
                inactiveTrackColor = Color.White.copy(alpha = 0.22f),
            ),
            modifier = Modifier.weight(1f).height(32.dp),
        )
        Spacer(Modifier.width(12.dp))
        Icon(
            painterResource(R.drawable.ic_brightness),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(22.dp),
        )
    }
}

/** Top right: appearance menu and a shortcut to Android's Settings. */
@Composable
private fun TopActions(vm: LauncherViewModel) {
    val backdrop by vm.backdrop.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    Row(Modifier.padding(end = 4.dp)) {
        IconButton(onClick = { vm.openCamera() }) {
            Icon(
                painterResource(R.drawable.ic_camera),
                contentDescription = "Camera",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(22.dp),
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Rounded.Edit, contentDescription = "Appearance", tint = Color.White.copy(alpha = 0.85f))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                Text(
                    "Background",
                    style = MaterialTheme.typography.labelLarge,
                    color = Accent,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Backdrop.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        trailingIcon = {
                            if (option == backdrop) Icon(Icons.Rounded.Check, contentDescription = null, tint = Accent)
                        },
                        onClick = { vm.setBackdrop(option); menuOpen = false },
                    )
                }
            }
        }
        IconButton(onClick = { vm.openSystemSettings() }) {
            Icon(Icons.Rounded.Settings, contentDescription = "System settings", tint = Color.White.copy(alpha = 0.85f))
        }
    }
}

/** A raised, frosted pill so search stands apart from the app list. */
@Composable
private fun SearchBar(query: String, vm: LauncherViewModel, onGo: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    BasicTextField(
        value = query,
        onValueChange = vm::setQuery,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
        cursorBrush = SolidColor(Accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { onGo() }),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(elevation = 10.dp, shape = shape, ambientColor = Color.Black, spotColor = Color.Black)
            .background(
                Brush.verticalGradient(listOf(Color(0xFF3A404A), Color(0xFF262A31))),
                shape,
            )
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
            .height(52.dp),
        decorationBox = { field ->
            Row(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = Accent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text("Search apps", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodyLarge)
                    }
                    field()
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = { vm.setQuery("") }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        },
    )
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
    // One line, e.g. "9:46 AM · Sun, Oct 18". Shows AM/PM unless the phone is set to 24-hour.
    val pattern = if (DateFormat.is24HourFormat(context)) "H:mm" else "h:mm a"
    val time = java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault()).format(now)
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d"))

    Row(
        modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        ) {
            try {
                context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Exception) {
            }
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            time,
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium.copy(shadow = TextShadow),
        )
        Text(
            "  ·  $date",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 20.sp,
            style = MaterialTheme.typography.bodyMedium.copy(shadow = TextShadow),
        )
    }
}

// --- Favorites grid: icons only, 5 / 4 / 5 like a honeycomb ----------------
// Tap to open. Long-press and drag to reorder. Long-press without moving opens the menu.

private val FavRowHeight = 60.dp
private val FavIconSize = 40.dp

/** Row and horizontal position (in cell widths) of favorite number [i]. */
private fun slotOf(i: Int): Pair<Int, Float> {
    var row = 0
    var start = 0
    while (true) {
        val size = if (row % 2 == 0) 5 else 4
        if (i < start + size) {
            val col = i - start
            // A 4-icon row is shifted half a cell so it sits between the rows above and below.
            return row to (if (size == 5) col + 0.5f else col + 1f)
        }
        start += size
        row++
    }
}

private fun rowsFor(n: Int): Int = if (n == 0) 0 else slotOf(n - 1).first + 1

@Composable
private fun FavoriteGrid(favorites: List<AppEntry>, vm: LauncherViewModel) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    // Working order while dragging; resets whenever the saved favorites change.
    var order by remember(favorites) { mutableStateOf(favorites) }
    var dragKey by remember { mutableStateOf<String?>(null) }
    var dragCenter by remember { mutableStateOf(Offset.Zero) }
    var menuKey by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .height(FavRowHeight * rowsFor(order.size))
    ) {
        val cellW = with(density) { maxWidth.toPx() } / 5f
        val rowH = with(density) { FavRowHeight.toPx() }
        val half = with(density) { (FavIconSize / 2).toPx() }
        val hitRadius = cellW * 0.5f

        fun centerOf(i: Int): Offset {
            val (row, x) = slotOf(i)
            return Offset(x * cellW, (row + 0.5f) * rowH)
        }
        fun nearest(p: Offset): Int =
            order.indices.minByOrNull { (centerOf(it) - p).getDistance() } ?: 0

        val latestOrder by rememberUpdatedState(order)

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(cellW) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val current = latestOrder
                        val hit = current.indices.firstOrNull {
                            (centerOf(it) - down.position).getDistance() < hitRadius
                        } ?: return@awaitEachGesture
                        val app = current[hit]

                        // 1 = released (tap), 2 = cancelled (e.g. list scrolled), 0 = held long enough
                        var outcome = 0
                        withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                            val up = waitForUpOrCancellation()
                            outcome = if (up != null) { up.consume(); 1 } else 2
                        }
                        if (outcome == 1) { vm.launch(app); return@awaitEachGesture }
                        if (outcome == 2) return@awaitEachGesture

                        // Long press: pick the icon up.
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val grab = centerOf(hit) - down.position
                        dragKey = app.key
                        dragCenter = centerOf(hit)
                        var moved = false
                        var total = Offset.Zero
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) { change.consume(); break }
                            total += change.positionChange()
                            if (!moved && total.getDistance() > viewConfiguration.touchSlop) moved = true
                            if (moved) {
                                dragCenter = change.position + grab
                                val from = latestOrder.indexOfFirst { it.key == app.key }
                                val to = nearest(dragCenter)
                                if (from != -1 && to != from) {
                                    order = latestOrder.toMutableList().apply { add(to, removeAt(from)) }
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                            change.consume()
                        }
                        dragKey = null
                        if (moved) vm.setFavoriteOrder(order.map { it.key })
                        else menuKey = app.key
                    }
                }
        ) {
            order.forEachIndexed { i, app ->
                key(app.key) {
                    val dragging = app.key == dragKey
                    val target = centerOf(i)
                    val slot = IntOffset((target.x - half).roundToInt(), (target.y - half).roundToInt())
                    val animated by animateIntOffsetAsState(slot, tween(180), label = "fav")
                    val scale by animateFloatAsState(if (dragging) 1.2f else 1f, tween(120), label = "lift")
                    Box(
                        Modifier
                            .offset {
                                if (dragging) IntOffset(
                                    (dragCenter.x - half).roundToInt(),
                                    (dragCenter.y - half).roundToInt(),
                                ) else animated
                            }
                            .zIndex(if (dragging) 1f else 0f)
                    ) {
                        Image(
                            app.icon,
                            contentDescription = app.label,
                            modifier = Modifier
                                .size(FavIconSize)
                                .graphicsLayer { scaleX = scale; scaleY = scale },
                        )
                        AppMenu(
                            app, isFavorite = true, vm = vm,
                            expanded = menuKey == app.key, onDismiss = { menuKey = null },
                        )
                    }
                }
            }
        }
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
