package com.example.bary.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.bary.R

/**
 * Универсальный компонент для отображения маскота Бари и его подсказки.
 *
 * @param tipText Текст подсказки. Если null или пустой, подсказка не показывается.
 * @param modifier Модификатор для настройки всего компонента.
 */
@Composable
fun BariTip(
    tipText: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !tipText.isNullOrBlank(),
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut(),
        label = "BariTipVisibility"
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.bary_static),
                contentDescription = "Маскот Бари",
                modifier = Modifier
                    .size(100.dp)
                    .offset(y = 10.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .shadow(
                        elevation = 8.dp,
                        shape = MaterialTheme.shapes.large,
                        spotColor = MaterialTheme.colorScheme.primary
                    )
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Text(
                    text = tipText ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}