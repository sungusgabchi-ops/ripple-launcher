package com.example.ripplelauncher.ui

import android.content.Intent
import android.provider.AlarmClock
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.window.Dialog
import com.example.ripplelauncher.Destination
import com.example.ripplelauncher.Section
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.ripplelauncher.R
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.pointer.positionChange
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

/** The scroller's last entry: jumps to the "Less used" section. */
private const val LESS_USED_MARK = '⋯'

/** Everything in the single Niagara-style list, top to bottom. */
private sealed interface Item {
    val key: String

    data object Hint : Item { override val key = "hint" }
    data object Gap : Item { override val key = "gap" }
    data object SectionGap : Item { override val key = "section_gap" }
    /** The favorites grid. Rows alternate 5 / 4 / 5 icons. */
    data class Favorites(val apps: List<AppEntry>) : Item {
        override val key = "favorites"
    }
    data class App(val app: AppEntry, val letter: Char?, val lessUsed: Boolean = false) : Item {
        override val key = "app_${app.key}"
    }
    /** Tap to expand or collapse the less-used apps below it. */
    data class LessUsedHeader(val count: Int, val expanded: Boolean) : Item {
        override val key = "less_used_header"
    }
    /** A named section between favorites and A. Tap to expand, long-press for options. */
    data class SectionHeader(
        val section: Section, val count: Int, val expanded: Boolean, val first: Boolean, val last: Boolean,
    ) : Item {
        override val key = "section_${section.id}"
    }
    /** Shown inside an expanded section that has no apps yet. */
    /** A section's or Hidden's apps, as an icon grid like the favorites. */
    data class Grid(val id: String, val apps: List<AppEntry>, val faded: Boolean = false) : Item {
        override val key = "grid_$id"
    }
    data class SectionEmpty(val section: Section) : Item {
        override val key = "section_empty_${section.id}"
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
    val lessUsedKeys by vm.lessUsedKeys.collectAsStateWithLifecycle()
    var lessUsedExpanded by rememberSaveable { mutableStateOf(false) }
    val sections by vm.sections.collectAsStateWithLifecycle()
    val sectionOf by vm.sectionOf.collectAsStateWithLifecycle()
    var expandedSections by rememberSaveable { mutableStateOf(listOf<String>()) }
    var renaming by remember { mutableStateOf<Section?>(null) }
    var deleting by remember { mutableStateOf<Section?>(null) }

    val groupOrder by vm.groupOrder.collectAsStateWithLifecycle()
    val dots by vm.dotPackages.collectAsStateWithLifecycle()
    // Saved drag order first; apps never placed yet follow alphabetically.
    fun ordered(groupId: String, members: List<AppEntry>): List<AppEntry> {
        val order = groupOrder[groupId] ?: return members
        val rank = order.withIndex().associate { (i, k) -> k to i }
        return members.sortedBy { rank[it.key] ?: Int.MAX_VALUE }
    }

    val (items, letterIndex) = remember(
        apps, favorites, favoriteSet, query, lessUsedKeys, lessUsedExpanded, sections, sectionOf, expandedSections, groupOrder,
    ) {
        val list = mutableListOf<Item>()
        val index = mutableMapOf(FAVORITES_MARK to 0)
        if (query.isBlank()) {
            if (favorites.isEmpty()) list += Item.Hint
            else list += Item.Favorites(favorites)
            list += Item.Gap
            var last: Char? = null
            // Each app shows in one place only: favorites, a section, A–Z, or Hidden.
            val notFavorite = apps.filter { it.key !in favoriteSet }
            val sectionIds = sections.map { it.id }.toSet()
            fun sectionFor(app: AppEntry) = sectionOf[app.key]?.takeIf { it in sectionIds }
            val (lessUsed, main) = notFavorite
                .filter { sectionFor(it) == null }
                .partition { it.key in lessUsedKeys }

            // Between favorites and A: your sections, each collapsible.
            sections.forEachIndexed { i, section ->
                val members = notFavorite.filter { sectionFor(it) == section.id }
                val open = section.id in expandedSections
                list += Item.SectionHeader(section, members.size, open, first = i == 0, last = i == sections.lastIndex)
                if (open) {
                    if (members.isEmpty()) list += Item.SectionEmpty(section)
                    else list += Item.Grid(section.id, ordered(section.id, members))
                }
            }
            if (sections.isNotEmpty()) list += Item.SectionGap
            for (app in main) {
                val first = app.letter != last
                if (first) index[app.letter] = list.size
                list += Item.App(app, if (first) app.letter else null)
                last = app.letter
            }
            // After Z: the collapsible "Less used" section.
            if (lessUsed.isNotEmpty()) {
                index[LESS_USED_MARK] = list.size
                list += Item.LessUsedHeader(lessUsed.size, lessUsedExpanded)
                if (lessUsedExpanded) list += Item.Grid("hidden", ordered("hidden", lessUsed), faded = true)
            }
        } else {
            // Search still finds every app, including less-used ones.
            val q = query.trim()
            apps.filter { it.label.contains(q, ignoreCase = true) }
                .sortedBy { !it.label.startsWith(q, ignoreCase = true) } // prefix matches first
                .forEach { list += Item.App(it, null) }
        }
        list.toList() to index.toMap()
    }
    val letters = remember(apps, favoriteSet, lessUsedKeys, sections, sectionOf) {
        val sectionIds = sections.map { it.id }.toSet()
        val (lessUsed, main) = apps
            .filter { it.key !in favoriteSet && sectionOf[it.key] !in sectionIds }
            .partition { it.key in lessUsedKeys }
        listOf(FAVORITES_MARK) + main.map { it.letter }.distinct() + if (lessUsed.isNotEmpty()) listOf(LESS_USED_MARK) else emptyList()
    }

    // Home pressed or an app launched: glide back to the top.
    LaunchedEffect(homeEvents) {
        if (homeEvents > 0) {
            focus.clearFocus()
            listState.animateScrollToItem(0)
        }
    }
    // Pressing Home folds every section back up, so the screen always starts tidy.
    val collapseEvents by vm.collapseEvents.collectAsStateWithLifecycle()
    LaunchedEffect(collapseEvents) {
        if (collapseEvents > 0) {
            expandedSections = emptyList()
            lessUsedExpanded = false
        }
    }

    val scrolledDown by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
    }
    val searchOpen by vm.searchOpen.collectAsStateWithLifecycle()
    BackHandler(enabled = scrolledDown || query.isNotEmpty() || searchOpen) { vm.goHome() }

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
                // Pull down on the list (already at the top) to open search.
                if (pulled > 160f) vm.openSearch()
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

    // The top area stays put. The favorites and app list scroll up *under* it,
    // where they show through as a soft, frosted blur, just a hint of what's there.
    val hazeState = remember { HazeState() }
    var headerPx by remember { mutableIntStateOf(0) }
    val headerHeight = with(LocalDensity.current) { headerPx.toDp() }
    val fade = 28.dp

    val enter = remember { Animatable(1f) }
    var wasAway by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { wasAway = true }
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        if (wasAway) {
            wasAway = false
            scope.launch {
                enter.snapTo(0f)
                enter.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(animatedScrim)
            .graphicsLayer {
                val t = enter.value
                alpha = 0.35f + 0.65f * t
                val sc = 0.965f + 0.035f * t
                scaleX = sc
                scaleY = sc
                translationY = (1f - t) * 24.dp.toPx()
            },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding(),
        ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(pullDown)
                // Hide the sharp copy under the top area, fading in just below it.
                // (The blurred copy the top area shows is taken before this mask.)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val bottom = headerPx.toFloat()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black),
                            startY = bottom - fade.toPx(),
                            endY = bottom,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                }
                .haze(hazeState),
            contentPadding = PaddingValues(top = headerHeight + 4.dp, end = 48.dp, bottom = 96.dp),
        ) {
            items(items, key = { it.key }) { item ->
                // Rows fade and slide into place when the list changes (e.g. expanding Less used).
                Box(Modifier.animateItem()) {
                when (item) {
                    Item.Hint -> Text(
                        "Long-press any app below to add it to favorites.",
                        style = Type.Caption,
                        color = Ui.InkMuted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = Ui.Gutter, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    )
                    Item.Gap -> Spacer(Modifier.height(16.dp))
                    Item.SectionGap -> Spacer(Modifier.height(10.dp))
                    is Item.Favorites -> FavoriteGrid(item.apps, vm, dots)
                    is Item.Grid -> Box(if (item.faded) Modifier.alpha(0.8f) else Modifier) {
                        ReorderGrid(item.apps, vm, dots, isFavorite = false) { vm.setGroupOrder(item.id, it) }
                    }
                    is Item.App -> Box(if (item.lessUsed) Modifier.alpha(0.75f) else Modifier) {
                        AppRow(
                            app = item.app,
                            letter = item.letter,
                            isFavorite = item.app.key in favoriteSet,
                            vm = vm,
                            big = false,
                            dot = item.app.component.packageName in dots,
                        )
                    }
                    is Item.LessUsedHeader -> SectionHeader("HIDDEN", item.count, item.expanded, divider = true) {
                        lessUsedExpanded = !lessUsedExpanded
                    }
                    is Item.SectionHeader -> CustomSectionHeader(
                        item = item,
                        vm = vm,
                        onToggle = {
                            val id = item.section.id
                            expandedSections = if (id in expandedSections) expandedSections - id else expandedSections + id
                        },
                        onRename = { renaming = item.section },
                        onDelete = { deleting = item.section },
                    )
                    is Item.SectionEmpty -> Text(
                        "Empty. Long-press any app and choose Move to… → ${item.section.name}.",
                        style = Type.Caption,
                        color = Ui.InkFaint,
                        modifier = Modifier.padding(start = Ui.Gutter, end = 8.dp, top = 2.dp, bottom = 10.dp),
                    )
                }
                }
            }
        }

        if (query.isBlank() && apps.isNotEmpty()) {
            // Beside the list only, below the top area.
            Box(Modifier.fillMaxSize().padding(top = headerHeight)) {
                AlphabetScroller(
                    letters = letters,
                    onLetterSelected = { letter ->
                        focus.clearFocus()
                        letterIndex[letter]?.let { i -> scope.launch { listState.scrollToItem(i) } }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(0.85f)
                        .width(36.dp)
                        .padding(end = 6.dp),
                )
            }
        }
        }

        // The fixed top area, frosted over whatever is scrolling beneath it.
        Box(
            Modifier
                .fillMaxWidth()
                .onSizeChanged { headerPx = it.height }
                .hazeChild(hazeState) {
                    backgroundColor = Color.Transparent
                    blurRadius = 26.dp
                    noiseFactor = 0f
                    tints = listOf(HazeTint(Color.Black.copy(alpha = 0.10f)))
                    alpha = 0.7f
                    // Soften the bottom edge so the blur melts into the list.
                    mask = Brush.verticalGradient(
                        0f to Color.Black,
                        0.82f to Color.Black,
                        1f to Color.Transparent,
                    )
                }
                .statusBarsPadding(),
        ) {
            Header(query, vm, onGo = {
                (items.firstOrNull { it is Item.App } as? Item.App)?.let { vm.launch(it.app) }
            })
        }
    }

    renaming?.let { section ->
        NameDialog(
            title = "Rename section",
            initial = section.name,
            confirmLabel = "Save",
            onConfirm = { vm.renameSection(section.id, it); renaming = null },
            onDismiss = { renaming = null },
        )
    }
    deleting?.let { section ->
        ConfirmDialog(
            title = "Delete “${section.name}”?",
            message = "The section is removed. Its apps go back to the A–Z list.",
            confirmLabel = "Delete",
            onConfirm = { vm.deleteSection(section.id); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

// --- Fixed top area: weather, clock, brightness, icon row ---------------------

@Composable
private fun Header(query: String, vm: LauncherViewModel, onGo: () -> Unit) {
    // Weather, time and date at the top; brightness; then a row of glass buttons
    // (search, camera, theme, settings). Tapping search swaps the row for the search box.
    val searchOpen by vm.searchOpen.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp)) {
        WeatherLine(vm, Modifier.padding(horizontal = Ui.Gutter))
        Spacer(Modifier.height(4.dp))
        Clock(Modifier.padding(horizontal = Ui.Gutter), onClick = vm::openClockApp)
        Spacer(Modifier.height(22.dp))
        BrightnessBar(vm)
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Ui.Gutter)
                .height(48.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            AnimatedContent(
                targetState = searchOpen || query.isNotEmpty(),
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "search",
            ) { open ->
                if (open) SearchBar(query, vm, onGo) else ActionRow(vm)
            }
        }
    }
}

/** e.g. [cloud]  52°  Cloudy · H 60° L 45° · Chicago. Tap opens the weather app. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WeatherLine(vm: LauncherViewModel, modifier: Modifier = Modifier) {
    val weather by vm.weather.collectAsStateWithLifecycle()
    var granted by remember { mutableStateOf(vm.hasLocationPermission()) }
    val askLocation = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        if (ok) vm.refreshWeather(force = true)
    }

    // Refresh whenever the home screen comes back into view (at most every 30 minutes).
    LifecycleResumeEffect(Unit) {
        granted = vm.hasLocationPermission()
        vm.refreshWeather()
        onPauseOrDispose { }
    }

    // Tap: open the weather app. Long-press: refresh now.
    val haptic = LocalHapticFeedback.current
    val tap = Modifier.combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = {
            if (granted) vm.openWeatherApp()
            else askLocation.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        },
        onLongClick = {
            if (granted) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.refreshWeather(force = true)
            }
        },
    )

    val w = weather
    Row(modifier.then(tap).height(24.dp), verticalAlignment = Alignment.CenterVertically) {
        when {
            !granted -> Text("Tap to show weather", style = Type.Caption, color = Ui.InkMuted)
            w == null -> Text("Loading weather…", style = Type.Caption, color = Ui.InkFaint)
            else -> {
                Icon(painterResource(w.icon), contentDescription = w.summary, tint = Ui.Ink, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("${w.temp}°", style = Type.BodyStrong, color = Ui.Ink)
                Spacer(Modifier.width(10.dp))
                Text(
                    buildString {
                        append("${w.summary}  ·  H ${w.high}°  L ${w.low}°")
                        w.place?.let { append("  ·  $it") }
                    },
                    style = Type.Caption,
                    color = Ui.InkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Brightness as a glass pill that fills from the left, like Control Center.
 * Drag anywhere on it. Asks for the system permission on first use.
 */
@Composable
private fun BrightnessBar(vm: LauncherViewModel) {
    val level by vm.brightness.collectAsStateWithLifecycle()
    var allowed by remember { mutableStateOf(vm.canChangeBrightness()) }
    val haptic = LocalHapticFeedback.current

    // Coming back from Settings: re-check permission and the current value.
    LifecycleResumeEffect(Unit) {
        allowed = vm.canChangeBrightness()
        vm.syncBrightness()
        onPauseOrDispose { }
    }

    val shape = RoundedCornerShape(22.dp)
    val shown by animateFloatAsState(level, spring(stiffness = Spring.StiffnessHigh), label = "brightness")
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Ui.Gutter)
            .height(44.dp)
            .glass(shape)
            .clip(shape)
            .pointerInput(allowed) {
                detectTapGestures { if (!allowed) vm.requestBrightnessPermission() }
            }
            .pointerInput(allowed) {
                detectHorizontalDragGestures(
                    onDragStart = { if (allowed) vm.startBrightnessDrag() else vm.requestBrightnessPermission() },
                    onDragEnd = {
                        vm.endBrightnessDrag()
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onDragCancel = { vm.endBrightnessDrag() },
                ) { change, dx ->
                    if (allowed) {
                        change.consume()
                        vm.setBrightness(vm.brightness.value + dx / size.width)
                    }
                }
            },
    ) {
        val minFill = 44.dp
        val fill = maxOf(minFill, maxWidth * shown)
        Box(
            Modifier
                .fillMaxHeight()
                .width(fill)
                .background(Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.78f), Color.White.copy(alpha = 0.92f)))),
        )
        Icon(
            painterResource(R.drawable.ic_brightness),
            contentDescription = "Brightness",
            tint = Color(0xFF2A2E35),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp)
                .size(20.dp),
        )
    }
}

/** A round glass button with an outline icon. */
@Composable
private fun GlassButton(icon: Int, description: String, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(48.dp)
            .pressScale(source)
            .glass(CircleShape)
            .clip(CircleShape)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = description, tint = Ui.Ink.copy(alpha = 0.92f), modifier = Modifier.size(22.dp))
    }
}

/** Search, camera, appearance and system settings. */
@Composable
private fun ActionRow(vm: LauncherViewModel) {
    val backdrop by vm.backdrop.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        GlassButton(R.drawable.ic_search, "Search") { vm.openSearch() }
        GlassButton(R.drawable.ic_camera, "Camera") { vm.openCamera() }
        Box {
            GlassButton(R.drawable.ic_palette, "Appearance") { menuOpen = true }
            RippleMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                val dotsOn by vm.dotsEnabled.collectAsStateWithLifecycle()
                MenuLabel("Home screen")
                RippleMenuItem(
                    text = "Notification dots",
                    onClick = { vm.toggleDots(); menuOpen = false },
                    trailing = {
                        if (dotsOn) Icon(painterResource(R.drawable.ic_check), contentDescription = "On", tint = Accent, modifier = Modifier.size(20.dp))
                    },
                )
                MenuLabel("Background")
                Backdrop.entries.forEach { option ->
                    RippleMenuItem(
                        text = option.label,
                        onClick = { vm.setBackdrop(option); menuOpen = false },
                        trailing = {
                            if (option == backdrop) {
                                Icon(painterResource(R.drawable.ic_check), contentDescription = "Selected", tint = Accent, modifier = Modifier.size(20.dp))
                            }
                        },
                    )
                }
            }
        }
        GlassButton(R.drawable.ic_settings, "System settings") { vm.openSystemSettings() }
        Spacer(Modifier.weight(1f))
        // Far right: create a new section.
        GlassButton(R.drawable.ic_add, "New section") { creating = true }
    }
    if (creating) {
        NameDialog(
            title = "New section",
            initial = "",
            confirmLabel = "Create",
            onConfirm = { vm.createSection(it); creating = false },
            onDismiss = { creating = false },
        )
    }
}

/** The search box, in the same glass style as the buttons it replaces. */
@Composable
private fun SearchBar(query: String, vm: LauncherViewModel, onGo: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }
    BasicTextField(
        value = query,
        onValueChange = vm::setQuery,
        singleLine = true,
        textStyle = Type.Body.copy(color = Ui.Ink, shadow = null),
        cursorBrush = SolidColor(Accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { onGo() }),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .glass(shape)
            .focusRequester(focusRequester),
        decorationBox = { field ->
            Row(
                Modifier.fillMaxSize().padding(start = 14.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_search), contentDescription = null, tint = Ui.InkMuted, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Search apps", style = Type.Body.copy(shadow = null), color = Ui.InkFaint)
                    field()
                }
                IconButton(onClick = { vm.closeSearch() }, modifier = Modifier.size(36.dp)) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = "Close search", tint = Ui.InkMuted, modifier = Modifier.size(20.dp))
                }
            }
        },
    )
}

/** Large, light time with a small AM/PM, then a quiet date: one line. */
@Composable
private fun Clock(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val context = LocalContext.current
    // Ticks exactly on each minute boundary.
    val now by produceState(Date()) {
        while (true) {
            value = Date()
            delay(60_000L - System.currentTimeMillis() % 60_000L)
        }
    }
    val is24 = DateFormat.is24HourFormat(context)
    val locale = java.util.Locale.getDefault()
    val time = java.text.SimpleDateFormat(if (is24) "H:mm" else "h:mm", locale).format(now)
    val amPm = if (is24) null else java.text.SimpleDateFormat("a", locale).format(now)
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d"))

    Row(
        modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
    ) {
        Text(time, style = Type.Display, color = Ui.Ink, modifier = Modifier.alignByBaseline())
        if (amPm != null) {
            Spacer(Modifier.width(5.dp))
            Text(amPm, style = Type.DisplaySuffix, color = Ui.Ink, modifier = Modifier.alignByBaseline())
        }
        Spacer(Modifier.width(14.dp))
        Text(date, style = Type.Body, color = Ui.InkMuted, modifier = Modifier.alignByBaseline())
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

/** Launches an app so it grows out of the icon that was tapped (bounds in window pixels). */
@Composable
private fun rememberLaunch(vm: LauncherViewModel): (AppEntry, androidx.compose.ui.geometry.Rect?) -> Unit {
    val view = LocalView.current
    return remember(view, vm) {
        { app, r ->
            if (r == null || r.width <= 0f) {
                vm.launch(app)
            } else {
                val x = r.left.roundToInt()
                val y = r.top.roundToInt()
                val w = r.width.roundToInt()
                val h = r.height.roundToInt()
                val opts = android.app.ActivityOptions.makeScaleUpAnimation(view, x, y, w, h).toBundle()
                vm.launch(app, android.graphics.Rect(x, y, x + w, y + h), opts)
            }
        }
    }
}

/** Small accent dot for apps with notifications. */
@Composable
private fun NotificationDot(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(9.dp)
            .border(1.5.dp, Color(0xFF1E2126), CircleShape)
            .background(Accent, CircleShape),
    )
}

@Composable
private fun FavoriteGrid(favorites: List<AppEntry>, vm: LauncherViewModel, dots: Set<String>) =
    ReorderGrid(favorites, vm, dots, isFavorite = true, onReorder = vm::setFavoriteOrder)

/**
 * Icons in a 5 / 4 / 5 grid: favorites, sections and Hidden all use this.
 * Tap to open. Long-press and drag to reorder. Long-press without moving opens the menu.
 */
@Composable
private fun ReorderGrid(
    apps: List<AppEntry>,
    vm: LauncherViewModel,
    dots: Set<String>,
    isFavorite: Boolean,
    onReorder: (List<String>) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val launchApp = rememberLaunch(vm)

    // Working order while dragging. Picks up outside changes (installs, moves) only
    // when nothing is being dragged, so a background refresh can't snap it back mid-drag.
    var order by remember { mutableStateOf(apps) }
    var dragKey by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(apps) { if (dragKey == null) order = apps }
    var dragCenter by remember { mutableStateOf(Offset.Zero) }
    var menuKey by remember { mutableStateOf<String?>(null) }
    var pressedKey by remember { mutableStateOf<String?>(null) }
    val origin = remember { floatArrayOf(0f, 0f) } // grid's top-left in the window

    // No container: the icons sit straight on the wallpaper, between the gutters.
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = Ui.Gutter - 6.dp, end = 0.dp, bottom = if (isFavorite) 0.dp else 6.dp)
    ) {
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(FavRowHeight * rowsFor(order.size))
            .onGloballyPositioned { val p = it.positionInWindow(); origin[0] = p.x; origin[1] = p.y }
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
        fun boundsOf(i: Int): androidx.compose.ui.geometry.Rect {
            val c = centerOf(i)
            return androidx.compose.ui.geometry.Rect(
                origin[0] + c.x - half, origin[1] + c.y - half,
                origin[0] + c.x + half, origin[1] + c.y + half,
            )
        }

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
                        pressedKey = app.key
                        try {

                        // 1 = released (tap), 2 = cancelled (e.g. list scrolled), 0 = held long enough
                        var outcome = 0
                        withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                            val up = waitForUpOrCancellation()
                            outcome = if (up != null) { up.consume(); 1 } else 2
                        }
                        if (outcome == 1) { launchApp(app, boundsOf(hit)); return@awaitEachGesture }
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
                        if (moved) onReorder(order.map { it.key })
                        else menuKey = app.key
                        dragKey = null
                        } finally {
                            pressedKey = null
                            dragKey = null
                        }
                    }
                }
        ) {
            order.forEachIndexed { i, app ->
                key(app.key) {
                    val dragging = app.key == dragKey
                    val target = centerOf(i)
                    val slot = IntOffset((target.x - half).roundToInt(), (target.y - half).roundToInt())
                    val animated by animateIntOffsetAsState(slot, tween(180), label = "fav")
                    val scale by animateFloatAsState(
                        when {
                            dragging -> 1.2f
                            app.key == pressedKey -> 0.88f
                            else -> 1f
                        },
                        spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
                        label = "lift",
                    )
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
                        Box(Modifier.graphicsLayer { scaleX = scale; scaleY = scale }) {
                            Image(app.icon, contentDescription = app.label, modifier = Modifier.size(FavIconSize))
                            if (app.component.packageName in dots) {
                                NotificationDot(Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp))
                            }
                        }
                        AppMenu(
                            app, isFavorite = isFavorite, vm = vm,
                            expanded = menuKey == app.key, onDismiss = { menuKey = null },
                        )
                    }
                }
            }
        }
    }
    }
}

// --- One app row in the A–Z list ---------------------------------------------

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
    dot: Boolean = false,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val source = remember { MutableInteractionSource() }
    val launchApp = rememberLaunch(vm)
    val iconBounds = remember { arrayOfNulls<androidx.compose.ui.geometry.Rect>(1) }
    val pressed by source.collectIsPressedAsState()
    val highlight by animateColorAsState(
        if (pressed || menuOpen) Color.White.copy(alpha = 0.08f) else Color.Transparent,
        tween(120),
        label = "row",
    )
    val shape = RoundedCornerShape(14.dp)

    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .pressScale(source, pressed = 0.985f)
                .clip(shape)
                .background(highlight, shape)
                .combinedClickable(
                    interactionSource = source,
                    indication = null,
                    onClick = { launchApp(app, iconBounds[0]) },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuOpen = true
                    },
                )
                // 8 + 12 = the 20dp gutter everything else lines up to.
                .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Niagara-style: the group's letter sits beside its first app.
            Box(Modifier.width(22.dp), contentAlignment = Alignment.CenterStart) {
                if (letter != null) Text(letter.toString(), style = Type.Label.copy(fontSize = 13.sp, letterSpacing = 0.sp), color = Accent)
            }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.onGloballyPositioned { iconBounds[0] = it.boundsInWindow() }) {
                Image(app.icon, contentDescription = null, modifier = Modifier.size(32.dp))
                if (dot) NotificationDot(Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp))
            }
            Spacer(Modifier.width(14.dp))
            Text(app.label, style = Type.Body, color = Ui.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        AppMenu(app, isFavorite, vm, menuOpen, { menuOpen = false }, canMoveUp, canMoveDown)
    }
}

// --- "Less used" section header, after Z ----------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    expanded: Boolean,
    divider: Boolean,
    onLongClick: (() -> Unit)? = null,
    onToggle: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val turn by animateFloatAsState(if (expanded) 180f else 0f, tween(220), label = "chevron")
    Column(Modifier.fillMaxWidth().padding(top = if (divider) 24.dp else 0.dp)) {
        if (divider) Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Ui.Gutter)
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.12f)),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggle,
                    onLongClick = onLongClick?.let { f -> { haptic.performHapticFeedback(HapticFeedbackType.LongPress); f() } },
                )
                .padding(start = Ui.Gutter, end = 12.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = Type.Label, color = Accent)
            Spacer(Modifier.width(10.dp))
            Text("$count", style = Type.Label, color = Ui.InkFaint)
            Spacer(Modifier.weight(1f))
            Icon(
                painterResource(R.drawable.ic_chevron_down),
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = Ui.InkMuted,
                modifier = Modifier.size(22.dp).graphicsLayer { rotationZ = turn },
            )
        }
    }
}

// --- A named section's header, with its long-press menu -------------------------

@Composable
private fun CustomSectionHeader(
    item: Item.SectionHeader,
    vm: LauncherViewModel,
    onToggle: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        SectionHeader(
            title = item.section.name.uppercase(),
            count = item.count,
            expanded = item.expanded,
            divider = false,
            onLongClick = { menuOpen = true },
            onToggle = onToggle,
        )
        RippleMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            MenuLabel(item.section.name)
            RippleMenuItem("Rename", { menuOpen = false; onRename() })
            if (!item.first) RippleMenuItem("Move up", { menuOpen = false; vm.moveSection(item.section.id, -1) })
            if (!item.last) RippleMenuItem("Move down", { menuOpen = false; vm.moveSection(item.section.id, 1) })
            RippleMenuItem("Delete section", { menuOpen = false; onDelete() }, destructive = true)
        }
    }
}

// --- Long-press menu for an app ---------------------------------------------------

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
    // Page 0: actions. Page 1: "Move to…" destinations. Resets each time the menu opens.
    var page by remember(expanded) { mutableStateOf(0) }

    RippleMenu(expanded = expanded, onDismissRequest = onDismiss) {
        // Read inside the menu so closed menus (one per app) cost nothing while scrolling.
        val sections by vm.sections.collectAsStateWithLifecycle()
        vm.favoriteKeys.collectAsStateWithLifecycle().value
        vm.sectionOf.collectAsStateWithLifecycle().value
        vm.lessUsedKeys.collectAsStateWithLifecycle().value
        val current = vm.destinationOf(app)

        if (page == 0) {
            MenuLabel(app.label)
            RippleMenuItem(if (isFavorite) "Remove from favorites" else "Add to favorites", { onDismiss(); vm.toggleFavorite(app) })
            if (canMoveUp) RippleMenuItem(moveUpLabel, { onDismiss(); vm.moveFavorite(app, -1) })
            if (canMoveDown) RippleMenuItem(moveDownLabel, { onDismiss(); vm.moveFavorite(app, 1) })
            RippleMenuItem(
                "Move to…",
                { page = 1 },
                trailing = {
                    Icon(
                        painterResource(R.drawable.ic_back), contentDescription = null, tint = Ui.InkMuted,
                        modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = 180f },
                    )
                },
            )
            RippleMenuItem(
                if (current == Destination.Hidden) "Unhide" else "Hide",
                { onDismiss(); vm.toggleLessUsed(app) },
            )
            RippleMenuItem("App info", { onDismiss(); vm.openAppInfo(app) })
            RippleMenuItem("Uninstall", { onDismiss(); vm.uninstall(app) }, destructive = true)
        } else {
            RippleMenuItem("Back", { page = 0 }, icon = R.drawable.ic_back)
            MenuLabel("Move ${app.label} to")
            val options = buildList<Pair<String, Destination>> {
                add("Favorites" to Destination.Favorites)
                sections.forEach { add(it.name to Destination.InSection(it.id)) }
                add("A–Z list" to Destination.List)
                add("Hidden" to Destination.Hidden)
            }
            options.forEach { (label, dest) ->
                RippleMenuItem(
                    label,
                    { onDismiss(); vm.moveApp(app, dest) },
                    trailing = {
                        if (dest == current) Icon(painterResource(R.drawable.ic_check), contentDescription = "Current", tint = Accent, modifier = Modifier.size(20.dp))
                    },
                )
            }
        }
    }
}

// --- Dialogs ------------------------------------------------------------------------

@Composable
private fun DialogCard(title: String, content: @Composable () -> Unit, actions: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(24.dp))
            .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 8.dp),
    ) {
        Text(title, style = Type.BodyStrong.copy(fontSize = 18.sp, shadow = null), color = Ui.Ink, modifier = Modifier.padding(end = 8.dp))
        Spacer(Modifier.height(14.dp))
        Box(Modifier.padding(end = 8.dp)) { content() }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { actions() }
    }
}

@Composable
private fun DialogButton(label: String, color: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        label,
        style = Type.BodyStrong.copy(shadow = null),
        color = if (enabled) color else color.copy(alpha = 0.35f),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    )
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Dialog(onDismissRequest = onDismiss) {
        DialogCard(
            title = title,
            content = {
                val shape = RoundedCornerShape(14.dp)
                BasicTextField(
                    value = name,
                    onValueChange = { name = it.take(30) },
                    singleLine = true,
                    textStyle = Type.Body.copy(color = Ui.Ink, shadow = null),
                    cursorBrush = SolidColor(Accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onConfirm(name) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.08f), shape)
                        .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                        .focusRequester(focusRequester),
                    decorationBox = { field ->
                        Box {
                            if (name.isEmpty()) Text("Section name", style = Type.Body.copy(shadow = null), color = Ui.InkFaint)
                            field()
                        }
                    },
                )
            },
            actions = {
                DialogButton("Cancel", Ui.InkMuted, onClick = onDismiss)
                DialogButton(confirmLabel, Accent, enabled = name.isNotBlank()) { onConfirm(name) }
            },
        )
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        DialogCard(
            title = title,
            content = { Text(message, style = Type.Caption.copy(shadow = null), color = Ui.InkMuted) },
            actions = {
                DialogButton("Cancel", Ui.InkMuted, onClick = onDismiss)
                DialogButton(confirmLabel, Ui.Danger, onClick = onConfirm)
            },
        )
    }
}
