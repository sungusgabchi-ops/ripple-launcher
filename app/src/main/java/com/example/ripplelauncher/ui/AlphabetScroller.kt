package com.example.ripplelauncher.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.exp

/** How many letters on each side of your finger bulge out. Bigger = wider wave. */
private const val WAVE_WIDTH = 2.2f

/**
 * The signature piece: a column of letters on the screen edge.
 * Drag along it and the letters near your thumb ripple outward in a wave.
 */
@Composable
fun AlphabetScroller(
    letters: List<Char>,
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onSelect by rememberUpdatedState(onLetterSelected)
    val haptic = LocalHapticFeedback.current
    val maxShiftPx = with(LocalDensity.current) { 56.dp.toPx() }

    var heightPx by remember { mutableIntStateOf(1) }
    var touching by remember { mutableStateOf(false) }
    var touchY by remember { mutableFloatStateOf(0f) }
    var current by remember { mutableStateOf<Char?>(null) }

    // 0 = flat, 1 = full wave. Springs in and out for a bouncy feel.
    val strength by animateFloatAsState(
        targetValue = if (touching) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
        label = "wave",
    )

    Column(
        modifier
            .onSizeChanged { heightPx = it.height.coerceAtLeast(1) }
            .pointerInput(letters) {
                fun update(y: Float) {
                    touchY = y.coerceIn(0f, heightPx.toFloat())
                    val index = (touchY / heightPx * letters.size).toInt().coerceIn(0, letters.lastIndex)
                    val letter = letters[index]
                    if (letter != current) {
                        current = letter
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(letter)
                    }
                }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    touching = true
                    update(down.position.y)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        update(change.position.y)
                    }
                    touching = false
                    current = null
                }
            },
    ) {
        letters.forEachIndexed { i, letter ->
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = letter.toString(),
                    color = if (letter == current) Accent else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.graphicsLayer {
                        // Distance from finger, measured in letters.
                        val slot = heightPx.toFloat() / letters.size
                        val d = ((i + 0.5f) * slot - touchY) / slot
                        // Gaussian bell curve: biggest at the finger, fading smoothly.
                        val w = exp(-(d * d) / (2f * WAVE_WIDTH * WAVE_WIDTH)) * strength
                        translationX = -w * maxShiftPx
                        scaleX = 1f + w * 0.9f
                        scaleY = scaleX
                        alpha = 0.6f + 0.4f * w
                    },
                )
            }
        }
    }
}
