package com.example.bary.ui

/**
 * Позиция Бари на экране.
 */
sealed class BariPosition {
    data object BottomEnd : BariPosition()      // Внизу справа (по умолчанию)
    data object BottomStart : BariPosition()   // Внизу слева
    data object CenterScreen : BariPosition()  // По центру экрана
    data object Hidden : BariPosition()        // Скрыт
    data class Custom(val offsetX: Float, val offsetY: Float) : BariPosition() // Кастомная позиция
}





