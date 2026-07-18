package com.limeday.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.limeday.app.data.TodoGroupIconCatalog

val todoGroupIconLabels = linkedMapOf(
    "daily" to "日常", "work" to "工作", "study" to "学习", "home" to "家庭",
    "health" to "健康", "shopping" to "购物", "finance" to "财务", "travel" to "出行",
    "idea" to "灵感", "project" to "项目", "habit" to "习惯", "reading" to "阅读",
    "sport" to "运动", "chores" to "家务", "social" to "社交", "other" to "其他"
)

@Composable
fun GroupDoodleIcon(
    iconKey: String,
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFF245F55),
    accent: Color = Color(0xFFF4B860),
    isInbox: Boolean = false
) {
    val key = TodoGroupIconCatalog.displayKey(iconKey, isInbox)
    Canvas(modifier) {
        val u = size.minDimension / 24f
        val stroke = Stroke(1.9f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val fine = Stroke(1.35f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun p(x: Float, y: Float) = Offset(x * u, y * u)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float, style: Stroke = stroke) =
            drawLine(tint, p(x1, y1), p(x2, y2), style.width, style.cap)
        fun path(vararg points: Pair<Float, Float>, closed: Boolean = false, style: Stroke = stroke) {
            val shape = Path().apply {
                points.forEachIndexed { index, point ->
                    if (index == 0) moveTo(point.first * u, point.second * u) else lineTo(point.first * u, point.second * u)
                }
                if (closed) close()
            }
            drawPath(shape, tint, style = style)
        }
        fun irregularCircle(cx: Float, cy: Float, radius: Float, style: Stroke = stroke) {
            val shape = Path().apply {
                moveTo((cx + radius) * u, cy * u)
                cubicTo((cx + radius) * u, (cy + radius * .72f) * u, (cx + radius * .55f) * u, (cy + radius) * u, cx * u, (cy + radius * .94f) * u)
                cubicTo((cx - radius * .75f) * u, (cy + radius) * u, (cx - radius) * u, (cy + radius * .35f) * u, (cx - radius * .95f) * u, cy * u)
                cubicTo((cx - radius) * u, (cy - radius * .72f) * u, (cx - radius * .4f) * u, (cy - radius) * u, cx * u, (cy - radius * .96f) * u)
                cubicTo((cx + radius * .7f) * u, (cy - radius) * u, (cx + radius) * u, (cy - radius * .42f) * u, (cx + radius) * u, cy * u)
                close()
            }
            drawPath(shape, tint, style = style)
        }

        when (key) {
            "daily" -> {
                drawCircle(accent.copy(alpha = .72f), 3.1f * u, p(17.5f, 6.2f))
                repeat(5) { index ->
                    val angle = index * 72f * Math.PI / 180.0
                    line(17.5f + kotlin.math.cos(angle).toFloat() * 4.4f, 6.2f + kotlin.math.sin(angle).toFloat() * 4.4f,
                        17.5f + kotlin.math.cos(angle).toFloat() * 5.4f, 6.2f + kotlin.math.sin(angle).toFloat() * 5.4f, fine)
                }
                path(4.2f to 7.2f, 6.1f to 9.1f, 9f to 5.8f, style = fine)
                line(10.8f, 7.3f, 14.1f, 7.2f, fine)
                path(4.2f to 13f, 6.1f to 14.9f, 9f to 11.6f, style = fine)
                line(10.8f, 13.1f, 19.5f, 12.9f, fine)
                path(4.2f to 18.4f, 6.1f to 20.2f, 9f to 17f, style = fine)
                line(10.8f, 18.5f, 18f, 18.3f, fine)
            }
            "work" -> {
                drawRoundRect(tint, p(3.5f, 8f), Size(17f * u, 11.5f * u), CornerRadius(2.2f * u), style = stroke)
                path(8f to 8f, 8.6f to 4.8f, 15.5f to 4.6f, 16f to 8f, style = fine)
                line(3.8f, 12f, 20.1f, 11.6f, fine); line(12f, 11.5f, 12.1f, 14.2f, fine)
            }
            "study" -> {
                path(3.2f to 6.2f, 7.2f to 5f, 11.8f to 7.3f, 12f to 19f, 7.2f to 16.9f, 3.5f to 17.8f, 3.2f to 6.2f, closed = true)
                path(12f to 7.3f, 16.7f to 5.2f, 20.7f to 6.3f, 20.3f to 17.9f, 16.7f to 17f, 12f to 19f)
            }
            "home" -> {
                path(3.5f to 11.2f, 12f to 4.2f, 20.7f to 11f)
                path(5.5f to 10f, 5.8f to 20f, 18.5f to 19.7f, 18.6f to 9.6f)
                drawRoundRect(tint, p(10f, 13f), Size(4.4f * u, 6.8f * u), CornerRadius(1f * u), style = fine)
            }
            "health" -> {
                val heart = Path().apply { moveTo(12f*u,20f*u); cubicTo(3f*u,14f*u,3f*u,6f*u,8.5f*u,5.2f*u); cubicTo(11f*u,4.8f*u,12f*u,7.2f*u,12f*u,7.2f*u); cubicTo(12f*u,7.2f*u,13.4f*u,4.6f*u,16f*u,5.1f*u); cubicTo(22f*u,6f*u,21f*u,14f*u,12f*u,20f*u) }
                drawPath(heart, tint, style = stroke); path(5.7f to 12.2f, 8.7f to 12.1f, 10.2f to 9.2f, 12.5f to 15.2f, 14.5f to 11.4f, 18.2f to 11.3f, style = fine)
            }
            "shopping" -> {
                path(4.2f to 8f, 19.4f to 8.2f, 18.1f to 20f, 5.8f to 19.6f, 4.2f to 8f, closed = true)
                val handle = Path().apply { moveTo(8f*u,8f*u); cubicTo(8f*u,3.8f*u,15.8f*u,3.5f*u,15.8f*u,8.1f*u) }
                drawPath(handle, tint, style = stroke); drawCircle(accent, 1.4f*u, p(15.2f,14f))
            }
            "finance" -> {
                irregularCircle(12f, 12f, 8.2f)
                path(14.8f to 8f, 12.8f to 6.8f, 9.3f to 8.4f, 10.3f to 11.5f, 14.2f to 12.4f, 14.5f to 15.7f, 11.6f to 17.2f, 9.1f to 16f)
                line(12f, 5.5f, 12f, 18.5f, fine)
            }
            "travel" -> {
                val pin = Path().apply { moveTo(12f*u,21f*u); cubicTo(4f*u,13f*u,5f*u,4f*u,12f*u,3.8f*u); cubicTo(19.5f*u,4f*u,20f*u,13f*u,12f*u,21f*u) }
                drawPath(pin, tint, style = stroke); irregularCircle(12f, 10f, 2.5f, fine)
            }
            "idea" -> {
                val bulb = Path().apply { moveTo(8f*u,15f*u); cubicTo(3.8f*u,9f*u,7f*u,3.8f*u,12f*u,3.7f*u); cubicTo(18f*u,3.6f*u,20f*u,10f*u,16f*u,15f*u); lineTo(15f*u,17f*u); lineTo(9f*u,17f*u); close() }
                drawPath(bulb, tint, style = stroke); line(9.5f,20f,14.5f,19.8f,fine); line(12f,1.2f,12f,0f,fine); drawCircle(accent,1.3f*u,p(16.8f,5.5f))
            }
            "project" -> {
                line(6f,3.5f,6.3f,21f); path(7f to 5f, 18.5f to 5.2f, 15.8f to 9.1f, 18.5f to 12.8f, 7.1f to 12.5f, closed = true)
            }
            "habit" -> {
                val loop = Path().apply { moveTo(18.5f*u,8f*u); cubicTo(14f*u,2.5f*u,5f*u,5f*u,5.2f*u,12f*u); cubicTo(5.4f*u,19f*u,15f*u,21f*u,19f*u,15f*u) }
                drawPath(loop,tint,style=stroke); path(14.8f to 5f,18.8f to 8f,18f to 3f,style=fine); path(15f to 18.8f,19f to 15f,14f to 14.8f,style=fine)
            }
            "reading" -> {
                path(7f to 3.8f, 17f to 4.2f, 16.7f to 20.3f, 12f to 16.9f, 7.3f to 20f, 7f to 3.8f, closed = true)
                line(9.5f,8f,14.8f,8.1f,fine); line(9.5f,11.5f,14f,11.6f,fine)
            }
            "sport" -> {
                path(4f to 16f, 8f to 10f, 10f to 13.3f, 15.3f to 15f, 20f to 18.6f, 18.2f to 21f, 7f to 20f, 4f to 16f, closed = true)
                line(9.7f,13.1f,12f,9f,fine); line(12f,14f,14f,10.8f,fine)
            }
            "chores" -> {
                line(14.8f,3f,9.3f,14.2f); path(7f to 13f, 12f to 15.4f, 15.5f to 21f, 4.2f to 20f, 7f to 13f, closed = true)
                line(7.2f,16.3f,12.8f,18.1f,fine)
            }
            "social" -> {
                irregularCircle(8f,8f,3.2f,fine); irregularCircle(16f,8.5f,3f,fine)
                val people = Path().apply { moveTo(2.8f*u,19f*u); cubicTo(3.5f*u,13f*u,12f*u,12.5f*u,12.5f*u,19f*u); moveTo(11.5f*u,18.8f*u); cubicTo(12f*u,13.5f*u,20.2f*u,13.5f*u,21f*u,19f*u) }
                drawPath(people,tint,style=stroke)
            }
            else -> {
                path(3.5f to 6.8f, 9.5f to 7f, 11f to 9f, 20.5f to 9.1f, 19.6f to 19.6f, 4f to 20f, 3.5f to 6.8f, closed = true)
                line(7f,13f,17.2f,12.8f,fine); drawCircle(accent,1.2f*u,p(17.5f,6f))
            }
        }
    }
}
