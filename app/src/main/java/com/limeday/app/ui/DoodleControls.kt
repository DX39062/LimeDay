package com.limeday.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

object DoodleTokens {
    val ControlCorner = 12.dp
    val CardCorner = 16.dp
    val Hairline = 1.6.dp
    val TouchTarget = 48.dp
}

@Composable
fun DoodleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val track by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        tween(160),
        label = "doodle switch track"
    )
    val ink by animateColorAsState(
        if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = .38f),
        tween(160),
        label = "doodle switch ink"
    )
    val knob by animateDpAsState(if (checked) 35.dp else 17.dp, tween(170), label = "doodle switch knob")
    val paper = MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .size(52.dp, DoodleTokens.TouchTarget)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch) { onCheckedChange(it) },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(46.dp, 28.dp)) {
            val stroke = DoodleTokens.Hairline.toPx()
            drawRoundRect(
                color = track,
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(size.height / 2f),
            )
            drawRoundRect(
                color = ink,
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(size.height / 2f),
                style = Stroke(stroke)
            )
            drawCircle(paper, 9.dp.toPx(), Offset(knob.toPx(), size.height / 2f))
            drawCircle(ink, 9.dp.toPx(), Offset(knob.toPx(), size.height / 2f), style = Stroke(stroke))
            if (checked) {
                drawLine(
                    ink,
                    Offset(knob.toPx() - 4.dp.toPx(), size.height / 2f),
                    Offset(knob.toPx() - 1.dp.toPx(), size.height / 2f + 3.dp.toPx()),
                    stroke
                )
                drawLine(
                    ink,
                    Offset(knob.toPx() - 1.dp.toPx(), size.height / 2f + 3.dp.toPx()),
                    Offset(knob.toPx() + 5.dp.toPx(), size.height / 2f - 4.dp.toPx()),
                    stroke
                )
            }
        }
    }
}
