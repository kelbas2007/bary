package com.example.bary.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bary.data.local.datastore.AppLanguage
import com.example.bary.ui.i18n.LocalAppLanguage
import com.example.bary.ui.i18n.StringResources
import com.example.bary.ui.theme.CurrentDesign
import com.example.bary.ui.theme.AppThemeMode
import com.example.bary.ui.theme.BariTheme
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.pow

/**
 * Компонент для отображения "Монитора Состояния" (Целостность Ядра)
 */
@Composable
fun CoreIntegrityMonitor(
    level: Int,
    currentXp: Int,
    modifier: Modifier = Modifier
) {
    val isScifi = BariTheme.mode == AppThemeMode.SCIFI
    val language = LocalAppLanguage.current
    val palette = CurrentDesign.palette
    
    // Вычисляем требуемый XP для следующего уровня
    val requiredXp = (100 * 1.25.pow(level - 1)).toInt()
    val progress = (currentXp.toFloat() / requiredXp.toFloat()).coerceIn(0f, 1f)
    
    // Рассчитываем "Целостность Ядра" как процент от максимального уровня
    val maxLevel = 20 // Предполагаем максимальный уровень
    val coreIntegrity = ((level.toFloat() / maxLevel.toFloat()) * 100f).coerceIn(0f, 100f)
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000),
        label = "core_integrity_progress"
    )
    
    if (isScifi) {
        // SCIFI версия - круговая диаграмма XP с эффектами плазмы
        SciFiXpRing(
            level = level,
            currentXp = currentXp,
            xpForNextLevel = requiredXp,
            coreIntegrity = coreIntegrity.toInt(),
            modifier = modifier
        )
    } else {
        // STANDARD версия
        AuroraGlassCard(
            modifier = modifier.fillMaxWidth(),
            opacity = 0.2f,
            border = BorderStroke(
                width = 2.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        palette.primary.copy(alpha = 0.8f),
                        palette.mint.copy(alpha = 0.6f)
                    )
                )
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Заголовок "Целостность Ядра"
                Text(
                    text = StringResources.getString("core_integrity", language) + ": ${coreIntegrity.toInt()}%",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.primary,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Шкала Энергии (XP Bar)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Gray.copy(alpha = 0.3f))
                ) {
                    // Градиентное заполнение
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        palette.primary,
                                        palette.mint
                                    )
                                )
                            )
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Подпись
                val xpToNext = requiredXp - currentXp
                Text(
                    text = StringResources.getString("data_accumulation", language) + "... " +
                           StringResources.getString("xp_to_next_level", language)
                               .replace("{level}", (level + 1).toString())
                               .replace("{xp}", xpToNext.toString()),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.text.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun SciFiXpRing(
    level: Int,
    currentXp: Int,
    xpForNextLevel: Int,
    coreIntegrity: Int,
    modifier: Modifier = Modifier
) {
    val progress = (currentXp.toFloat() / xpForNextLevel.toFloat()).coerceIn(0f, 1f)
    val language = LocalAppLanguage.current
    
    // Анимация пульсации при заполнении
    val infiniteTransition = rememberInfiniteTransition(label = "xp_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000),
        label = "xp_ring_progress"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Круговая диаграмма XP
            val density = LocalDensity.current
            val tertiaryColor = BariTheme.colors.tertiary
            val primaryColor = BariTheme.colors.primary
            val cardBgColor = BariTheme.colors.cardBackground
            val textPrimaryColor = BariTheme.colors.textPrimary
            val textSecondaryColor = BariTheme.colors.textSecondary
            
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = with(density) { 12.dp.toPx() }
                    val canvasSize = this.size
                    val radius = (canvasSize.minDimension / 2) - (strokeWidth / 2)
                    val center = Offset(canvasSize.width / 2, canvasSize.height / 2)
                    
                    // Фон кольца
                    drawCircle(
                        color = cardBgColor.copy(alpha = 0.3f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeWidth)
                    )
                    
                    // Заполненная часть (плазма)
                    val sweepAngle = 360f * animatedProgress
                    if (sweepAngle > 0) {
                        val rect = androidx.compose.ui.geometry.Rect(
                            offset = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2)
                        )
                        val path = Path()
                        path.moveTo(center.x, center.y - radius)
                        path.arcTo(
                            rect = rect,
                            startAngleDegrees = -90f,
                            sweepAngleDegrees = sweepAngle,
                            forceMoveTo = false
                        )
                        
                        drawPath(
                            path = path,
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    tertiaryColor.copy(alpha = pulseAlpha),
                                    primaryColor.copy(alpha = pulseAlpha * 0.7f),
                                    tertiaryColor.copy(alpha = pulseAlpha)
                                ),
                                center = center
                            ),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }
                
                // Уровень в центре
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Lv.$level",
                        style = MaterialTheme.typography.headlineLarge,
                        color = textPrimaryColor,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "$currentXp / $xpForNextLevel XP",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondaryColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Целостность ядра
            Text(
                text = StringResources.getString("core_integrity", language) + ": ${coreIntegrity}%",
                style = MaterialTheme.typography.titleMedium,
                color = BariTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

