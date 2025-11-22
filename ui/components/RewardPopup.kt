package com.example.bary.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bary.ui.i18n.stringResource
import com.example.bary.ui.theme.AuroraMint
import com.example.bary.ui.theme.NeonYellow
import kotlinx.coroutines.delay

/**
 * Данные о награде
 */
data class RewardData(
    val title: String,
    val description: String,
    val xpGained: Int,
    val isLevelUp: Boolean = false,
    val newLevel: Int? = null
)

/**
 * Всплывающее окно с наградой
 */
@Composable
fun RewardPopup(
    reward: RewardData?,
    finalScore: Int = 0,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "reward_scale"
    )
    
    LaunchedEffect(reward) {
        if (reward != null) {
            isVisible = true
            delay(3000) // Показываем 3 секунды
            isVisible = false
            delay(300) // Ждем окончания анимации
            onDismiss()
        }
    }
    
    AnimatedVisibility(
        visible = isVisible && reward != null,
        enter = fadeIn(tween(300)) + scaleIn(tween(300)),
        exit = fadeOut(tween(300)) + scaleOut(tween(300)),
        label = "RewardPopup"
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            reward?.let { rewardData ->
                AuroraGlassCard(
                    modifier = Modifier
                        .padding(32.dp)
                        .scale(scale),
                    opacity = 0.95f
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Иконка награды
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    if (rewardData.isLevelUp) NeonYellow else AuroraMint,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (rewardData.isLevelUp) Icons.Default.Star else Icons.Default.EmojiEvents,
                                contentDescription = "Reward",
                                tint = Color.Black,
                                modifier = Modifier.size(48.dp)
                                )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Заголовок (локализованный)
                        val title = when {
                            finalScore >= 80 -> stringResource("excellent")
                            finalScore >= 60 -> stringResource("good")
                            else -> stringResource("test_passed")
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Описание (локализованное)
                        val description = when {
                            finalScore >= 80 -> stringResource("correct_answers_percent", "percent" to finalScore.toString())
                            finalScore >= 60 -> stringResource("good_result_continue")
                            else -> stringResource("try_again")
                        }
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // XP полученный
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "XP",
                                tint = AuroraMint,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "+${rewardData.xpGained} XP",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = AuroraMint,
                                fontSize = 24.sp
                            )
                        }
                        
                        // Если повышение уровня
                        if (rewardData.isLevelUp && rewardData.newLevel != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource("level_up", "level" to rewardData.newLevel.toString()),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeonYellow
                            )
                        }
                    }
                }
            }
        }
    }
}

