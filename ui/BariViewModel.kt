package com.example.bary.ui

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bary.R
import com.example.bary.data.model.GameEvent
import com.example.bary.data.model.UserMode
import com.example.bary.domain.usecases.CheckAchievementsUseCase
import com.example.bary.domain.usecases.GetBariHintUseCase
import android.content.Context
import com.example.bary.repository.ContentRepository
import com.example.bary.repository.GamificationRepository
import com.example.bary.repository.SettingsRepository
import com.example.bary.service.ReminderService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

data class BariUiState(
    val isVisible: Boolean = true,
    val currentHint: String? = null,
    val asset: BariAsset = BariAsset.Image(R.drawable.bary_static),
    val position: BariPosition = BariPosition.BottomEnd,
    val mood: BariMood = BariMood.NEUTRAL,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val currentRoute: String? = null // Текущий экран для отслеживания позиций
)

sealed class BariEvent {
    data class OnDrag(val dragAmount: Offset) : BariEvent()
}

@HiltViewModel
class BariViewModel @Inject constructor(
    private val gamificationRepository: GamificationRepository,
    private val settingsRepository: SettingsRepository,
    private val contentRepository: ContentRepository,
    private val checkAchievementsUseCase: CheckAchievementsUseCase,
    private val getBariHintUseCase: GetBariHintUseCase,
    private val bariEventBus: BariEventBus,
    private val reminderService: ReminderService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(BariUiState())
    val uiState = _uiState.asStateFlow()

    private var lastTriggerTime = System.currentTimeMillis()
    private var screenVisitCount = mutableMapOf<String, Int>()
    // Храним позиции Бари для каждого экрана (route -> Pair(offsetX, offsetY))
    private val screenPositions = mutableMapOf<String, Pair<Float, Float>>()

    init {
        listenToGameEvents()
        listenToTriggers()
        startSmartBehavior()
        scheduleDailyReminders()
    }

    private fun startSmartBehavior() {
        viewModelScope.launch {
            // Периодически проверяем контекст и даем умные подсказки
            // Увеличен интервал для лучшей производительности
            while (true) {
                delay(180_000) // Каждые 3 минуты вместо 1 минуты
                checkAndGiveSmartHint()
            }
        }
    }

    private suspend fun checkAndGiveSmartHint() {
        val minutesSinceLastTrigger = (System.currentTimeMillis() - lastTriggerTime) / 60_000
        
        // Если прошло больше 5 минут с последнего взаимодействия
        if (minutesSinceLastTrigger > 5) {
            // Выполняем тяжелые операции в фоне
            val timeOfDay = withContext(Dispatchers.Default) { TimeOfDay.current() }
            val userMode = settingsRepository.userModeFlow.first()
            val hint = withContext(Dispatchers.Default) {
                getSmartHintBasedOnContext(timeOfDay, userMode)
            }
            
            // Обновляем UI на главном потоке
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        currentHint = hint,
                        mood = BariMood.HAPPY,
                        asset = BariAsset.Image(R.drawable.bary_static)
                    )
                }
                delay(7000)
                _uiState.update { it.copy(currentHint = null) }
            }
        }
    }

    private suspend fun getSmartHintBasedOnContext(timeOfDay: TimeOfDay, userMode: UserMode): String {
        val language = settingsRepository.appLanguageFlow.first()
        // Используем дефолтные подсказки из BariSmartHints
        val route = when (timeOfDay) {
            TimeOfDay.MORNING -> "balance"
            TimeOfDay.AFTERNOON -> "balance"
            TimeOfDay.EVENING -> "balance"
            TimeOfDay.NIGHT -> "balance"
        }
        val visitCount = 1
        return BariSmartHints.getScreenHint(route, userMode, visitCount, timeOfDay, language)
    }

    private fun listenToTriggers() {
        bariEventBus.triggers
            .onEach { trigger -> handleTrigger(trigger) }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: BariEvent) {
        when (event) {
            is BariEvent.OnDrag -> {
                _uiState.update { currentState ->
                    val currentRoute = currentState.currentRoute ?: "default"
                    val newOffsetX = currentState.offsetX + event.dragAmount.x
                    val newOffsetY = currentState.offsetY + event.dragAmount.y
                    
                    // Сохраняем позицию для текущего экрана
                    screenPositions[currentRoute] = Pair(newOffsetX, newOffsetY)
                    
                    currentState.copy(
                        offsetX = newOffsetX,
                        offsetY = newOffsetY,
                        position = BariPosition.Custom(newOffsetX, newOffsetY)
                    )
                }
            }
        }
    }

    /**
     * Главный метод для обработки триггеров - "мозг" Бари.
     * Принимает решение на основе контекста: UserMode, настроения, разблокированных навыков.
     */
    fun handleTrigger(trigger: BariTrigger) {
        lastTriggerTime = System.currentTimeMillis() // Обновляем время последнего взаимодействия
        
        viewModelScope.launch(Dispatchers.IO) {
            val userMode = settingsRepository.userModeFlow.first()
            val userProgress = gamificationRepository.getUserProgress().first()
            val currentState = _uiState.value
            val unlockedSkills = userProgress?.skills?.keys ?: emptySet<String>()
            val timeOfDay = TimeOfDay.current()

            data class BariReaction(
                val hint: String,
                val asset: BariAsset,
                val mood: BariMood,
                val position: BariPosition
            )

                val reaction = when (trigger) {
                    // Жизненный цикл и Навигация
                    is BariTrigger.OnAppStart -> {
                        val language = settingsRepository.appLanguageFlow.first()
                        val greeting = when (timeOfDay) {
                            TimeOfDay.MORNING -> if (userMode == UserMode.EXPLORER) {
                                com.example.bary.ui.i18n.StringResources.getString("bari_app_start_morning_explorer", language)
                            } else {
                                com.example.bary.ui.i18n.StringResources.getString("bari_app_start_morning_professional", language)
                            }
                            TimeOfDay.AFTERNOON -> if (userMode == UserMode.EXPLORER) {
                                com.example.bary.ui.i18n.StringResources.getString("bari_app_start_afternoon_explorer", language)
                            } else {
                                com.example.bary.ui.i18n.StringResources.getString("bari_app_start_afternoon_professional", language)
                            }
                            TimeOfDay.EVENING -> if (userMode == UserMode.EXPLORER) {
                                com.example.bary.ui.i18n.StringResources.getString("bari_app_start_evening_explorer", language)
                            } else {
                                com.example.bary.ui.i18n.StringResources.getString("bari_app_start_evening_professional", language)
                            }
                            TimeOfDay.NIGHT -> if (userMode == UserMode.EXPLORER) {
                                com.example.bary.ui.i18n.StringResources.getString("bari_app_start_night_explorer", language)
                            } else {
                                com.example.bary.ui.i18n.StringResources.getString("bari_app_start_night_professional", language)
                            }
                        }
                        
                        BariReaction(
                            hint = greeting,
                            asset = BariAsset.Image(R.drawable.bary_static),
                            mood = BariMood.HAPPY,
                            position = BariPosition.BottomEnd
                        )
                    }

                    is BariTrigger.OnScreenChanged -> {
                        // Увеличиваем счетчик посещений экрана
                        trigger.route?.let { route ->
                            screenVisitCount[route] = (screenVisitCount[route] ?: 0) + 1
                        }
                        
                        val visitCount = trigger.route?.let { screenVisitCount[it] } ?: 1
                        val hint = getSmartScreenHint(trigger.route, userMode, visitCount, timeOfDay)
                        val image = when (trigger.route) {
                            "piggy_bank", "piggy_banks" -> R.drawable.bari_pose_piggybank
                            "calendar" -> R.drawable.bari_pose_calendar
                            else -> R.drawable.bary_static
                        }
                        BariReaction(
                            hint = hint,
                            asset = BariAsset.Image(image),
                            mood = if (visitCount > 5) BariMood.EXCITED else currentState.mood,
                            position = BariPosition.BottomEnd
                        )
                    }

                    // Финансы
                    is BariTrigger.OnTransactionAdded -> {
                        val language = settingsRepository.appLanguageFlow.first()
                        val encouragements = listOf(
                            com.example.bary.ui.i18n.StringResources.getString("bari_transaction_added_1", language),
                            com.example.bary.ui.i18n.StringResources.getString("bari_transaction_added_2", language),
                            com.example.bary.ui.i18n.StringResources.getString("bari_transaction_added_3", language),
                            com.example.bary.ui.i18n.StringResources.getString("bari_transaction_added_4", language),
                            com.example.bary.ui.i18n.StringResources.getString("bari_transaction_added_5", language)
                        )
                        
                        BariReaction(
                            hint = encouragements.random(),
                            asset = BariAsset.Image(R.drawable.bary_static),
                            mood = BariMood.HAPPY,
                            position = currentState.position
                        )
                    }

                is BariTrigger.OnCategoryOverspent -> {
                    // Проверяем, разблокирован ли навык "budgeting"
                    if (!unlockedSkills.contains("budgeting")) {
                        // Навык заблокирован - инициируем квест
                        gamificationRepository.issueQuest("Q_BUDGETING_LESSON")
                        val dialogue = contentRepository.getDialogueByKey("bal_002", userMode)
                        BariReaction(
                            hint = dialogue.ifEmpty { "Капитан, я вижу утечку... Нам нужно найти 'Чертеж Бюджетирования'!" },
                            asset = BariAsset.Image(R.drawable.bary_static), // Используем существующий ресурс
                            mood = BariMood.WORRIED,
                            position = currentState.position
                        )
                    } else {
                        // Навык открыт - даем аналитический совет
                        val dialogue = contentRepository.getDialogueByKey("bal_003", userMode)
                        BariReaction(
                            hint = dialogue.ifEmpty { "Анализ схем показывает, что сектор '${trigger.categoryId}' потребляет слишком много..." },
                            asset = BariAsset.Image(R.drawable.bary_static),
                            mood = BariMood.NEUTRAL,
                            position = currentState.position
                        )
                    }
                }

                is BariTrigger.OnBalanceLow -> {
                    val dialogue = contentRepository.getDialogueByKey("hint_balance_low", userMode)
                    BariReaction(
                        hint = dialogue.ifEmpty { "Осторожно, баланс низкий!" },
                        asset = BariAsset.Image(R.drawable.bary_static), // Используем существующий ресурс
                        mood = BariMood.WORRIED,
                        position = currentState.position
                    )
                }

                is BariTrigger.OnPiggyBankCreated -> {
                    val dialogue = contentRepository.getDialogueByKey("piggy_001", userMode)
                    BariReaction(
                        hint = dialogue.ifEmpty { "Отличная цель: ${trigger.goalName}! Давай начнем копить!" },
                        asset = BariAsset.Image(R.drawable.bari_pose_piggybank),
                        mood = BariMood.EXCITED,
                        position = currentState.position
                    )
                }

                is BariTrigger.OnPiggyBankFilled -> {
                    val dialogue = contentRepository.getDialogueByKey("piggy_002", userMode)
                    BariReaction(
                        hint = dialogue.ifEmpty { "Поздравляю! Копилка заполнена!" },
                        asset = BariAsset.Image(R.drawable.bary),
                        mood = BariMood.EXCITED,
                        position = BariPosition.CenterScreen
                    )
                }

                // Геймификация
                is BariTrigger.OnSkillUnlocked -> {
                    val dialogue = contentRepository.getDialogueByKey("skill_001", userMode)
                    BariReaction(
                        hint = dialogue.ifEmpty { "Новая схема ядра разблокирована: ${trigger.skillId}!" },
                        asset = BariAsset.Image(R.drawable.bary),
                        mood = BariMood.EXCITED,
                        position = BariPosition.CenterScreen
                    )
                }

                is BariTrigger.OnQuestCompleted -> {
                    val dialogue = contentRepository.getDialogueByKey("quest_001", userMode)
                    BariReaction(
                        hint = dialogue.ifEmpty { "Миссия выполнена! Отличная работа!" },
                        asset = BariAsset.Image(R.drawable.bary),
                        mood = BariMood.EXCITED,
                        position = currentState.position
                    )
                }

                is BariTrigger.OnLevelUp -> {
                    val dialogue = contentRepository.getDialogueByKey("level_001", userMode)
                    BariReaction(
                        hint = dialogue.ifEmpty { "ЕСТЬ! Целостность Ядра: ${trigger.newLevel}%!" },
                        asset = BariAsset.Image(R.drawable.bary),
                        mood = BariMood.EXCITED,
                        position = BariPosition.CenterScreen
                    )
                }

                is BariTrigger.OnAchievementUnlocked -> {
                    val dialogue = contentRepository.getDialogueByKey("achievement_001", userMode)
                    BariReaction(
                        hint = dialogue.ifEmpty { "Круто! Новое достижение: ${trigger.achievementName}!" },
                        asset = BariAsset.Image(R.drawable.bary),
                        mood = BariMood.EXCITED,
                        position = currentState.position
                    )
                }

                // Обучение
                is BariTrigger.OnLessonCompleted -> {
                    val dialogue = contentRepository.getDialogueByKey("lesson_001", userMode)
                    BariReaction(
                        hint = dialogue.ifEmpty { "Чертеж дешифрован! Ты узнал что-то новое!" },
                        asset = BariAsset.Image(R.drawable.bary_static),
                        mood = BariMood.HAPPY,
                        position = currentState.position
                    )
                }

                is BariTrigger.OnQuizCompleted -> {
                    val dialogue = if (trigger.score >= 80) {
                        contentRepository.getDialogueByKey("quiz_good", userMode)
                    } else {
                        contentRepository.getDialogueByKey("quiz_ok", userMode)
                    }
                    BariReaction(
                        hint = dialogue.ifEmpty { "Тест пройден! Результат: ${trigger.score}%" },
                        asset = BariAsset.Image(R.drawable.bary_static),
                        mood = if (trigger.score >= 80) BariMood.EXCITED else BariMood.NEUTRAL,
                        position = currentState.position
                    )
                }
                
                // Интерактивный тур
                is BariTrigger.TutorialStep -> {
                    BariReaction(
                        hint = trigger.stepData.message,
                        asset = BariAsset.Image(R.drawable.bary_static),
                        mood = BariMood.HAPPY,
                        position = currentState.position
                    )
                }
                
                // Бизнес-каюта - совет Бари
                is BariTrigger.OnBariAdvice -> {
                    BariReaction(
                        hint = trigger.advice,
                        asset = BariAsset.Image(R.drawable.bary_static),
                        mood = BariMood.NEUTRAL,
                        position = currentState.position
                    )
                }
            }

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        currentHint = reaction.hint,
                        asset = reaction.asset,
                        mood = reaction.mood,
                        position = reaction.position
                    )
                }

                // Автоматически скрываем подсказку через 5 секунд (кроме CenterScreen)
                if (reaction.position !is BariPosition.CenterScreen) {
                    delay(5000)
                    _uiState.update { it.copy(currentHint = null) }
                }
            }
        }
    }

    private fun listenToGameEvents() {
        viewModelScope.launch {
            gamificationRepository.gameEvents.collect { event ->
                when (event) {
                    is GameEvent.LevelUp -> {
                        handleTrigger(BariTrigger.OnLevelUp(event.newLevel))
                    }
                    is GameEvent.AchievementUnlocked -> {
                        handleTrigger(BariTrigger.OnAchievementUnlocked(event.achievementName))
                    }
                    is GameEvent.QuestCompleted -> {
                        handleTrigger(BariTrigger.OnQuestCompleted(event.questId))
                    }
                    is GameEvent.SkillUnlocked -> {
                        handleTrigger(BariTrigger.OnSkillUnlocked(event.skillId))
                    }
                    is GameEvent.LessonCompleted -> {
                        handleTrigger(BariTrigger.OnLessonCompleted(event.lessonId))
                    }
                    is GameEvent.QuizCompleted -> {
                        handleTrigger(BariTrigger.OnQuizCompleted(event.quizId, event.score))
                    }
                    // Финансовые события (TransactionAdded, PiggyBankCreated, PiggyBankFilled)
                    // обрабатываются напрямую через BariTrigger, не через GameEvent
                }
            }
        }
    }

    fun checkAchievements() {
        viewModelScope.launch(Dispatchers.IO) {
            checkAchievementsUseCase()
        }
    }

    private suspend fun getSmartScreenHint(
        route: String?,
        userMode: UserMode,
        visitCount: Int,
        timeOfDay: TimeOfDay
    ): String {
        val language = settingsRepository.appLanguageFlow.first()
        return BariSmartHints.getScreenHint(route ?: "default", userMode, visitCount, timeOfDay, language)
    }

    fun onNavigate(route: String?, userMode: UserMode) {
        val routeKey = route ?: "default"
        
        // Сохраняем текущую позицию перед переходом на другой экран
        val currentState = _uiState.value
        currentState.currentRoute?.let { currentRoute ->
            screenPositions[currentRoute] = Pair(currentState.offsetX, currentState.offsetY)
        }
        
        // Загружаем сохраненную позицию для нового экрана или используем позицию по умолчанию
        val savedPosition = screenPositions[routeKey]
        val (offsetX, offsetY) = savedPosition ?: Pair(0f, 0f)
        
        // Обновляем состояние с позицией для нового экрана
        _uiState.update { state ->
            state.copy(
                currentRoute = routeKey,
                offsetX = offsetX,
                offsetY = offsetY,
                position = if (offsetX == 0f && offsetY == 0f) {
                    BariPosition.BottomEnd
                } else {
                    BariPosition.Custom(offsetX, offsetY)
                }
            )
        }
        
        handleTrigger(BariTrigger.OnScreenChanged(route ?: ""))
    }

    /**
     * Планирует ежедневные напоминания от Бари
     */
    private fun scheduleDailyReminders() {
        viewModelScope.launch {
            val language = settingsRepository.appLanguageFlow.first()
            val tomorrow = LocalDateTime.now().plusDays(1)
            
            // Напоминание проверить баланс утром (9:00) - планируем на завтра
            val morningReminderTime = tomorrow.toLocalDate().atTime(9, 0)
            scheduleBariReminder(
                reminderId = "daily_balance_check",
                title = "Бари",
                message = "Капитан, пора проверить баланс! 💰",
                reminderTime = morningReminderTime
            )
            
            // Напоминание добавить транзакцию вечером (20:00) - планируем на завтра
            val eveningReminderTime = tomorrow.toLocalDate().atTime(20, 0)
            scheduleBariReminder(
                reminderId = "daily_transaction_reminder",
                title = "Бари",
                message = "Не забудь добавить транзакции за сегодня! 📊",
                reminderTime = eveningReminderTime
            )
        }
    }

    /**
     * Планирует напоминание от Бари
     */
    fun scheduleBariReminder(
        reminderId: String,
        title: String,
        message: String,
        reminderTime: LocalDateTime
    ) {
        // Проверяем, что время еще не прошло
        if (reminderTime.isBefore(LocalDateTime.now())) {
            return
        }
        
        reminderService.scheduleBariReminder(
            context = context,
            reminderId = reminderId,
            title = title,
            message = message,
            reminderTime = reminderTime
        )
    }
}
