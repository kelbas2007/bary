package com.example.bary.ui

/**
 * Ресурс для отображения Бари (статичное изображение).
 */
sealed class BariAsset {
    data class Image(val drawableResId: Int) : BariAsset()
}





