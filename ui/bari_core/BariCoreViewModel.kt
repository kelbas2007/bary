package com.example.bary.ui.bari_core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bary.data.model.Quest
import com.example.bary.data.model.QuestProgress
import com.example.bary.data.model.QuestType
import com.example.bary.data.model.Skill
import com.example.bary.data.model.UserProgress
import com.example.bary.repository.FinanceRepository
import com.example.bary.repository.GamificationRepository
import com.example.bary.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BariCoreUiState(
    val skills: List<Skill> = emptyList(),
    val userProgress: UserProgress? = null,
    val quests: List<QuestProgress> = emptyList(),
    val isLoading: Boolean = true
)

sealed class BariCoreEvent {
    data class OnSkillClick(val skill: Skill) : BariCoreEvent()
    data class OnQuestClick(val questProgress: QuestProgress) : BariCoreEvent()
    data class OnQuestRewardCollected(val questProgress: QuestProgress) : BariCoreEvent()
}

@HiltViewModel
class BariCoreViewModel @Inject constructor(
    private val gamificationRepository: GamificationRepository,
    private val financeRepository: FinanceRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BariCoreUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                gamificationRepository.getSkillsFlow(),
                gamificationRepository.getUserProgress(),
                flow { emit(gamificationRepository.getQuests()) }
            ) { skills, userProgress, quests ->
                // Вычисляем прогресс каждого квеста
                val questProgress = calculateQuestProgress(quests, userProgress)
                
                BariCoreUiState(
                    skills = skills,
                    userProgress = userProgress,
                    quests = questProgress,
                    isLoading = false
                )
            }
            .flowOn(Dispatchers.Default) // Выполняем тяжелые вычисления в фоне
            .collect { state ->
                _uiState.update { state }
            }
        }
    }

    private suspend fun calculateQuestProgress(
        quests: List<Quest>,
        userProgress: UserProgress?
    ): List<QuestProgress> {
        val completedQuestIds = userProgress?.completedQuests?.toSet() ?: emptySet()
        
        return quests.map { quest ->
            val progressCount = when {
                quest.id.contains("transaction", ignoreCase = true) -> {
                    // Количество транзакций сегодня
                    financeRepository.getTransactionsToday().size
                }
                quest.id.contains("piggy", ignoreCase = true) || quest.id.contains("копилк", ignoreCase = true) -> {
                    // Количество созданных копилок
                    financeRepository.getAllPiggyBanks().size
                }
                quest.id.contains("lesson", ignoreCase = true) || quest.id.contains("урок", ignoreCase = true) -> {
                    // Количество пройденных уроков
                    userProgress?.completedLessons?.size ?: 0
                }
                quest.id.contains("quiz", ignoreCase = true) || quest.id.contains("тест", ignoreCase = true) -> {
                    // Количество пройденных тестов
                    userProgress?.completedQuizzes?.size ?: 0
                }
                quest.id.contains("login", ignoreCase = true) || quest.id.contains("вход", ignoreCase = true) -> {
                    // Проверяем последний вход (упрощенная версия - всегда 0 или 1)
                    // TODO: Реализовать проверку последнего входа через SettingsRepository
                    0
                }
                else -> 0
            }
            
            val targetCount = when {
                quest.id.contains("first", ignoreCase = true) || quest.id.contains("перв", ignoreCase = true) -> 1
                quest.id.contains("transaction", ignoreCase = true) && quest.id.contains("3", ignoreCase = true) -> 3
                else -> 1
            }
            
            QuestProgress.fromQuest(quest, progressCount, targetCount)
        }.filter { it.quest.id !in completedQuestIds || it.isCompleted }
    }

    fun onEvent(event: BariCoreEvent) {
        when (event) {
            is BariCoreEvent.OnSkillClick -> {
                viewModelScope.launch {
                    gamificationRepository.unlockSkill(event.skill)
                }
            }
            is BariCoreEvent.OnQuestClick -> {
                // Навигация обрабатывается в Screen
            }
            is BariCoreEvent.OnQuestRewardCollected -> {
                viewModelScope.launch {
                    gamificationRepository.completeQuest(event.questProgress.quest)
                }
            }
        }
    }
}