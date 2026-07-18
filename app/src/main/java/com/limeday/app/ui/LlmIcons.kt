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
import com.limeday.app.llm.LlmProtocol
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class LlmActionIconType { Add, Activate, Edit, More, Duplicate, MoveUp, MoveDown, Delete, Refresh, Test, Favorite }

enum class LlmProviderMark {
    OpenAi, Anthropic, Gemini, OpenRouter, DeepSeek, Kimi, Qwen, Zhipu,
    SiliconFlow, MiniMax, Doubao, Xai, Mistral, Groq, Ollama, Custom
}

fun llmProviderMark(presetId: String): LlmProviderMark = when (presetId.lowercase()) {
    "openai" -> LlmProviderMark.OpenAi
    "anthropic" -> LlmProviderMark.Anthropic
    "gemini" -> LlmProviderMark.Gemini
    "openrouter" -> LlmProviderMark.OpenRouter
    "deepseek" -> LlmProviderMark.DeepSeek
    "kimi" -> LlmProviderMark.Kimi
    "qwen" -> LlmProviderMark.Qwen
    "zhipu" -> LlmProviderMark.Zhipu
    "siliconflow" -> LlmProviderMark.SiliconFlow
    "minimax" -> LlmProviderMark.MiniMax
    "doubao" -> LlmProviderMark.Doubao
    "xai" -> LlmProviderMark.Xai
    "mistral" -> LlmProviderMark.Mistral
    "groq" -> LlmProviderMark.Groq
    "ollama" -> LlmProviderMark.Ollama
    else -> LlmProviderMark.Custom
}

/** Recognisable vendor marks redrawn with the same uneven, round-ended LimeDay line work. */
@Composable
fun LlmProviderIcon(
    presetId: String,
    tint: Color,
    modifier: Modifier = Modifier,
    accent: Color = Color(0xFFF4B860)
) {
    val mark = llmProviderMark(presetId)
    Canvas(modifier) {
        val u = size.minDimension / 24f
        val stroke = Stroke(2.05f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val fine = Stroke(1.45f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun p(x: Float, y: Float) = Offset(x * u, y * u)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float, style: Stroke = stroke) =
            drawLine(tint, p(x1, y1), p(x2, y2), style.width, style.cap)
        fun path(points: List<Pair<Float, Float>>, closed: Boolean = false, style: Stroke = stroke) {
            val value = Path().apply {
                points.forEachIndexed { index, point ->
                    if (index == 0) moveTo(point.first * u, point.second * u)
                    else lineTo(point.first * u, point.second * u)
                }
                if (closed) close()
            }
            drawPath(value, tint, style = style)
        }

        when (mark) {
            LlmProviderMark.OpenAi -> {
                repeat(6) { index ->
                    val angle = index * PI.toFloat() / 3f - PI.toFloat() / 2f
                    val next = angle + PI.toFloat() * .72f
                    val start = p(12f + cos(angle) * 6.1f, 12f + sin(angle) * 6.1f)
                    val end = p(12f + cos(next) * 5.4f, 12f + sin(next) * 5.4f)
                    drawLine(tint, start, end, stroke.width, StrokeCap.Round)
                }
                drawCircle(tint, 3.15f * u, p(12f, 12f), style = fine)
            }
            LlmProviderMark.Anthropic -> {
                path(listOf(4.7f to 19.2f, 10.1f to 4.3f, 15.6f to 19.1f))
                line(7.2f, 13.3f, 13.1f, 13.2f, fine)
                line(15.3f, 5.3f, 20f, 18.9f, fine)
            }
            LlmProviderMark.Gemini -> {
                val sparkle = Path().apply {
                    moveTo(12f * u, 2.8f * u)
                    cubicTo(12f * u, 8.4f * u, 15.7f * u, 12f * u, 21.2f * u, 12f * u)
                    cubicTo(15.8f * u, 12f * u, 12f * u, 15.7f * u, 12f * u, 21.2f * u)
                    cubicTo(12f * u, 15.7f * u, 8.3f * u, 12f * u, 2.8f * u, 12f * u)
                    cubicTo(8.3f * u, 12f * u, 12f * u, 8.3f * u, 12f * u, 2.8f * u)
                }
                drawPath(sparkle, tint, style = stroke)
                drawCircle(accent, 1.25f * u, p(18.6f, 5.1f))
            }
            LlmProviderMark.OpenRouter -> {
                val upper = Path().apply {
                    moveTo(3.2f * u, 8.2f * u)
                    cubicTo(7.8f * u, 8.2f * u, 8.5f * u, 4.9f * u, 12.7f * u, 4.9f * u)
                    cubicTo(16.2f * u, 4.9f * u, 16.1f * u, 8.1f * u, 20.7f * u, 8.1f * u)
                }
                drawPath(upper, tint, style = stroke)
                path(listOf(17.7f to 5.2f, 21f to 8.1f, 17.8f to 11f), style = fine)
                val lower = Path().apply {
                    moveTo(20.8f * u, 16f * u)
                    cubicTo(16.5f * u, 16f * u, 15.6f * u, 19.1f * u, 11.4f * u, 19.1f * u)
                    cubicTo(7.8f * u, 19.1f * u, 7.8f * u, 16.1f * u, 3.3f * u, 16.1f * u)
                }
                drawPath(lower, tint, style = stroke)
                path(listOf(6.2f to 13.2f, 3f to 16.1f, 6.2f to 19f), style = fine)
            }
            LlmProviderMark.DeepSeek -> {
                val whale = Path().apply {
                    moveTo(3.2f * u, 13.7f * u)
                    cubicTo(6f * u, 9f * u, 12.5f * u, 8.8f * u, 16.9f * u, 11.6f * u)
                    cubicTo(18.6f * u, 10.7f * u, 20.1f * u, 9.3f * u, 21.2f * u, 7.3f * u)
                    cubicTo(21.7f * u, 13.4f * u, 17.2f * u, 18.6f * u, 11.1f * u, 19f * u)
                    cubicTo(6.5f * u, 19.2f * u, 3.8f * u, 17.3f * u, 3.2f * u, 13.7f * u)
                }
                drawPath(whale, tint, style = stroke)
                drawCircle(tint, .8f * u, p(15.1f, 13f))
                path(listOf(5.1f to 10.8f, 3.3f to 7.4f, 7.7f to 9.1f), style = fine)
            }
            LlmProviderMark.Kimi -> {
                val moon = Path().apply {
                    moveTo(15.8f * u, 3.4f * u)
                    cubicTo(8.1f * u, 3.7f * u, 4f * u, 12.1f * u, 8.3f * u, 18.1f * u)
                    cubicTo(12.2f * u, 23.1f * u, 20.2f * u, 19.3f * u, 20.6f * u, 13.6f * u)
                    cubicTo(15.3f * u, 17.3f * u, 9f * u, 11.1f * u, 15.8f * u, 3.4f * u)
                }
                drawPath(moon, tint, style = stroke)
                drawCircle(accent, 1.05f * u, p(18.5f, 5.4f))
            }
            LlmProviderMark.Qwen -> {
                repeat(6) { index ->
                    val angle = index * PI.toFloat() / 3f
                    val center = p(12f + cos(angle) * 5.4f, 12f + sin(angle) * 5.4f)
                    drawCircle(tint, 3.25f * u, center, style = fine)
                }
                drawCircle(accent, 2f * u, p(12f, 12f))
            }
            LlmProviderMark.Zhipu -> {
                drawRoundRect(tint, p(4f, 4.2f), Size(16f * u, 15.6f * u), CornerRadius(3f * u), style = stroke)
                path(listOf(8f to 8.2f, 16.4f to 8.2f, 16.4f to 15.6f, 10.1f to 15.6f, 10.1f to 11.9f, 13.6f to 11.9f), style = fine)
            }
            LlmProviderMark.SiliconFlow -> {
                repeat(3) { index ->
                    val y = 7.1f + index * 5f
                    val wave = Path().apply {
                        moveTo(3.8f * u, y * u)
                        cubicTo(7f * u, (y - 3f) * u, 9.3f * u, (y + 3f) * u, 12.3f * u, y * u)
                        cubicTo(15.3f * u, (y - 3f) * u, 17.2f * u, (y + 2.6f) * u, 20.2f * u, y * u)
                    }
                    drawPath(wave, tint, style = fine)
                }
            }
            LlmProviderMark.MiniMax -> {
                path(listOf(3.5f to 18.8f, 5.8f to 5.3f, 10.1f to 14.2f, 13.8f to 5.2f, 16.2f to 18.8f))
                path(listOf(12.3f to 18.8f, 15.8f to 11.2f, 20.5f to 18.8f), style = fine)
                line(20.1f, 11.1f, 15.6f, 18.7f, fine)
            }
            LlmProviderMark.Doubao -> {
                val bean = Path().apply {
                    moveTo(12.2f * u, 3.2f * u)
                    cubicTo(19.2f * u, 3.3f * u, 22f * u, 9.1f * u, 18.8f * u, 14.8f * u)
                    cubicTo(15.8f * u, 20.2f * u, 7.2f * u, 21.4f * u, 4.2f * u, 16.2f * u)
                    cubicTo(1.2f * u, 10.8f * u, 5.5f * u, 3.3f * u, 12.2f * u, 3.2f * u)
                }
                drawPath(bean, tint, style = stroke)
                path(listOf(7.2f to 12.3f, 10.2f to 15.2f, 16.9f to 8.6f), style = fine)
            }
            LlmProviderMark.Xai -> {
                line(5f, 4.5f, 19f, 19.5f)
                line(18.6f, 4.2f, 5.3f, 19.8f, fine)
                drawArc(accent, -35f, 245f, false, p(3.4f, 7f), Size(17f * u, 10.5f * u), style = fine)
            }
            LlmProviderMark.Mistral -> {
                val blocks = listOf(5f to 4f, 9f to 4f, 13f to 4f, 17f to 4f, 5f to 8f, 9f to 8f, 13f to 8f, 17f to 8f, 5f to 12f, 9f to 12f, 13f to 12f, 17f to 12f, 5f to 16f, 13f to 16f)
                blocks.forEach { (x, y) -> drawRoundRect(tint, p(x, y), Size(3.2f * u, 3.2f * u), CornerRadius(.5f * u)) }
            }
            LlmProviderMark.Groq -> {
                drawArc(tint, 35f, 292f, false, p(3.6f, 3.7f), Size(16.7f * u, 16.7f * u), style = stroke)
                path(listOf(12.2f to 12f, 20f to 12f, 19.8f to 19.2f, 13.8f to 19.2f), style = fine)
            }
            LlmProviderMark.Ollama -> {
                path(listOf(6.1f to 20f, 6.2f to 9f, 8.2f to 4.2f, 10.4f to 8f, 13.9f to 4f, 17.8f to 8.9f, 18f to 20f, 6.1f to 20f), closed = true)
                line(10.2f, 13.7f, 10.2f, 16.2f, fine)
                line(14.2f, 13.7f, 14.2f, 16.2f, fine)
                line(9.5f, 18.3f, 14.8f, 18.3f, fine)
            }
            LlmProviderMark.Custom -> {
                drawRoundRect(tint, p(5.2f, 8.2f), Size(13.7f * u, 11f * u), CornerRadius(3f * u), style = stroke)
                line(9f, 8.1f, 9f, 4.2f, fine)
                line(15f, 8.1f, 15f, 4.2f, fine)
                path(listOf(9f to 13.2f, 12f to 16f, 15.5f to 12.4f), style = fine)
            }
        }
    }
}

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
