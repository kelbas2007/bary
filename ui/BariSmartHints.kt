package com.example.bary.ui

import com.example.bary.data.local.datastore.AppLanguage
import com.example.bary.data.model.UserMode
import com.example.bary.ui.i18n.StringResources

/**
 * Умные подсказки Бари в зависимости от экрана и контекста
 */
object BariSmartHints {
    
    fun getScreenHint(
        route: String?,
        userMode: UserMode,
        visitCount: Int,
        timeOfDay: TimeOfDay,
        language: AppLanguage = AppLanguage.RUSSIAN
    ): String {
        // Если пользователь часто посещает экран
        if (visitCount > 5 && visitCount % 5 == 0) {
            val loyaltyMessages = listOf(
                StringResources.getString("bari_loyalty_1", language).replace("{count}", visitCount.toString()),
                StringResources.getString("bari_loyalty_2", language),
                StringResources.getString("bari_loyalty_3", language).replace("{count}", visitCount.toString())
            )
            return loyaltyMessages.random()
        }
        
        return when (route) {
            "balance", "home" -> getBalanceHint(userMode, timeOfDay, language)
            "piggy_bank", "piggy_banks" -> getPiggyBankHint(userMode, visitCount, language)
            "calendar" -> getCalendarHint(userMode, timeOfDay, language)
            "stats" -> getStatsHint(userMode, language)
            "bari_core" -> getBariCoreHint(userMode, visitCount, language)
            "lessons" -> getLessonsHint(userMode, language)
            "settings" -> getSettingsHint(language)
            else -> getDefaultHint(userMode, timeOfDay, language)
        }
    }
    
    private fun getBalanceHint(userMode: UserMode, timeOfDay: TimeOfDay, language: AppLanguage): String {
        val hints = if (userMode == UserMode.EXPLORER) {
            when (timeOfDay) {
                TimeOfDay.MORNING -> listOf(
                    StringResources.getString("bari_balance_morning_1", language),
                    StringResources.getString("bari_balance_morning_2", language),
                    StringResources.getString("bari_balance_morning_3", language)
                )
                TimeOfDay.AFTERNOON -> listOf(
                    StringResources.getString("bari_balance_afternoon_1", language),
                    StringResources.getString("bari_balance_afternoon_2", language),
                    StringResources.getString("bari_balance_afternoon_3", language)
                )
                TimeOfDay.EVENING -> listOf(
                    StringResources.getString("bari_balance_evening_1", language),
                    StringResources.getString("bari_balance_evening_2", language),
                    StringResources.getString("bari_balance_evening_3", language)
                )
                TimeOfDay.NIGHT -> listOf(
                    StringResources.getString("bari_balance_night_1", language),
                    StringResources.getString("bari_balance_night_2", language)
                )
            }
        } else {
            listOf(
                StringResources.getString("bari_balance_professional_1", language),
                StringResources.getString("bari_balance_professional_2", language),
                StringResources.getString("bari_balance_professional_3", language),
                StringResources.getString("bari_balance_professional_4", language)
            )
        }
        return hints.random()
    }
    
    private fun getPiggyBankHint(userMode: UserMode, visitCount: Int, language: AppLanguage): String {
        val jokes = listOf(
            StringResources.getString("bari_piggy_joke_1", language),
            StringResources.getString("bari_piggy_joke_2", language),
            StringResources.getString("bari_piggy_joke_3", language)
        )
        
        if (visitCount > 3 && visitCount % 3 == 0) {
            return jokes.random()
        }
        
        val hints = if (userMode == UserMode.EXPLORER) {
            listOf(
                StringResources.getString("bari_piggy_explorer_1", language),
                StringResources.getString("bari_piggy_explorer_2", language),
                StringResources.getString("bari_piggy_explorer_3", language),
                StringResources.getString("bari_piggy_explorer_4", language)
            )
        } else {
            listOf(
                StringResources.getString("bari_piggy_professional_1", language),
                StringResources.getString("bari_piggy_professional_2", language),
                StringResources.getString("bari_piggy_professional_3", language),
                StringResources.getString("bari_piggy_professional_4", language)
            )
        }
        return hints.random()
    }
    
    private fun getCalendarHint(userMode: UserMode, timeOfDay: TimeOfDay, language: AppLanguage): String {
        val hints = if (userMode == UserMode.EXPLORER) {
            listOf(
                StringResources.getString("bari_calendar_explorer_1", language),
                StringResources.getString("bari_calendar_explorer_2", language),
                StringResources.getString("bari_calendar_explorer_3", language),
                StringResources.getString("bari_calendar_explorer_4", language)
            )
        } else {
            listOf(
                StringResources.getString("bari_calendar_professional_1", language),
                StringResources.getString("bari_calendar_professional_2", language),
                StringResources.getString("bari_calendar_professional_3", language),
                StringResources.getString("bari_calendar_professional_4", language)
            )
        }
        return hints.random()
    }
    
    private fun getStatsHint(userMode: UserMode, language: AppLanguage): String {
        val hints = if (userMode == UserMode.EXPLORER) {
            listOf(
                StringResources.getString("bari_stats_explorer_1", language),
                StringResources.getString("bari_stats_explorer_2", language),
                StringResources.getString("bari_stats_explorer_3", language),
                StringResources.getString("bari_stats_explorer_4", language)
            )
        } else {
            listOf(
                StringResources.getString("bari_stats_professional_1", language),
                StringResources.getString("bari_stats_professional_2", language),
                StringResources.getString("bari_stats_professional_3", language),
                StringResources.getString("bari_stats_professional_4", language)
            )
        }
        return hints.random()
    }
    
    private fun getBariCoreHint(userMode: UserMode, visitCount: Int, language: AppLanguage): String {
        if (visitCount == 1) {
            return if (userMode == UserMode.EXPLORER) {
                StringResources.getString("bari_core_welcome_explorer", language)
            } else {
                StringResources.getString("bari_core_welcome_professional", language)
            }
        }
        
        val hints = if (userMode == UserMode.EXPLORER) {
            listOf(
                StringResources.getString("bari_core_explorer_1", language),
                StringResources.getString("bari_core_explorer_2", language),
                StringResources.getString("bari_core_explorer_3", language),
                StringResources.getString("bari_core_explorer_4", language)
            )
        } else {
            listOf(
                StringResources.getString("bari_core_professional_1", language),
                StringResources.getString("bari_core_professional_2", language),
                StringResources.getString("bari_core_professional_3", language),
                StringResources.getString("bari_core_professional_4", language)
            )
        }
        return hints.random()
    }
    
    private fun getLessonsHint(userMode: UserMode, language: AppLanguage): String {
        val hints = if (userMode == UserMode.EXPLORER) {
            listOf(
                StringResources.getString("bari_lessons_explorer_1", language),
                StringResources.getString("bari_lessons_explorer_2", language),
                StringResources.getString("bari_lessons_explorer_3", language),
                StringResources.getString("bari_lessons_explorer_4", language)
            )
        } else {
            listOf(
                StringResources.getString("bari_lessons_professional_1", language),
                StringResources.getString("bari_lessons_professional_2", language),
                StringResources.getString("bari_lessons_professional_3", language),
                StringResources.getString("bari_lessons_professional_4", language)
            )
        }
        return hints.random()
    }
    
    private fun getSettingsHint(language: AppLanguage): String {
        val hints = listOf(
            StringResources.getString("bari_settings_1", language),
            StringResources.getString("bari_settings_2", language),
            StringResources.getString("bari_settings_3", language),
            StringResources.getString("bari_settings_4", language)
        )
        return hints.random()
    }
    
    private fun getDefaultHint(userMode: UserMode, timeOfDay: TimeOfDay, language: AppLanguage): String {
        val hints = if (userMode == UserMode.EXPLORER) {
            when (timeOfDay) {
                TimeOfDay.MORNING -> listOf(
                    StringResources.getString("bari_default_morning_1", language),
                    StringResources.getString("bari_default_morning_2", language),
                    StringResources.getString("bari_default_morning_3", language)
                )
                TimeOfDay.AFTERNOON -> listOf(
                    StringResources.getString("bari_default_afternoon_1", language),
                    StringResources.getString("bari_default_afternoon_2", language),
                    StringResources.getString("bari_default_afternoon_3", language)
                )
                TimeOfDay.EVENING -> listOf(
                    StringResources.getString("bari_default_evening_1", language),
                    StringResources.getString("bari_default_evening_2", language),
                    StringResources.getString("bari_default_evening_3", language)
                )
                TimeOfDay.NIGHT -> listOf(
                    StringResources.getString("bari_default_night_1", language),
                    StringResources.getString("bari_default_night_2", language),
                    StringResources.getString("bari_default_night_3", language)
                )
            }
        } else {
            listOf(
                StringResources.getString("bari_default_professional_1", language),
                StringResources.getString("bari_default_professional_2", language),
                StringResources.getString("bari_default_professional_3", language),
                StringResources.getString("bari_default_professional_4", language)
            )
        }
        return hints.random()
    }
}


