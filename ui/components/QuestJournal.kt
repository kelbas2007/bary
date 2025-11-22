package com.example.bary.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bary.data.local.datastore.AppLanguage
import com.example.bary.data.model.Quest
import com.example.bary.ui.i18n.LocalAppLanguage
import com.example.bary.data.model.QuestProgress
import com.example.bary.data.model.QuestType
import com.example.bary.ui.i18n.StringResources
import com.example.bary.ui.theme.CurrentDesign

/**
 * Компонент для отображения "Журнала Миссий" (список активных квестов)
 */
@Composable
fun QuestJournal(
    quests: List<QuestProgress>,
    onQuestClick: (QuestProgress) -> Unit,
    onCollectReward: (QuestProgress) -> Unit,
    onHeaderClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeQuests = quests.filter { !it.isCompleted }
    
    if (activeQuests.isEmpty()) {
        return // Не показываем секцию, если нет активных квестов
    }
    
    val language = LocalAppLanguage.current
    
    Column(modifier = modifier.fillMaxWidth()) {
        // Заголовок секции - кликабельный для перехода к экрану всех квестов
        Text(
            text = StringResources.getString("active_protocols", language),
            style = MaterialTheme.typography.titleLarge,
            color = CurrentDesign.palette.yellow,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable { onHeaderClick() }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Карточки заданий
        activeQuests.forEach { questProgress ->
            QuestCard(
                questProgress = questProgress,
                onClick = { 
                    if (questProgress.actionRoute != null) {
                        onQuestClick(questProgress)
                    }
                },
                onCollectReward = { onCollectReward(questProgress) },
                language = language
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun QuestCard(
    questProgress: QuestProgress,
    onClick: () -> Unit,
    onCollectReward: () -> Unit,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val palette = CurrentDesign.palette
    val borderColor = if (questProgress.isCompleted) palette.mint else palette.secondary
    
    AuroraGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        opacity = 0.2f,
        border = BorderStroke(
            width = 2.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    borderColor.copy(alpha = 0.8f),
                    borderColor.copy(alpha = 0.5f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка задачи
            Icon(
                imageVector = getQuestIcon(questProgress.questType),
                contentDescription = null,
                tint = borderColor,
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Текст задания
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = StringResources.getString("quest_${questProgress.quest.id}_title", language) 
                        ?: questProgress.quest.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.text,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = StringResources.getString("quest_${questProgress.quest.id}_description", language) 
                        ?: (questProgress.quest.description[language.name.lowercase()] ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.text.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Индикатор прогресса
                Text(
                    text = "${questProgress.progressCount}/${questProgress.targetCount}",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.yellow
                )
            }
            
            // Кнопка "ЗАБРАТЬ"
            if (questProgress.isCompleted) {
                Button(
                    onClick = onCollectReward,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.mint,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = StringResources.getString("collect_reward", language),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun getQuestIcon(questType: QuestType): ImageVector {
    return when (questType) {
        QuestType.TRANSACTION_COUNT -> Icons.Default.ShoppingCart
        QuestType.PIGGY_BANK_CREATE -> Icons.Default.Add
        QuestType.LESSON_COMPLETE -> Icons.Default.School
        QuestType.QUIZ_COMPLETE -> Icons.Default.Star
        QuestType.DAILY_LOGIN -> Icons.Default.CheckCircle
        QuestType.GENERAL -> Icons.Default.Star
    }
}

