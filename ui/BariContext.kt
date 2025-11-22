package com.example.bary.ui

import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Контекст для умного поведения Бари
 */
data class BariContext(
    val timeOfDay: TimeOfDay,
    val currentScreen: String?,
    val userBalance: Long = 0,
    val transactionsToday: Int = 0,
    val daysStreak: Int = 0,
    val lastInteractionMinutesAgo: Int = 0
)

enum class TimeOfDay {
    MORNING,    // 6:00 - 12:00
    AFTERNOON,  // 12:00 - 18:00
    EVENING,    // 18:00 - 22:00
    NIGHT;      // 22:00 - 6:00

    companion object {
        fun fromTime(time: LocalTime): TimeOfDay {
            return when (time.hour) {
                in 6..11 -> MORNING
                in 12..17 -> AFTERNOON
                in 18..21 -> EVENING
                else -> NIGHT
            }
        }

        fun current(): TimeOfDay {
            return fromTime(LocalTime.now())
        }
    }
}





