package com.example.bary.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.bary.ui.theme.AuroraMint

/**
 * Интерполяция между двумя значениями
 */
private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

/**
 * Анимированное ребро с "течением энергии"
 * Маркер-частица едет от start к end каждые 1.2с
 */
@Composable
fun FlowingEdge(
    start: Offset,
    end: Offset,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val infiniteTransition = rememberInfiniteTransition(label = "flowing")
    val t by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing)
        ),
        label = "flowing_t"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // Базовая линия активного пути: Mint 6px
        drawLine(
            color = AuroraMint, // #32E2C2
            start = start,
            end = end,
            strokeWidth = with(density) { 6.dp.toPx() },
            cap = StrokeCap.Round
        )

        // Маркер-частица, которая "едет" по линии
        val p = Offset(
            lerp(start.x, end.x, t),
            lerp(start.y, end.y, t)
        )
        drawCircle(
            color = AuroraMint,
            radius = with(density) { 6.dp.toPx() },
            center = p
        )
    }
}

