package com.example.bary.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bary.data.model.Skill
import com.example.bary.ui.i18n.LocalAppLanguage
import com.example.bary.ui.i18n.StringResources
import com.example.bary.ui.i18n.stringResource
import com.example.bary.ui.theme.AuroraBlue
import com.example.bary.ui.theme.AuroraMint
import com.example.bary.ui.theme.AuroraPurple
import com.example.bary.ui.theme.NeonYellow

@Composable
fun SkillNode(
    skill: Skill,
    isUnlocked: Boolean,
    canBeUnlocked: Boolean,
    onClick: () -> Unit
) {
    // Плавная пульсация для доступных узлов
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Определяем стиль узла
    data class NodeStyle(
        val borderBrush: Brush,
        val glowColor: Color,
        val bgGradient: Brush,
        val textColor: Color
    )
    
    val nodeStyle = when {
        isUnlocked -> {
            val colors = listOf(
                AuroraMint.copy(alpha = 0.8f),
                AuroraBlue.copy(alpha = 0.6f)
            )
            NodeStyle(
                borderBrush = Brush.linearGradient(colors),
                glowColor = AuroraMint.copy(alpha = 0.4f),
                bgGradient = Brush.radialGradient(
                    colors = listOf(
                        AuroraMint.copy(alpha = 0.3f),
                        AuroraBlue.copy(alpha = 0.15f),
                        Color.Transparent
                    )
                ),
                textColor = Color.White
            )
        }
        canBeUnlocked -> {
            val colors = listOf(
                NeonYellow.copy(alpha = 0.8f),
                AuroraPurple.copy(alpha = 0.6f)
            )
            NodeStyle(
                borderBrush = Brush.linearGradient(colors),
                glowColor = NeonYellow.copy(alpha = 0.3f),
                bgGradient = Brush.radialGradient(
                    colors = listOf(
                        NeonYellow.copy(alpha = 0.25f),
                        AuroraPurple.copy(alpha = 0.1f),
                        Color.Transparent
                    )
                ),
                textColor = Color.White
            )
        }
        else -> {
            NodeStyle(
                borderBrush = Brush.linearGradient(
                    listOf(
                        Color.Gray.copy(alpha = 0.3f),
                        Color.Gray.copy(alpha = 0.2f)
                    )
                ),
                glowColor = Color.Transparent,
                bgGradient = Brush.radialGradient(
                    colors = listOf(
                        Color.Gray.copy(alpha = 0.15f),
                        Color.Gray.copy(alpha = 0.05f),
                        Color.Transparent
                    )
                ),
                textColor = Color.Gray.copy(alpha = 0.7f)
            )
        }
    }

    Box(
        modifier = Modifier
            .size(100.dp)
            .scale(if (canBeUnlocked) pulseScale else 1f)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Внешнее свечение
        if (nodeStyle.glowColor != Color.Transparent) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .blur(15.dp)
                    .clip(CircleShape)
                    .background(nodeStyle.glowColor)
            )
        }

        // Основной узел с градиентным фоном
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(nodeStyle.bgGradient)
                .border(
                    width = 2.5.dp,
                    brush = nodeStyle.borderBrush,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!isUnlocked && !canBeUnlocked) {
                // Заблокированный - показываем замок
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = nodeStyle.textColor,
                    modifier = Modifier.size(36.dp)
                )
            } else {
                // Разблокированный или доступный - показываем название
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    val language = LocalAppLanguage.current
                    val localizedTitle = StringResources.getSkillTitle(skill.id, language)
                    Text(
                        text = localizedTitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = nodeStyle.textColor,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
