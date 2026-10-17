package com.example.ripplelauncher.ui

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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

@Composable
fun HoneycombGrid(
    apps: List<AppEntry>,
    favoriteKeys: Set<String>,
    vm: LauncherViewModel,
    jumpLetter: Char?,
    jumpId: Long,
    resetKey: String,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val widthPx = constraints.maxWidth.toFloat()
        val viewportH = constraints.maxHeight.toFloat()
        val cell = widthPx / WIDE_ROW
        val iconPx = cell * 0.8f
        val rowH = cell * 0.86f
        val topPad = viewportH / 2f - cell / 2f
        val rows = if (apps.isEmpty()) 0 else rowOf(apps.lastIndex) + 1
        val contentH = topPad * 2f + (rows - 1).coerceAtLeast(0) * rowH + cell
        val iconDp = with(density) { iconPx.toDp() }
        val inset = (cell - iconPx) / 2f

        // Top-left corner of every cell, in content coordinates.
        val positions = remember(apps.size, widthPx, viewportH) {
            List(apps.size) { i ->
                val r = i % PER_PAIR
                val row = rowOf(i)
                val x = if (r < WIDE_ROW) r * cell else cell / 2f + (r - WIDE_ROW) * cell
                Offset(x, topPad + row * rowH)
            }
        }

        val scroll = rememberScrollState()

        // Alphabet scroller: glide the first app of that letter to the center.
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
        val centerIndex by remember(apps.size, positions) {
            derivedStateOf {
                if (apps.isEmpty()) -1 else {
                    val centerY = scroll.value + viewportH / 2f
                    val row = ((centerY - topPad - cell / 2f) / rowH).roundToInt().coerceIn(0, rows - 1)
                    val i = (row / 2) * PER_PAIR + if (row % 2 == 0) 2 else WIDE_ROW + 1
                    i.coerceAtMost(apps.lastIndex)
                }
            }
        }

        Box(Modifier.fillMaxSize().verticalScroll(scroll)) {
            Box(Modifier.fillMaxWidth().height(with(density) { contentH.toDp() })) {
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
                                    translationY = -sign(dy) * (1f - s) * cell * 0.45f
                                    translationX = -sign(dx) * (1f - s) * cell * 0.3f
                                    alpha = (s * 1.6f - 0.25f).coerceIn(0f, 1f)
                                },
                        )
                    }
                }
            }
        }

        if (centerIndex >= 0) {
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
