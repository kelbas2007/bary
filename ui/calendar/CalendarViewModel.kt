package com.example.bary.ui.calendar

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bary.data.model.PlannedEvent
import com.example.bary.data.model.RepeatRule
import com.example.bary.data.model.TransactionType
import com.example.bary.data.model.UserMode
import com.example.bary.repository.CalendarSyncRepository
import com.example.bary.repository.FinanceRepository
import com.example.bary.repository.SettingsRepository
import com.example.bary.repository.SystemCalendarEvent
import com.example.bary.service.ReminderService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CalendarDay(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val hasIncome: Boolean,
    val hasExpense: Boolean,
    val prognosticBalanceOnDay: Double?
)

enum class EventFilter {
    ALL,
    INCOME,
    EXPENSE
}

data class DayStatistics(
    val date: LocalDate,
    val totalIncome: Long,
    val totalExpense: Long,
    val netAmount: Long,
    val plannedEventsCount: Int,
    val systemEventsCount: Int
)

data class CalendarState(
    val prognosticBalance: Long? = null,
    val currencySymbol: String = "₽",
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedDay: LocalDate? = null,
    val daysInMonth: List<CalendarDay> = emptyList(),
    val events: List<PlannedEvent> = emptyList(),
    val selectedDayEvents: List<PlannedEvent> = emptyList(),
    val systemCalendarEvents: List<SystemCalendarEvent> = emptyList(),
    val selectedDayStatistics: DayStatistics? = null,
    val eventFilter: EventFilter = EventFilter.ALL,
    val userMode: UserMode = UserMode.EXPLORER,
    val isSimulating: Boolean = false,
    val hasCalendarPermission: Boolean = false,
    val isSyncing: Boolean = false,
    val syncError: String? = null
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val financeRepository: FinanceRepository,
    private val settingsRepository: SettingsRepository,
    private val calendarSyncRepository: CalendarSyncRepository,
    private val reminderService: ReminderService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarState())
    val uiState = _uiState.asStateFlow()

    init {
        checkCalendarPermission()
    }

    private fun checkCalendarPermission() {
        viewModelScope.launch {
            val hasPermission = calendarSyncRepository.hasCalendarPermission()
            _uiState.update { it.copy(hasCalendarPermission = hasPermission) }
        }
    }

    init {
        viewModelScope.launch {
            combine(
                financeRepository.getAllPlannedEvents(),
                financeRepository.getAllTransactions(),
                settingsRepository.userModeFlow,
                settingsRepository.currencyFlow,
                _uiState.map { it.selectedDate }.distinctUntilChanged(),
                _uiState.map { it.eventFilter }.distinctUntilChanged()
            ) { values ->
                val events = values[0] as List<PlannedEvent>
                val transactions = values[1] as List<com.example.bary.data.model.Transaction>
                val userMode = values[2] as UserMode
                val currency = values[3] as String?
                val selectedDate = values[4] as LocalDate
                val filter = values[5] as EventFilter
                
                val filteredEvents = when (filter) {
                    EventFilter.ALL -> events
                    EventFilter.INCOME -> events.filter { it.type == TransactionType.INCOME }
                    EventFilter.EXPENSE -> events.filter { it.type == TransactionType.EXPENSE }
                }
                val days = generateCalendarDays(selectedDate, filteredEvents, transactions)
                val currentState = _uiState.value
                
                // Пересчитать статистику для выбранного дня
                val statistics = if (currentState.selectedDay != null) {
                    calculateDayStatistics(currentState.selectedDay, events, transactions)
                } else {
                    null
                }
                
                currentState.copy(
                    events = filteredEvents,
                    userMode = userMode,
                    currencySymbol = currency ?: "₽",
                    selectedDate = selectedDate,
                    daysInMonth = days,
                    selectedDayStatistics = statistics
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun onDayClicked(date: LocalDate) {
        val currentState = _uiState.value
        val eventsOnDay = currentState.events.filter { isEventOnDay(it, date) }
        viewModelScope.launch {
            val transactions = financeRepository.getAllTransactions().first()
            val statistics = calculateDayStatistics(date, currentState.events, transactions)
            _uiState.update { 
                it.copy(
                    selectedDay = date, 
                    selectedDayEvents = eventsOnDay,
                    selectedDayStatistics = statistics
                )
            }
        }
    }

    fun changeMonth(direction: Int) {
        val newDate = _uiState.value.selectedDate.plusMonths(direction.toLong())
        _uiState.update { it.copy(selectedDate = newDate, selectedDay = null, prognosticBalance = null) } // Reset selection
    }

    private fun generateCalendarDays(date: LocalDate, allEvents: List<PlannedEvent>, allTransactions: List<com.example.bary.data.model.Transaction>): List<CalendarDay> {
        val yearMonth = YearMonth.from(date)
        val firstDayOfMonth = date.withDayOfMonth(1)
        val daysInMonth = yearMonth.lengthOfMonth()
        val firstDayOfWeek = (firstDayOfMonth.dayOfWeek.value - 1) % 7 // Monday is 0
        val days = mutableListOf<CalendarDay>()
        
        var currentPrognosticBalance = allTransactions
            .filter { !it.date.isAfter(LocalDate.now()) }
            .sumOf { if (it.type == TransactionType.INCOME) it.amount.toDouble() else -it.amount.toDouble() }

        // Previous month (for display only, no prognosis)
        (0 until firstDayOfWeek).forEach {
            val day = firstDayOfMonth.minusDays((firstDayOfWeek - it).toLong())
            days.add(CalendarDay(day, isCurrentMonth = false, isToday = day.isEqual(LocalDate.now()), hasIncome = false, hasExpense = false, prognosticBalanceOnDay = null))
        }

        // Current month
        (1..daysInMonth).forEach {
            val day = date.withDayOfMonth(it)
            val eventsOnDay = allEvents.filter { event -> isEventOnDay(event, day) }
            
            if (day.isAfter(LocalDate.now())) {
                 for (event in eventsOnDay) {
                    currentPrognosticBalance += if (event.type == TransactionType.INCOME) event.amount.toDouble() else -event.amount.toDouble()
                }
            }

            days.add(CalendarDay(
                date = day,
                isCurrentMonth = true,
                isToday = day.isEqual(LocalDate.now()),
                hasIncome = eventsOnDay.any { e -> e.type == TransactionType.INCOME },
                hasExpense = eventsOnDay.any { e -> e.type == TransactionType.EXPENSE },
                prognosticBalanceOnDay = if(day.isAfter(LocalDate.now().minusDays(1))) currentPrognosticBalance else null
            ))
        }

        // Next month (for display only)
        val remainingDays = 42 - days.size
        (1..remainingDays).forEach {
            val day = date.withDayOfMonth(daysInMonth).plusDays(it.toLong())
            days.add(CalendarDay(day, isCurrentMonth = false, isToday = day.isEqual(LocalDate.now()), hasIncome = false, hasExpense = false, prognosticBalanceOnDay = null))
        }
        return days
    }
    
    private fun isEventOnDay(event: PlannedEvent, day: LocalDate): Boolean {
        if (event.date.isAfter(day)) return false
        return when (event.repeatRule) {
            RepeatRule.NONE -> event.date == day
            RepeatRule.WEEKLY -> event.date.dayOfWeek == day.dayOfWeek && !event.date.isAfter(day)
            RepeatRule.MONTHLY -> event.date.dayOfMonth == day.dayOfMonth && !event.date.isAfter(day)
            RepeatRule.YEARLY -> event.date.dayOfMonth == day.dayOfMonth && event.date.month == day.month && !event.date.isAfter(day)
        }
    }

    fun calculatePrognosis() {
        val prognosticDate = _uiState.value.selectedDay ?: LocalDate.now()
        val dayData = _uiState.value.daysInMonth.find { it.date == prognosticDate }
        _uiState.update { it.copy(prognosticBalance = dayData?.prognosticBalanceOnDay?.toLong()) }
    }

    fun requestCalendarSync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncError = null) }
            try {
                val hasPermission = calendarSyncRepository.hasCalendarPermission()
                _uiState.update { 
                    it.copy(
                        hasCalendarPermission = hasPermission,
                        isSyncing = false
                    )
                }
                
                if (hasPermission) {
                    syncWithSystemCalendar()
                } else {
                    _uiState.update { 
                        it.copy(
                            syncError = "Calendar permission required",
                            isSyncing = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        syncError = e.message ?: "Sync failed",
                        isSyncing = false
                    )
                }
            }
        }
    }

    private suspend fun syncWithSystemCalendar() {
        val currentState = _uiState.value
        val startDate = currentState.selectedDate.withDayOfMonth(1)
        val endDate = currentState.selectedDate.withDayOfMonth(
            currentState.selectedDate.lengthOfMonth()
        )
        
        val systemEvents = calendarSyncRepository.readCalendarEvents(startDate, endDate)
        _uiState.update { 
            it.copy(
                systemCalendarEvents = systemEvents,
                isSyncing = false,
                syncError = null
            )
        }
    }

    fun addEventToSystemCalendar(plannedEvent: PlannedEvent) {
        viewModelScope.launch {
            try {
                val systemEvent = SystemCalendarEvent(
                    title = plannedEvent.title,
                    startTime = plannedEvent.date,
                    endTime = plannedEvent.date,
                    description = "${plannedEvent.type.name}: ${plannedEvent.amount} ${_uiState.value.currencySymbol}"
                )
                calendarSyncRepository.addEventToCalendar(systemEvent)
                
                // Создать напоминание
                reminderService.scheduleReminder(
                    context = context,
                    event = plannedEvent,
                    reminderMinutesBefore = 60
                )
                
                requestCalendarSync() // Обновить список событий
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(syncError = "Failed to add event: ${e.message}")
                }
            }
        }
    }

    fun setEventFilter(filter: EventFilter) {
        _uiState.update { it.copy(eventFilter = filter) }
    }

    private fun calculateDayStatistics(
        date: LocalDate,
        allEvents: List<PlannedEvent>,
        allTransactions: List<com.example.bary.data.model.Transaction>
    ): DayStatistics {
        val eventsOnDay = allEvents.filter { isEventOnDay(it, date) }
        val transactionsOnDay = allTransactions.filter { it.date == date }
        
        val totalIncome = (eventsOnDay.filter { it.type == TransactionType.INCOME }.sumOf { it.amount } +
            transactionsOnDay.filter { it.type == TransactionType.INCOME }.sumOf { it.amount })
        
        val totalExpense = (eventsOnDay.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount } +
            transactionsOnDay.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount })
        
        val netAmount = totalIncome - totalExpense
        
        val systemEventsOnDay = _uiState.value.systemCalendarEvents.filter { 
            it.startTime == date || (it.startTime.isBefore(date) && it.endTime.isAfter(date))
        }
        
        return DayStatistics(
            date = date,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netAmount = netAmount,
            plannedEventsCount = eventsOnDay.size,
            systemEventsCount = systemEventsOnDay.size
        )
    }

    fun exportEventsToCalendar() {
        viewModelScope.launch {
            try {
                _uiState.value.events.forEach { event ->
                    addEventToSystemCalendar(event)
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(syncError = "Export failed: ${e.message}")
                }
            }
        }
    }

    fun refreshPermissions() {
        checkCalendarPermission()
    }
}