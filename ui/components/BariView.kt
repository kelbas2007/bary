package com.example.bary.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.bary.ui.BariAsset
import com.example.bary.ui.BariEvent
import com.example.bary.ui.BariPosition
import com.example.bary.ui.BariUiState
import com.example.bary.ui.theme.AuroraMint
import com.example.bary.ui.theme.AuroraPurple
import kotlin.math.roundToInt

@Composable
fun BariView(
    modifier: Modifier = Modifier,
    state: BariUiState,
    onEvent: (BariEvent) -> Unit
) {
    if (!state.isVisible || state.position is BariPosition.Hidden) {
        return
    }

    Box(modifier = modifier) {
        val positionModifier = when (val pos = state.position) {
            is BariPosition.BottomEnd -> Modifier.align(Alignment.BottomEnd)
            is BariPosition.BottomStart -> Modifier.align(Alignment.BottomStart)
            is BariPosition.CenterScreen -> Modifier.align(Alignment.Center)
            is BariPosition.Hidden -> Modifier // Не должно быть видно
            is BariPosition.Custom -> Modifier.offset {
                IntOffset(pos.offsetX.roundToInt(), pos.offsetY.roundToInt())
            }
        }

        Box(
            modifier = positionModifier
                .then(
                    if (state.position !is BariPosition.Custom) {
                        Modifier.offset {
                            IntOffset(state.offsetX.roundToInt(), state.offsetY.roundToInt())
                        }
                    } else {
                        Modifier
                    }
                )
                .then(
                    if (state.position !is BariPosition.CenterScreen) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onEvent(BariEvent.OnDrag(dragAmount))
                            }
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            // Bari's hint bubble с улучшенным дизайном
            if (state.currentHint != null) {
                BariBubbleHint(
                    text = state.currentHint,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(bottom = 160.dp)
                )
            }

            // Эффект свечения вокруг Бари - оптимизированная анимация (медленнее для лучшей производительности)
            val infiniteTransition = rememberInfiniteTransition(label = "bari_glow")
            val glowScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.12f, // Уменьшена амплитуда для меньшей нагрузки
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = LinearEasing), // Увеличена длительность с 2000 до 3000ms
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glow_scale"
            )

            // Внешнее свечение
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(180.dp)
                    .offset(y = (-10).dp)
                    .scale(glowScale)
                    .blur(20.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                AuroraMint.copy(alpha = 0.3f),
                                AuroraPurple.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Среднее свечение
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(160.dp)
                    .offset(y = (-5).dp)
                    .scale(glowScale * 0.9f)
                    .blur(15.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                AuroraMint.copy(alpha = 0.4f),
                                AuroraPurple.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Bari's avatar с Crossfade animation и улучшенным дизайном
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                AuroraMint.copy(alpha = 0.2f),
                                AuroraPurple.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                Crossfade(
                    targetState = state.asset,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(140.dp),
                    label = "BariAssetCrossfade"
                ) { asset ->
                    when (asset) {
                        is BariAsset.Image -> {
                            Image(
                                painter = painterResource(id = asset.drawableResId),
                                contentDescription = "Bari Assistant",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BariBubbleHint(text: String, modifier: Modifier = Modifier) {
    // Пульсирующая анимация для привлечения внимания - оптимизированная
    val infiniteTransition = rememberInfiniteTransition(label = "bubble_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f, // Уменьшена амплитуда
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing), // Увеличена длительность с 1500 до 2500ms
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier
            .scale(pulseScale)
    ) {
        // Эффект свечения вокруг подсказки
        Box(
            modifier = Modifier
                .widthIn(max = 220.dp)
                .blur(8.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AuroraMint.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        AuroraGlassCard(
            modifier = Modifier.widthIn(max = 200.dp),
            opacity = 0.95f, // Высокая непрозрачность для читаемости текста
            border = androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        AuroraMint.copy(alpha = 0.6f),
                        AuroraPurple.copy(alpha = 0.4f)
                    )
                )
            )
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF1C1B1F) // Явный темный цвет для читаемости на белом фоне
            )
        }
    }
}
