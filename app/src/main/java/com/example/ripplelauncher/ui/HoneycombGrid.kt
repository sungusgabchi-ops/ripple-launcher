package com.example.ripplelauncher.ui

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ripplelauncher.AppEntry
import com.example.ripplelauncher.LauncherViewModel
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/*
 * Apple Watch–style honeycomb: rows of 5 and 4 icons, offset by half a cell.
 * Icons are full size in the middle of the screen and shrink toward the edges,
 * so scrolling feels like rolling a ball of apps under your thumb.
 */

private const val WIDE_ROW = 5
private const val NARROW_ROW = 4
private const val PER_PAIR = WIDE_ROW + NARROW_ROW

private fun rowOf(i: Int) = (i / PER_PAIR) * 2 + if (i % PER_PAIR >= WIDE_ROW) 1 else 0

private fun smooth(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/** dx, dy: distance from screen center, -1..1. Returns the icon scale. */
private fun scaleFor(dx: Float, dy: Float): Float {
    val vertical = 1f - 0.75f * smooth(0.35f, 1.0f, abs(dy))
    val horizontal = 1f - 0.3f * smooth(0.5f, 1.0f, abs(dx))
    return (vertical * horizontal).coerceIn(0.15f, 1f)
}

/** Where everything goes, in content coordinates. */
private class GridLayout(
    val positions: List<Offset>,          // top-left of each icon's cell
    val dividers: List<Pair<Char, Float>>, // letter + top of its divider
    val contentHeight: Float,
)

@Composable
fun HoneycombGrid(
    apps: List<AppEntry>,
    favoriteKeys: Set<String>,
    vm: LauncherViewModel,
    jumpLetter: Char?,
    jumpId: Long,
    resetKey: String,
    grouped: Boolean,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val widthPx = constraints.maxWidth.toFloat()
        val viewportH = constraints.maxHeight.toFloat()
        val cell = widthPx / WIDE_ROW
        val iconPx = cell * 0.66f          // smaller icon in the same cell = more breathing room
        val rowH = cell * 0.94f
        val dividerH = with(density) { 40.dp.toPx() }
        val edgePad = viewportH / 2f - cell / 2f // lets the first and last rows reach the center
        val iconDp = with(density) { iconPx.toDp() }
        val inset = (cell - iconPx) / 2f

        val layout = remember(apps, grouped, widthPx, viewportH) {
            val positions = ArrayList<Offset>(apps.size)
            val dividers = mutableListOf<Pair<Char, Float>>()
            var groupTop = edgePad
            var lastRowTop = edgePad
            var k = 0 // index within the current letter group
            apps.forEachIndexed { i, app ->
                val newGroup = grouped && (i == 0 || app.letter != apps[i - 1].letter)
                if (newGroup) {
                    val dividerTop = if (i == 0) edgePad else lastRowTop + cell + rowH * 0.2f
                    dividers += app.letter to dividerTop
                    groupTop = dividerTop + dividerH
                    k = 0
                }
                val r = k % PER_PAIR
                val x = if (r < WIDE_ROW) r * cell else cell / 2f + (r - WIDE_ROW) * cell
                lastRowTop = groupTop + rowOf(k) * rowH
                positions += Offset(x, lastRowTop)
                k++
            }
            val height = if (apps.isEmpty()) 0f else lastRowTop + cell + edgePad
            GridLayout(positions, dividers, height)
        }
        val positions = layout.positions

        val scroll = rememberScrollState()

        // Alphabet scroller: glide that letter's divider to just above the center.
        LaunchedEffect(jumpId) {
            val letter = jumpLetter ?: return@LaunchedEffect
            val i = apps.indexOfFirst { it.letter == letter }
            if (i >= 0) {
                val target = (positions[i].y + cell / 2f - viewportH / 2f).roundToInt().coerceAtLeast(0)
                scroll.animateScrollTo(target, tween(220))
            }
        }
        // New search text: start from the top.
        LaunchedEffect(resetKey) {
            if (resetKey.isNotEmpty()) scroll.scrollTo(0)
        }

        // The app closest to the middle of the screen, for the name label.
        val centerIndex by remember(layout) {
            derivedStateOf {
                val cy = scroll.value + viewportH / 2f
                val cx = widthPx / 2f
                var best = -1
                var bestDist = Float.MAX_VALUE
                positions.forEachIndexed { i, p ->
                    val d = abs(p.y + cell / 2f - cy) + 0.5f * abs(p.x + cell / 2f - cx)
                    if (d < bestDist) { bestDist = d; best = i }
                }
                best
            }
        }

        Box(Modifier.fillMaxSize().verticalScroll(scroll)) {
            Box(Modifier.fillMaxWidth().height(with(density) { layout.contentHeight.toDp() })) {

                // Letter dividers: a label and a thin line, fading out near the edges.
                layout.dividers.forEach { (letter, top) ->
                    key("divider_$letter") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .offset { IntOffset(0, top.roundToInt()) }
                                .fillMaxWidth()
                                .height(with(density) { dividerH.toDp() })
                                .padding(horizontal = 12.dp)
                                .graphicsLayer {
                                    val cy = top + dividerH / 2f - scroll.value
                                    val dy = (cy - viewportH / 2f) / (viewportH / 2f)
                                    alpha = (1f - smooth(0.55f, 1.0f, abs(dy))).coerceIn(0f, 1f)
                                },
                        ) {
                            Text(
                                letter.toString(),
                                color = Accent,
                                style = MaterialTheme.typography.titleSmall.copy(shadow = TextShadow),
                            )
                            Spacer(Modifier.width(12.dp))
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.25f)),
                            )
                        }
                    }
                }

                apps.forEachIndexed { i, app ->
                    key(app.key) {
                        val p = positions[i]
                        HoneyIcon(
                            app = app,
                            isFavorite = app.key in favoriteKeys,
                            vm = vm,
                            modifier = Modifier
                                .offset { IntOffset((p.x + inset).roundToInt(), (p.y + inset).roundToInt()) }
                                .size(iconDp)
                                .graphicsLayer {
                                    // Runs every frame while scrolling, without recomposing.
                                    val cy = p.y + cell / 2f - scroll.value
                                    val cx = p.x + cell / 2f
                                    val dy = (cy - viewportH / 2f) / (viewportH / 2f)
                                    val dx = (cx - widthPx / 2f) / (widthPx / 2f)
                                    val s = scaleFor(dx, dy)
                                    scaleX = s
                                    scaleY = s
                                    // Shrinking icons drift toward the center to close the gaps.
                                    translationY = -sign(dy) * (1f - s) * cell * 0.35f
                                    translationX = -sign(dx) * (1f - s) * cell * 0.25f
                                    alpha = (s * 1.6f - 0.25f).coerceIn(0f, 1f)
                                },
                        )
                    }
                }
            }
        }

        if (centerIndex in apps.indices) {
            Text(
                apps[centerIndex].label,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium.copy(shadow = TextShadow),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp, start = 24.dp, end = 24.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HoneyIcon(app: AppEntry, isFavorite: Boolean, vm: LauncherViewModel, modifier: Modifier) {
    var menuOpen by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    Box(modifier) {
        Image(
            app.icon,
            contentDescription = app.label,
            modifier = Modifier
                .fillMaxSize()
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
        AppMenu(app, isFavorite, vm, expanded = menuOpen, onDismiss = { menuOpen = false })
    }
}
