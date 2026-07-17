package com.limeday.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.LocalContentColor

@Composable
fun TodoAddIcon(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val color = LocalContentColor.current
    Canvas(modifier.size(size)) {
        val stroke = this.size.minDimension * .09f
        drawLine(color, Offset(center.x, this.size.height * .25f), Offset(center.x, this.size.height * .75f), stroke, StrokeCap.Round)
        drawLine(color, Offset(this.size.width * .25f, center.y), Offset(this.size.width * .75f, center.y), stroke, StrokeCap.Round)
    }
}

@Composable
fun TodoCheckIcon(checked: Boolean, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val color = LocalContentColor.current
    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(190),
        label = "todo check"
    )
    Canvas(modifier.size(size)) {
        val stroke = this.size.minDimension * .085f
        drawCircle(color, radius = this.size.minDimension * .42f, style = Stroke(stroke))
        if (progress > 0f) {
            val firstStart = Offset(this.size.width * .28f, this.size.height * .51f)
            val corner = Offset(this.size.width * .44f, this.size.height * .67f)
            val end = Offset(this.size.width * .73f, this.size.height * .36f)
            val firstProgress = (progress * 2f).coerceAtMost(1f)
            val secondProgress = ((progress - .5f) * 2f).coerceIn(0f, 1f)
            drawLine(color, firstStart, lerp(firstStart, corner, firstProgress), stroke, StrokeCap.Round)
            if (secondProgress > 0f) drawLine(color, corner, lerp(corner, end, secondProgress), stroke, StrokeCap.Round)
        }
    }
}

@Composable
fun TodoMoreIcon(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val color = LocalContentColor.current
    Canvas(modifier.size(size)) {
        val radius = this.size.minDimension * .075f
        listOf(.28f, .5f, .72f).forEach { x -> drawCircle(color, radius, Offset(this.size.width * x, center.y)) }
    }
}

@Composable
fun TodoTrashIcon(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val color = LocalContentColor.current
    Canvas(modifier.size(size)) {
        val stroke = this.size.minDimension * .075f
        drawRoundRect(
            color = color,
            topLeft = Offset(this.size.width * .29f, this.size.height * .34f),
            size = androidx.compose.ui.geometry.Size(this.size.width * .42f, this.size.height * .46f),
            cornerRadius = CornerRadius(this.size.width * .06f),
            style = Stroke(stroke)
        )
        drawLine(color, Offset(this.size.width * .23f, this.size.height * .27f), Offset(this.size.width * .77f, this.size.height * .27f), stroke, StrokeCap.Round)
        drawLine(color, Offset(this.size.width * .41f, this.size.height * .19f), Offset(this.size.width * .59f, this.size.height * .19f), stroke, StrokeCap.Round)
        drawLine(color, Offset(this.size.width * .43f, this.size.height * .43f), Offset(this.size.width * .43f, this.size.height * .68f), stroke * .72f, StrokeCap.Round)
        drawLine(color, Offset(this.size.width * .57f, this.size.height * .43f), Offset(this.size.width * .57f, this.size.height * .68f), stroke * .72f, StrokeCap.Round)
    }
}

@Composable
fun TodoCopyIcon(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val color = LocalContentColor.current
    Canvas(modifier.size(size)) {
        val stroke = this.size.minDimension * .075f
        val style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val width = this.size.width
        val height = this.size.height
        drawRoundRect(color, Offset(this.size.width * .31f, this.size.height * .30f), androidx.compose.ui.geometry.Size(this.size.width * .46f, this.size.height * .48f), CornerRadius(this.size.width * .06f), style)
        val path = Path().apply {
            moveTo(width * .60f, height * .21f)
            lineTo(width * .28f, height * .21f)
            quadraticTo(width * .21f, height * .21f, width * .21f, height * .29f)
            lineTo(width * .21f, height * .61f)
        }
        drawPath(path, color, style = style)
    }
}

@Composable
fun TodoCalendarIcon(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val color = LocalContentColor.current
    Canvas(modifier.size(size)) {
        val stroke = this.size.minDimension * .075f
        val style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(color, Offset(this.size.width * .19f, this.size.height * .24f), androidx.compose.ui.geometry.Size(this.size.width * .62f, this.size.height * .58f), CornerRadius(this.size.width * .08f), style)
        drawLine(color, Offset(this.size.width * .20f, this.size.height * .40f), Offset(this.size.width * .80f, this.size.height * .40f), stroke, StrokeCap.Round)
        drawLine(color, Offset(this.size.width * .34f, this.size.height * .17f), Offset(this.size.width * .34f, this.size.height * .31f), stroke, StrokeCap.Round)
        drawLine(color, Offset(this.size.width * .66f, this.size.height * .17f), Offset(this.size.width * .66f, this.size.height * .31f), stroke, StrokeCap.Round)
        drawCircle(color, this.size.minDimension * .045f, Offset(this.size.width * .37f, this.size.height * .58f))
        drawCircle(color, this.size.minDimension * .045f, Offset(this.size.width * .58f, this.size.height * .58f))
    }
}

@Composable
fun TodoPriorityIcon(modifier: Modifier = Modifier, size: Dp = 24.dp, filled: Boolean = false) {
    val color = LocalContentColor.current
    Canvas(modifier.size(size)) {
        val stroke = this.size.minDimension * .075f
        val width = this.size.width
        val height = this.size.height
        drawLine(color, Offset(this.size.width * .27f, this.size.height * .16f), Offset(this.size.width * .27f, this.size.height * .84f), stroke, StrokeCap.Round)
        val flag = Path().apply {
            moveTo(width * .31f, height * .22f)
            lineTo(width * .73f, height * .22f)
            lineTo(width * .63f, height * .40f)
            lineTo(width * .73f, height * .57f)
            lineTo(width * .31f, height * .57f)
            close()
        }
        drawPath(flag, color, style = if (filled) androidx.compose.ui.graphics.drawscope.Fill else Stroke(stroke, join = StrokeJoin.Round))
    }
}

@Composable
fun TodoRestoreIcon(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val color = LocalContentColor.current
    Canvas(modifier.size(size)) {
        val stroke = this.size.minDimension * .075f
        val width = this.size.width
        val height = this.size.height
        val arcRect = Rect(this.size.width * .23f, this.size.height * .23f, this.size.width * .80f, this.size.height * .80f)
        clipRect(0f, 0f, this.size.width, this.size.height) {
            drawArc(color, 205f, 285f, false, arcRect.topLeft, arcRect.size, 1f, Stroke(stroke, cap = StrokeCap.Round))
        }
        val path = Path().apply {
            moveTo(width * .18f, height * .23f)
            lineTo(width * .40f, height * .24f)
            lineTo(width * .25f, height * .41f)
        }
        drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

private fun lerp(start: Offset, end: Offset, fraction: Float): Offset = Offset(
    x = start.x + (end.x - start.x) * fraction,
    y = start.y + (end.y - start.y) * fraction
)
