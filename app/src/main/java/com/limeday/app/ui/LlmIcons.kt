package com.limeday.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.limeday.app.llm.LlmProtocol

enum class LlmActionIconType { Add, Activate, Edit, More, Duplicate, MoveUp, MoveDown, Delete, Refresh, Test, Favorite }

@Composable
fun LlmProtocolIcon(protocol: LlmProtocol, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = size.minDimension * .09f
        val center = this.center
        when (protocol) {
            LlmProtocol.OPENAI_CHAT -> {
                drawCircle(tint, radius = size.minDimension * .34f, center = center, style = Stroke(stroke))
                drawLine(tint, Offset(center.x - size.width * .22f, center.y), Offset(center.x + size.width * .22f, center.y), stroke, StrokeCap.Round)
                drawLine(tint, Offset(center.x, center.y - size.height * .22f), Offset(center.x, center.y + size.height * .22f), stroke, StrokeCap.Round)
            }
            LlmProtocol.OPENAI_RESPONSES -> {
                repeat(3) { index ->
                    val y = size.height * (.28f + index * .22f)
                    drawCircle(tint, radius = stroke * .55f, center = Offset(size.width * .25f, y))
                    drawLine(tint, Offset(size.width * .4f, y), Offset(size.width * .78f, y), stroke, StrokeCap.Round)
                }
            }
            LlmProtocol.ANTHROPIC_MESSAGES -> {
                drawLine(tint, Offset(size.width * .24f, size.height * .76f), Offset(size.width * .5f, size.height * .22f), stroke, StrokeCap.Round)
                drawLine(tint, Offset(size.width * .5f, size.height * .22f), Offset(size.width * .76f, size.height * .76f), stroke, StrokeCap.Round)
                drawLine(tint, Offset(size.width * .34f, size.height * .56f), Offset(size.width * .66f, size.height * .56f), stroke, StrokeCap.Round)
            }
            LlmProtocol.GEMINI_NATIVE -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(center.x, size.height * .16f)
                    quadraticTo(center.x, center.y, size.width * .84f, center.y)
                    quadraticTo(center.x, center.y, center.x, size.height * .84f)
                    quadraticTo(center.x, center.y, size.width * .16f, center.y)
                    quadraticTo(center.x, center.y, center.x, size.height * .16f)
                }
                drawPath(path, tint, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
    }
}

@Composable
fun LlmActionIcon(type: LlmActionIconType, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = size.minDimension * .095f
        val left = size.width * .22f
        val right = size.width * .78f
        val top = size.height * .22f
        val bottom = size.height * .78f
        fun line(a: Offset, b: Offset) = drawLine(tint, a, b, stroke, StrokeCap.Round)
        when (type) {
            LlmActionIconType.Add -> {
                line(Offset(size.width / 2, top), Offset(size.width / 2, bottom))
                line(Offset(left, size.height / 2), Offset(right, size.height / 2))
            }
            LlmActionIconType.Activate -> {
                line(Offset(left, size.height * .52f), Offset(size.width * .43f, bottom))
                line(Offset(size.width * .43f, bottom), Offset(right, top))
            }
            LlmActionIconType.Edit -> {
                line(Offset(size.width * .28f, size.height * .72f), Offset(size.width * .7f, size.height * .3f))
                line(Offset(size.width * .28f, size.height * .72f), Offset(size.width * .22f, size.height * .8f))
                line(Offset(size.width * .62f, size.height * .25f), Offset(size.width * .75f, size.height * .38f))
            }
            LlmActionIconType.More -> repeat(3) { drawCircle(tint, stroke * .7f, Offset(size.width * (.28f + it * .22f), size.height / 2)) }
            LlmActionIconType.Duplicate -> {
                drawRect(tint, Offset(size.width * .2f, size.height * .28f), androidx.compose.ui.geometry.Size(size.width * .45f, size.height * .5f), style = Stroke(stroke))
                drawRect(tint, Offset(size.width * .35f, size.height * .18f), androidx.compose.ui.geometry.Size(size.width * .45f, size.height * .5f), style = Stroke(stroke))
            }
            LlmActionIconType.MoveUp, LlmActionIconType.MoveDown -> {
                val up = type == LlmActionIconType.MoveUp
                val tipY = if (up) top else bottom
                val baseY = if (up) bottom else top
                line(Offset(size.width / 2, tipY), Offset(size.width / 2, baseY))
                line(Offset(size.width / 2, tipY), Offset(left, size.height / 2))
                line(Offset(size.width / 2, tipY), Offset(right, size.height / 2))
            }
            LlmActionIconType.Delete -> {
                drawRect(tint, Offset(size.width * .3f, size.height * .32f), androidx.compose.ui.geometry.Size(size.width * .4f, size.height * .5f), style = Stroke(stroke))
                line(Offset(size.width * .24f, size.height * .27f), Offset(size.width * .76f, size.height * .27f))
                line(Offset(size.width * .42f, size.height * .18f), Offset(size.width * .58f, size.height * .18f))
            }
            LlmActionIconType.Refresh -> {
                drawArc(tint, -55f, 275f, false, style = Stroke(stroke, cap = StrokeCap.Round))
                line(Offset(size.width * .75f, size.height * .2f), Offset(size.width * .76f, size.height * .43f))
                line(Offset(size.width * .75f, size.height * .2f), Offset(size.width * .54f, size.height * .22f))
            }
            LlmActionIconType.Test -> {
                drawCircle(tint, size.minDimension * .3f, style = Stroke(stroke))
                line(Offset(size.width * .5f, size.height * .5f), Offset(size.width * .67f, size.height * .38f))
                drawCircle(tint, stroke * .55f, Offset(size.width * .5f, size.height * .5f))
            }
            LlmActionIconType.Favorite -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * .5f, top)
                    lineTo(size.width * .59f, size.height * .42f)
                    lineTo(right, size.height * .44f)
                    lineTo(size.width * .65f, size.height * .57f)
                    lineTo(size.width * .69f, bottom)
                    lineTo(size.width * .5f, size.height * .67f)
                    lineTo(size.width * .31f, bottom)
                    lineTo(size.width * .35f, size.height * .57f)
                    lineTo(left, size.height * .44f)
                    lineTo(size.width * .41f, size.height * .42f)
                    close()
                }
                drawPath(path, tint, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
    }
}
