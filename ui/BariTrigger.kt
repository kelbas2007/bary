package com.example.bary.ui

/**
 * Sealed interface для всех триггеров, которые могут активировать реакцию Бари.
 * Это единый API для всех ViewModel для общения с Бари.
 */
sealed interface BariTrigger {
    // Жизненный цикл и Навигация
    data object OnAppStart : BariTrigger
    data class OnScreenChanged(val route: String) : BariTrigger
    
    // Финансы
    data object OnTransactionAdded : BariTrigger
    data class OnCategoryOverspent(val categoryId: String, val amount: Double) : BariTrigger
    data object OnBalanceLow : BariTrigger // Баланс упал ниже X
    data class OnPiggyBankCreated(val goalName: String) : BariTrigger
    data object OnPiggyBankFilled : BariTrigger // Копилка заполнена

    // Геймификация
    data class OnSkillUnlocked(val skillId: String) : BariTrigger
    data class OnQuestCompleted(val questId: String) : BariTrigger
    data class OnLevelUp(val newLevel: Int) : BariTrigger
    data class OnAchievementUnlocked(val achievementName: String) : BariTrigger
    
    // Обучение
    data class OnLessonCompleted(val lessonId: String) : BariTrigger
    data class OnQuizCompleted(val quizId: String, val score: Int) : BariTrigger
    
    // Интерактивный тур
    data class TutorialStep(val stepData: com.example.bary.bari.TutorialStepData) : BariTrigger
    
    // Бизнес-каюта
    data class OnBariAdvice(val advice: String) : BariTrigger
}


