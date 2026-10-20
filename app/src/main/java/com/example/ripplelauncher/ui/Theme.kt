package com.example.ripplelauncher.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ripplelauncher.R

// ---------------------------------------------------------------------------
// Design tokens. Everything on screen pulls from here so it stays consistent.
// ---------------------------------------------------------------------------

val Accent = Color(0xFF9BD4FF)

/** Soft shadow so white text stays readable on any wallpaper. */
val TextShadow = Shadow(color = Color.Black.copy(alpha = 0.5f), offset = Offset(0f, 1.5f), blurRadius = 6f)

val Inter = FontFamily(
    Font(R.font.inter_light, FontWeight.Light),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

object Ui {
    /** The one left edge everything lines up to. */
    val Gutter = 20.dp

    val Ink = Color.White
    val InkMuted = Color.White.copy(alpha = 0.68f)
    val InkFaint = Color.White.copy(alpha = 0.42f)
    val Danger = Color(0xFFFF8A80)
}

/** Three sizes, clear hierarchy: display (clock), body (apps), caption (details). */
object Type {
    val Display = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Light, fontSize = 36.sp,
        letterSpacing = (-0.8).sp, shadow = TextShadow,
    )
    val DisplaySuffix = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 15.sp,
        letterSpacing = 0.2.sp, shadow = TextShadow,
    )
    val Body = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 16.sp,
        letterSpacing = (-0.1).sp, shadow = TextShadow,
    )
    val BodyStrong = Body.copy(fontWeight = FontWeight.Medium)
    val Caption = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp,
        letterSpacing = 0.sp, shadow = TextShadow,
    )
    val Label = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
        letterSpacing = 1.2.sp, shadow = TextShadow,
    )
}

// ---------------------------------------------------------------------------
// Surfaces
// ---------------------------------------------------------------------------

/** Raised, frosted control: buttons, search box, brightness. */
fun Modifier.glass(shape: Shape): Modifier = this
    .shadow(10.dp, shape, ambientColor = Color.Black.copy(alpha = 0.45f), spotColor = Color.Black.copy(alpha = 0.45f))
    .background(Brush.verticalGradient(listOf(Color(0xF2393E47), Color(0xF2202329))), shape)
    .border(
        1.dp,
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.04f))),
        shape,
    )

/** Flat, quiet panel that groups content (the favorites). */
fun Modifier.panel(shape: Shape): Modifier = this
    .background(Color.White.copy(alpha = 0.07f), shape)
    .border(1.dp, Color.White.copy(alpha = 0.08f), shape)

/** Springy press-down feedback for anything tappable. */
@Composable
fun Modifier.pressScale(source: InteractionSource, pressed: Float = 0.92f): Modifier {
    val isPressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(
        targetValue = if (isPressed) pressed else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    return this.graphicsLayer { scaleX = s; scaleY = s }
}

// ---------------------------------------------------------------------------
// Menus styled to match, instead of stock white Android pop-ups
// ---------------------------------------------------------------------------

@Composable
fun RippleMenu(expanded: Boolean, onDismissRequest: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(18.dp),
        containerColor = Color(0xF71E2126),
        tonalElevation = 0.dp,
        shadowElevation = 16.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        content = content,
    )
}

@Composable
fun RippleMenuItem(
    text: String,
    onClick: () -> Unit,
    icon: Int? = null,
    destructive: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val color = if (destructive) Ui.Danger else Ui.Ink
    DropdownMenuItem(
        text = { Text(text, style = Type.Body.copy(fontSize = 15.sp, shadow = null), color = color) },
        onClick = onClick,
        leadingIcon = icon?.let {
            { Icon(painterResource(it), contentDescription = null, tint = if (destructive) color else Ui.InkMuted, modifier = Modifier.size(20.dp)) }
        },
        trailingIcon = trailing,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
    )
}

@Composable
fun MenuLabel(text: String) {
    Text(
        text.uppercase(),
        style = Type.Label.copy(shadow = null),
        color = Accent,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 6.dp),
    )
}

@Composable
fun RippleTheme(content: @Composable () -> Unit) {
    val base = Typography()
    fun TextStyle.inter() = copy(fontFamily = Inter)
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            background = Color.Transparent,
        ),
        typography = Typography(
            displayLarge = base.displayLarge.inter(), displayMedium = base.displayMedium.inter(), displaySmall = base.displaySmall.inter(),
            headlineLarge = base.headlineLarge.inter(), headlineMedium = base.headlineMedium.inter(), headlineSmall = base.headlineSmall.inter(),
            titleLarge = base.titleLarge.inter(), titleMedium = base.titleMedium.inter(), titleSmall = base.titleSmall.inter(),
            bodyLarge = base.bodyLarge.inter(), bodyMedium = base.bodyMedium.inter(), bodySmall = base.bodySmall.inter(),
            labelLarge = base.labelLarge.inter(), labelMedium = base.labelMedium.inter(), labelSmall = base.labelSmall.inter(),
        ),
        content = content,
    )
}
