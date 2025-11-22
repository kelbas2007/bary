plugins {
    // Плагин для сборки Android-приложений
    id("com.android.application") version "8.5.0" apply false
    // Плагин для разработки на Kotlin
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    // Плагин для Hilt (внедрение зависимостей)
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
    // Плагин для KSP (обработка аннотаций для Room и др.)
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
}