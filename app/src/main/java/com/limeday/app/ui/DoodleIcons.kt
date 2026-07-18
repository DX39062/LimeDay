package com.limeday.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class DoodleIconType {
    Todo,
    Summary,
    Settings,
    Back,
    Forward,
    Calendar,
    Review,
    ChevronRight,
    Close,
    Lock,
    Check,
    Refresh,
    Trash,
    Info,
    Reminder,
    Appearance,
    Data,
    WebDav,
    Edit,
    Expand,
    Collapse,
    Export,
    Import,
    Search,
    Group,
    Clock,
    Repeat,
    Steps,
    Erase
}

@Composable
fun DoodleIcon(
    type: DoodleIconType,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    accent: Color = Color.Unspecified
) {
    val actualTint = if (tint == Color.Unspecified) Color(0xFF245F55) else tint
    val actualAccent = if (accent == Color.Unspecified) Color(0xFFF4B860) else accent
    val semanticsModifier = if (contentDescription == null) modifier else modifier.semantics {
        this.contentDescription = contentDescription
    }
    Canvas(semanticsModifier) {
        drawDoodleIcon(type, actualTint, actualAccent)
    }
}

@Composable
fun NavigationDoodleIcon(type: DoodleIconType, selected: Boolean, contentDescription: String) {
    DoodleIcon(
        type = type,
        contentDescription = contentDescription,
        modifier = Modifier.size(26.dp),
        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        accent = if (selected) MaterialTheme.colorScheme.tertiaryContainer else Color.Transparent
    )
}

private fun DrawScope.drawDoodleIcon(type: DoodleIconType, tint: Color, accent: Color) {
    val unit = size.minDimension / 24f
    val stroke = Stroke(width = 2.05f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val fine = Stroke(width = 1.55f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun p(x: Float, y: Float) = Offset(x * unit, y * unit)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, style: Stroke = stroke) =
        drawLine(tint, p(x1, y1), p(x2, y2), style.width, style.cap)
    fun path(points: List<Pair<Float, Float>>, closed: Boolean = false, style: Stroke = stroke) {
        val value = Path().apply {
            points.forEachIndexed { index, point ->
                if (index == 0) moveTo(point.first * unit, point.second * unit)
                else lineTo(point.first * unit, point.second * unit)
            }
            if (closed) close()
        }
        drawPath(value, tint, style = style)
    }

    when (type) {
        DoodleIconType.Todo -> {
            if (accent.alpha > 0f) drawCircle(accent, 8.3f * unit, p(12f, 12f))
            drawRoundRect(tint, p(4.2f, 4.8f), Size(15.5f * unit, 14.4f * unit), CornerRadius(3.2f * unit), style = stroke)
            path(listOf(7.2f to 12.1f, 10.4f to 15.2f, 17.2f to 8.4f))
        }
        DoodleIconType.Summary -> {
            if (accent.alpha > 0f) drawCircle(accent, 5.2f * unit, p(8.3f, 8.2f))
            path(listOf(12.1f to 3.4f, 14.2f to 9.1f, 20.2f to 10.2f, 15.1f to 13.5f, 15.9f to 19.5f, 11.2f to 15.8f, 5.7f to 18.4f, 7.8f to 12.8f, 3.6f to 8.8f, 9.5f to 9.1f), closed = true)
        }
        DoodleIconType.Settings -> {
            if (accent.alpha > 0f) drawCircle(accent, 4.3f * unit, p(16.7f, 7.1f))
            drawCircle(tint, 3.2f * unit, p(12f, 12f), style = stroke)
            drawCircle(tint, 7.2f * unit, p(12f, 12f), style = fine)
            repeat(8) { index ->
                val angle = index * PI.toFloat() / 4f - PI.toFloat() / 2f
                line(
                    12f + cos(angle) * 7.3f,
                    12f + sin(angle) * 7.3f,
                    12f + cos(angle) * 9.4f,
                    12f + sin(angle) * 9.4f,
                    fine
                )
            }
        }
        DoodleIconType.Back -> path(listOf(18.5f to 5.4f, 10.8f to 12f, 18.2f to 18.6f, 10.8f to 12f, 4.2f to 12.4f))
        DoodleIconType.Forward, DoodleIconType.ChevronRight -> path(listOf(5.5f to 5.6f, 13.2f to 12f, 5.8f to 18.5f, 13.2f to 12f, 19.5f to 12.3f))
        DoodleIconType.Calendar -> {
            drawRoundRect(tint, p(3.8f, 5.2f), Size(16.5f * unit, 15f * unit), CornerRadius(2.2f * unit), style = stroke)
            line(4.2f, 9.2f, 19.7f, 9.1f, fine)
            line(8f, 3.5f, 8f, 7f, fine)
            line(16.1f, 3.6f, 16f, 7f, fine)
            drawCircle(tint, 1.15f * unit, p(9f, 13.2f))
            drawCircle(tint, 1.15f * unit, p(14.8f, 13.2f))
            drawCircle(tint, 1.15f * unit, p(9f, 17f))
        }
        DoodleIconType.Review -> {
            path(listOf(5f to 4.2f, 18.2f to 4.8f, 18.7f to 18.8f, 5.3f to 19.5f, 5f to 4.2f), closed = true)
            line(8f, 8.5f, 15.7f, 8.3f, fine)
            line(8f, 12.2f, 14f, 12.1f, fine)
            path(listOf(13f to 17.4f, 18.8f to 11.6f, 20.4f to 13.2f, 14.7f to 19f, 12.6f to 19.4f, 13f to 17.4f), closed = true, style = fine)
        }
        DoodleIconType.Close -> {
            line(5.5f, 5.8f, 18.7f, 18.4f)
            line(18.3f, 5.4f, 5.8f, 18.8f)
        }
        DoodleIconType.Lock -> {
            drawRoundRect(tint, p(5.3f, 10.4f), Size(13.5f * unit, 10f * unit), CornerRadius(2f * unit), style = stroke)
            val lockPath = Path().apply {
                moveTo(8.2f * unit, 10.5f * unit)
                cubicTo(8.1f * unit, 3.6f * unit, 15.9f * unit, 3.5f * unit, 15.8f * unit, 10.5f * unit)
            }
            drawPath(lockPath, tint, style = stroke)
            drawCircle(tint, 1.1f * unit, p(12.1f, 15.2f))
        }
        DoodleIconType.Check -> path(listOf(4.8f to 12.3f, 9.8f to 17.1f, 19.5f to 6.9f))
        DoodleIconType.Refresh -> {
            val refresh = Path().apply {
                moveTo(18.4f * unit, 8.4f * unit)
                cubicTo(14.9f * unit, 3.8f * unit, 7.2f * unit, 5f * unit, 5.4f * unit, 11.2f * unit)
                cubicTo(3.8f * unit, 16.9f * unit, 10.5f * unit, 21f * unit, 16f * unit, 17.6f * unit)
            }
            drawPath(refresh, tint, style = stroke)
            path(listOf(14.6f to 5.8f, 19.2f to 8.1f, 18.2f to 3.1f), style = fine)
        }
        DoodleIconType.Trash -> {
            path(listOf(6.2f to 7.4f, 7.3f to 20f, 17.1f to 19.6f, 18f to 7.2f), style = stroke)
            line(4.6f, 6.8f, 19.4f, 6.5f)
            line(9f, 3.8f, 15.1f, 3.8f, fine)
            line(10f, 10.2f, 10.3f, 16.8f, fine)
            line(14.4f, 10f, 14.1f, 16.6f, fine)
        }
        DoodleIconType.Info -> {
            drawCircle(tint, 8.7f * unit, p(12f, 12f), style = stroke)
            drawCircle(tint, 1.1f * unit, p(12.1f, 7.8f))
            line(12f, 11.4f, 12f, 17f, fine)
        }
        DoodleIconType.Reminder -> {
            val bell = Path().apply {
                moveTo(5.1f * unit, 16.8f * unit)
                cubicTo(7.2f * unit, 14.9f * unit, 6f * unit, 10.2f * unit, 8.3f * unit, 7.6f * unit)
                cubicTo(10.3f * unit, 5.3f * unit, 14.3f * unit, 5.5f * unit, 16.1f * unit, 7.7f * unit)
                cubicTo(18.3f * unit, 10.3f * unit, 17f * unit, 15f * unit, 19.1f * unit, 16.6f * unit)
                close()
            }
            drawPath(bell, tint, style = stroke)
            path(listOf(9.8f to 19f, 12f to 20.3f, 14.4f to 18.8f), style = fine)
        }
        DoodleIconType.Appearance -> {
            drawCircle(tint, 4.2f * unit, p(12f, 12f), style = stroke)
            repeat(8) { index ->
                val angle = index * PI.toFloat() / 4f
                line(12f + cos(angle) * 6.7f, 12f + sin(angle) * 6.7f, 12f + cos(angle) * 9.2f, 12f + sin(angle) * 9.2f, fine)
            }
        }
        DoodleIconType.Data -> {
            drawRoundRect(tint, p(4.2f, 4.5f), Size(15.8f * unit, 15.2f * unit), CornerRadius(2f * unit), style = stroke)
            path(listOf(8.2f to 9.2f, 11.7f to 6f, 15.1f to 9.2f), style = fine)
            line(11.7f, 6.4f, 11.7f, 13.4f, fine)
            line(8f, 16.2f, 16f, 16.1f, fine)
        }
        DoodleIconType.WebDav -> {
            val cloud = Path().apply {
                moveTo(6.8f * unit, 18.1f * unit)
                cubicTo(2.6f * unit, 17.6f * unit, 3.1f * unit, 11.3f * unit, 7.1f * unit, 10.7f * unit)
                cubicTo(8.4f * unit, 4.7f * unit, 16.8f * unit, 5.3f * unit, 17.4f * unit, 11f * unit)
                cubicTo(22f * unit, 11.8f * unit, 21.2f * unit, 18.2f * unit, 17.2f * unit, 18.2f * unit)
                close()
            }
            drawPath(cloud, tint, style = stroke)
            line(9f, 14.2f, 15.3f, 14.1f, fine)
        }
        DoodleIconType.Edit -> {
            path(listOf(5.1f to 17.5f, 15.9f to 6.4f, 19.2f to 9.5f, 8.4f to 20.3f, 4.4f to 20.5f, 5.1f to 17.5f), closed = true)
            line(14.7f, 7.9f, 17.8f, 11f, fine)
        }
        DoodleIconType.Expand -> path(listOf(4.8f to 8.2f, 12f to 15.7f, 19.3f to 8.4f))
        DoodleIconType.Collapse -> path(listOf(4.8f to 15.8f, 12f to 8.3f, 19.3f to 15.6f))
        DoodleIconType.Export -> {
            path(listOf(5f to 10.7f, 5.4f to 19.2f, 18.8f to 19.1f, 19f to 10.8f), style = fine)
            line(12f, 15.4f, 12f, 3.8f)
            path(listOf(7.8f to 7.6f, 12f to 3.8f, 16.2f to 7.6f), style = fine)
        }
        DoodleIconType.Import -> {
            path(listOf(5f to 10.7f, 5.4f to 19.2f, 18.8f to 19.1f, 19f to 10.8f), style = fine)
            line(12f, 3.8f, 12f, 15.4f)
            path(listOf(7.8f to 11.6f, 12f to 15.4f, 16.2f to 11.6f), style = fine)
        }
        DoodleIconType.Search -> {
            drawCircle(tint, 6.2f * unit, p(10.2f, 10.2f), style = stroke)
            line(14.7f, 14.7f, 20f, 20f)
        }
        DoodleIconType.Group -> {
            path(listOf(4.2f to 7.2f, 10f to 7.3f, 11.6f to 9.1f, 20f to 9.1f, 19.7f to 19.2f, 4.5f to 19.5f, 4.2f to 7.2f), closed = true)
            line(7.2f, 12.2f, 16.8f, 12.1f, fine)
        }
        DoodleIconType.Clock -> {
            drawCircle(tint, 8.4f * unit, p(12f, 12f), style = stroke)
            line(12f, 7f, 12f, 12.2f, fine)
            line(12f, 12.2f, 16.2f, 14.4f, fine)
        }
        DoodleIconType.Repeat -> {
            path(listOf(5f to 9f, 7.4f to 6.5f, 17.2f to 6.5f, 19.2f to 8.8f), style = fine)
            path(listOf(16.6f to 4.2f, 19.2f to 8.8f, 14.5f to 9.2f), style = fine)
            path(listOf(19f to 15f, 16.6f to 17.5f, 6.8f to 17.5f, 4.8f to 15.2f), style = fine)
            path(listOf(7.4f to 19.8f, 4.8f to 15.2f, 9.5f to 14.8f), style = fine)
        }
        DoodleIconType.Steps -> {
            path(listOf(4.2f to 7.2f, 6.1f to 9.1f, 9.1f to 5.8f), style = fine)
            line(11.5f, 7.3f, 20f, 7.1f, fine)
            path(listOf(4.2f to 13f, 6.1f to 14.9f, 9.1f to 11.6f), style = fine)
            line(11.5f, 13.1f, 20f, 12.9f, fine)
            path(listOf(4.2f to 18.6f, 6.1f to 20.5f, 9.1f to 17.2f), style = fine)
            line(11.5f, 18.7f, 20f, 18.5f, fine)
        }
        DoodleIconType.Erase -> {
            path(listOf(5f to 14.4f, 12.7f to 5.2f, 19f to 10.5f, 11.6f to 19.2f, 7.8f to 19.2f, 5f to 14.4f), closed = true)
            line(8.8f, 10f, 14.7f, 15f, fine)
            line(11.7f, 19.2f, 20f, 19.1f, fine)
        }
    }
}

@Composable
fun LimeHeaderDoodle(modifier: Modifier = Modifier) {
    val green = MaterialTheme.colorScheme.primary
    val yellow = MaterialTheme.colorScheme.tertiaryContainer
    Canvas(modifier) {
        val u = size.minDimension / 32f
        val stroke = Stroke(2f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawArc(yellow, 198f, 144f, true, topLeft = Offset(5f * u, 7f * u), size = Size(18f * u, 18f * u))
        drawArc(green, 198f, 144f, false, topLeft = Offset(5f * u, 7f * u), size = Size(18f * u, 18f * u), style = stroke)
        drawLine(green, Offset(14f * u, 16f * u), Offset(20f * u, 10f * u), stroke.width, StrokeCap.Round)
        drawLine(green, Offset(24f * u, 7f * u), Offset(28f * u, 4f * u), stroke.width, StrokeCap.Round)
        drawLine(green, Offset(26f * u, 13f * u), Offset(31f * u, 12f * u), stroke.width, StrokeCap.Round)
        val wave = Path().apply {
            moveTo(3f * u, 28f * u)
            cubicTo(7f * u, 24f * u, 10f * u, 31f * u, 14f * u, 27f * u)
            cubicTo(18f * u, 23f * u, 21f * u, 30f * u, 26f * u, 26f * u)
        }
        drawPath(wave, green, style = Stroke(1.6f * u, cap = StrokeCap.Round))
    }
}
