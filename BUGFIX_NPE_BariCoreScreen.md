# ИСПРАВЛЕНИЕ: NullPointerException в BariCoreScreen

**Дата:** 2024  
**Проблема:** `NullPointerException: Attempt to invoke interface method 'boolean java.util.List.isEmpty()' on a null object reference`

---

## 🔴 ПРОБЛЕМА

Приложение падало с `NullPointerException` при открытии экрана `BariCoreScreen` (дерево навыков).

**Стек ошибки:**
```
at com.example.bary.ui.screens.BariCoreScreenKt.getSkillLevel(BariCoreScreen.kt:206)
```

**Причина:** Поле `dependencies` в модели `Skill` могло быть `null` при десериализации из JSON через Gson, даже если в data class указано значение по умолчанию `emptyList()`.

---

## ✅ РЕШЕНИЕ

### 1. Обновлена модель Skill
**Файл:** `app/src/main/java/com/example/bary/data/model/Skill.kt`

- Поле `dependencies` сделано nullable: `List<String>? = null`
- Добавлено helper-свойство `safeDependencies` для безопасного доступа

```kotlin
data class Skill(
    val id: String,
    val title: String,
    val description: String,
    val cost: Int,
    val dependencies: List<String>? = null // Nullable для корректной десериализации из JSON
) {
    // Helper property для безопасного доступа
    val safeDependencies: List<String>
        get() = dependencies ?: emptyList()
}
```

### 2. Обновлен BariCoreScreen
**Файл:** `app/src/main/java/com/example/bary/ui/screens/BariCoreScreen.kt`

Заменены все прямые обращения к `skill.dependencies` на `skill.safeDependencies`:

- В функции `getSkillLevel()` (строка 207)
- В Canvas при отрисовке линий (строка 89)
- При проверке разблокированных зависимостей (строка 114)

---

## 📝 ИЗМЕНЕННЫЕ ФАЙЛЫ

1. `app/src/main/java/com/example/bary/data/model/Skill.kt` - добавлен `safeDependencies`
2. `app/src/main/java/com/example/bary/ui/screens/BariCoreScreen.kt` - заменены все использования на `safeDependencies`

---

## ✅ РЕЗУЛЬТАТ

- ✅ Ошибка исправлена
- ✅ Приложение компилируется без ошибок
- ✅ Null-safety обеспечена во всех местах использования `dependencies`

**Статус:** Исправлено и протестировано





