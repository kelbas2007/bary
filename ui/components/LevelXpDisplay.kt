package com.example.bary.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bary.ui.i18n.stringResource
import com.example.bary.ui.theme.AuroraMint
import com.example.bary.ui.theme.NeonYellow
import kotlin.math.pow

/**
 * Компонент для отображения уровня игрока и прогресса XP
 */
@Composable
fun LevelXpDisplay(
    level: Int,
    currentXp: Int,
    modifier: Modifier = Modifier,
    showDetails: Boolean = true
) {
    // Вычисляем требуемый XP для следующего уровня
    val requiredXp = (100 * 1.25.pow(level - 1)).toInt()
    val progress = (currentXp.toFloat() / requiredXp.toFloat()).coerceIn(0f, 1f)
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000),
        label = "xp_progress"
    )

    AuroraGlassCard(
        modifier = modifier,
        opacity = 0.2f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Уровень и иконка
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Level",
                    tint = NeonYellow,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                
                Column {
                    Text(
                        text = stringResource("level", "level" to level.toString()),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (showDetails) {
                        Text(
                            text = "$currentXp / $requiredXp XP",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            if (showDetails) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Прогресс-бар
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = AuroraMint,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Текст прогресса
                val xpToNextLevel = requiredXp - currentXp
                Text(
                    text = stringResource("xp_to_next_level", "level" to (level + 1).toString(), "xp" to xpToNextLevel.toString()),
                    style = MaterialTheme.typography.bodySmall,
                    color = AuroraMint,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Компактная версия для отображения в хедере
 */
@Composable
fun CompactLevelDisplay(
    level: Int,
    currentXp: Int,
    modifier: Modifier = Modifier
) {
    val requiredXp = (100 * 1.25.pow(level - 1)).toInt()
    val progress = (currentXp.toFloat() / requiredXp.toFloat()).coerceIn(0f, 1f)
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(8.dp)
    ) {
        // Иконка уровня
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Level",
            tint = NeonYellow,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        
        Column {
            Text(
                text = stringResource("level", "level" to level.toString()),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Мини прогресс-бар
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .width(60.dp)
                    .height(4.dp),
                color = AuroraMint,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            )
        }
    }
}

