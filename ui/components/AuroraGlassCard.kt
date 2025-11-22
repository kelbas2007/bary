package com.example.bary.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.bary.ui.theme.AuroraMint

@Composable
fun AuroraGlassCard(
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
    opacity: Float = 0.15f, // Параметр для контроля непрозрачности (по умолчанию 15% как раньше)
    content: @Composable BoxScope.() -> Unit
) {
    val cornerRadius = 24.dp

    val defaultBorder = BorderStroke(
        width = 1.dp,
        brush = Brush.verticalGradient(
            colors = listOf(
                AuroraMint.copy(alpha = 0.5f),
                AuroraMint.copy(alpha = 0.2f)
            )
        )
    )

    // Вычисляем цвета фона на основе параметра opacity
    val topOpacity = (opacity * 255).toInt().coerceIn(0, 255)
    val bottomOpacity = ((opacity - 0.05f) * 255).toInt().coerceIn(0, 255)
    
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(topOpacity shl 24 or 0xFFFFFF), // Верхний цвет с заданной непрозрачностью
            Color(bottomOpacity shl 24 or 0xFFFFFF)  // Нижний цвет немного прозрачнее
        )
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundBrush)
            .border(
                border = border ?: defaultBorder,
                shape = RoundedCornerShape(cornerRadius)
            ),
        content = content
    )
}