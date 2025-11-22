package com.example.bary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.bary.ui.theme.AuroraBlue

@Composable
fun AppIcon(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String?,
    brush: Brush? = null,
    tint: Color? = null,
    glow: Boolean = false
) {
    // Определяем, какой способ раскраски использовать
    val finalBrush = if (tint != null) {
        Brush.verticalGradient(colors = listOf(tint, tint.copy(alpha = 0.7f)))
    } else {
        brush ?: Brush.verticalGradient(colors = listOf(AuroraBlue, AuroraBlue.copy(alpha = 0.5f)))
    }
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Эффект свечения (glow) для активной иконки
        if (glow && tint != null) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .blur(8.dp)
                    .clip(CircleShape)
                    .graphicsLayer(alpha = 0.6f)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                tint.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = Color.Unspecified, // Не используем tint напрямую, используем brush
            modifier = Modifier
                .graphicsLayer(alpha = 0.99f) // Workaround for blending
                .drawWithCache {
                    onDrawWithContent {
                        drawContent()
                        drawRect(finalBrush, blendMode = BlendMode.SrcAtop)
                    }
                }
        )
    }
}