package com.example.bary.ui.calendar

import android.Manifest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bary.data.model.PlannedEvent
import com.example.bary.data.model.TransactionType
import com.example.bary.ui.components.AppIcon
import com.example.bary.ui.components.AuroraGlassCard
import com.example.bary.ui.theme.AuroraMint
import com.example.bary.ui.theme.AuroraPurple
import com.example.bary.ui.theme.NeonYellow
import com.example.bary.ui.theme.AppThemeMode
import com.example.bary.ui.theme.BariTheme
import com.example.bary.ui.i18n.stringResource
import com.example.bary.ui.i18n.LocalAppLanguage
import com.example.bary.ui.i18n.StringResources
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.time.LocalDate
import kotlin.math.roundToInt
import com.example.bary.ui.calendar.CalendarDay

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val language = LocalAppLanguage.current
    val context = LocalContext.current

    // Запрос разрешений для календаря
    val calendarPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        )
    )

    LaunchedEffect(Unit) {
        if (!calendarPermissions.allPermissionsGranted) {
            calendarPermissions.launchMultiplePermissionRequest()
        } else {
            viewModel.refreshPermissions()
        }
    }

    LaunchedEffect(calendarPermissions.allPermissionsGranted) {
        if (calendarPermissions.allPermissionsGranted) {
            viewModel.refreshPermissions()
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MonthHeader(
            currentMonth = StringResources.getMonthName(uiState.selectedDate.monthValue, language) + " ${uiState.selectedDate.year}",
            onPrevMonth = { viewModel.changeMonth(-1) },
            onNextMonth = { viewModel.changeMonth(1) },
            onSyncClick = { viewModel.requestCalendarSync() },
            isSyncing = uiState.isSyncing,
            hasPermission = uiState.hasCalendarPermission
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Фильтры событий
        EventFilters(
            selectedFilter = uiState.eventFilter,
            onFilterSelected = { viewModel.setEventFilter(it) }
        )
        Spacer(modifier = Modifier.height(8.dp))

        WeekDaysRow()
        Spacer(modifier = Modifier.height(8.dp))

        // Календарь - обычная сетка (Column + Row) для работы внутри скролла
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Разбиваем дни на ряды по 7 дней
            uiState.daysInMonth.chunked(7).forEach { weekDays ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    weekDays.forEach { day ->
                        DayCell(
                            day = day,
                            isSelected = uiState.selectedDay == day.date,
                            onDayClick = { viewModel.onDayClicked(day.date) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Заполняем пустые ячейки, если в ряду меньше 7 дней
                    repeat(7 - weekDays.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Статистика выбранного дня
        uiState.selectedDayStatistics?.let { statistics ->
            DayStatisticsCard(statistics = statistics, currencySymbol = uiState.currencySymbol)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // События выбранного дня
        if (uiState.selectedDayEvents.isNotEmpty()) {
            AuroraGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource("planned_events"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Column {
                        uiState.selectedDayEvents.forEach { event ->
                            EventItem(event = event, currencySymbol = uiState.currencySymbol)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Кнопка экспорта в календарь
        if (uiState.hasCalendarPermission && uiState.events.isNotEmpty()) {
            Button(
                onClick = { viewModel.exportEventsToCalendar() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text(stringResource("export_to_calendar"))
            }
        }

        Button(
            onClick = { viewModel.calculatePrognosis() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource("calculate_prognosis"))
        }
        
        if (uiState.isSimulating) {
            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else if (uiState.prognosticBalance != null) {
            val prognosticDate = uiState.selectedDay ?: uiState.selectedDate
            Text(
                text = stringResource("prognosis_text", "day" to prognosticDate.dayOfMonth.toString(), "balance" to uiState.prognosticBalance.toString(), "currency" to uiState.currencySymbol), 
                style = MaterialTheme.typography.titleMedium, 
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Ошибка синхронизации
        uiState.syncError?.let { error ->
            Text(
                text = error,
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        // Дополнительный отступ снизу для нижнего меню
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun DayCell(
    day: CalendarDay, 
    isSelected: Boolean, 
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val isScifi = BariTheme.mode == AppThemeMode.SCIFI
    
    if (isScifi) {
        // Переливающийся бортик (Shimmer эффект)
        val shimmerTransition = rememberInfiniteTransition(label = "border_shimmer")
        val shimmerAlpha by shimmerTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "shimmer_alpha"
        )
        
        // Определяем цвет бортика в зависимости от состояния
        val borderColor = when {
            day.isToday -> BariTheme.colors.accent // Neon Yellow
            isSelected -> BariTheme.colors.tertiary // Aurora Mint
            day.hasIncome || day.hasExpense -> BariTheme.colors.tertiary
            else -> BariTheme.colors.textSecondary.copy(alpha = 0.3f)
        }
        
        val borderWidth = when {
            day.isToday -> 2.5.dp
            isSelected -> 2.dp
            else -> 1.dp
        }
        
        // SCIFI версия - прозрачные квадраты с переливающимся неоновым бортиком
        Box(
            modifier = modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = borderWidth,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            borderColor.copy(alpha = 0.4f),
                            borderColor.copy(alpha = shimmerAlpha),
                            borderColor.copy(alpha = 0.6f),
                            borderColor.copy(alpha = shimmerAlpha * 0.8f),
                            borderColor.copy(alpha = 0.4f)
                        )
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                .background(Color.Transparent) // Полностью прозрачный фон
                .clickable { onDayClick(day.date) }
        ) {
            // Подсветка дна (Underglow)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        when {
                            day.hasIncome && day.hasExpense -> Brush.horizontalGradient(
                                listOf(
                                    BariTheme.colors.tertiary.copy(alpha = 0.6f),
                                    BariTheme.colors.secondary.copy(alpha = 0.6f)
                                )
                            )
                            day.hasIncome -> Brush.horizontalGradient(
                                listOf(
                                    BariTheme.colors.tertiary.copy(alpha = 0.6f),
                                    BariTheme.colors.tertiary.copy(alpha = 0.4f)
                                )
                            )
                            day.hasExpense -> Brush.horizontalGradient(
                                listOf(
                                    BariTheme.colors.secondary.copy(alpha = 0.6f),
                                    BariTheme.colors.secondary.copy(alpha = 0.4f)
                                )
                            )
                            else -> Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.Transparent)
                            )
                        }
                    )
            )
            
            // Прицел для сегодняшнего дня (угловые квадратики)
            if (day.isToday) {
                listOf(
                    Alignment.TopStart,
                    Alignment.TopEnd,
                    Alignment.BottomStart,
                    Alignment.BottomEnd
                ).forEach { alignment ->
                    Box(
                        modifier = Modifier
                            .align(alignment)
                            .size(4.dp)
                            .background(BariTheme.colors.accent)
                    )
                }
            }
            
            // Номер дня
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = day.date.dayOfMonth.toString(),
                    color = if (day.isCurrentMonth) 
                        BariTheme.colors.textPrimary 
                    else 
                        BariTheme.colors.textSecondary.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.bodyLarge
                )
                if (day.prognosticBalanceOnDay != null && day.isCurrentMonth) {
                    Text(
                        text = day.prognosticBalanceOnDay.roundToInt().toString(),
                        color = BariTheme.colors.textSecondary.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    } else {
        // STANDARD версия
        val borderColor = when {
            isSelected -> BorderStroke(1.5.dp, Color.White)
            day.isToday -> BorderStroke(1.5.dp, NeonYellow)
            else -> null
        }
        val textColor = if (day.isCurrentMonth) Color.White else Color.White.copy(alpha = 0.4f)

        val backgroundBrush = when {
            day.hasIncome && day.hasExpense -> Brush.verticalGradient(listOf(AuroraMint, AuroraPurple))
            day.hasIncome -> Brush.verticalGradient(listOf(AuroraMint, AuroraMint.copy(alpha = 0.5f)))
            day.hasExpense -> Brush.verticalGradient(listOf(AuroraPurple, AuroraPurple.copy(alpha = 0.5f)))
            else -> null
        }

        AuroraGlassCard(
            modifier = modifier
                .aspectRatio(1f)
                .clickable { onDayClick(day.date) },
            border = borderColor
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (backgroundBrush != null) {
                    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {}
                }
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = day.date.dayOfMonth.toString(),
                        color = textColor,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (day.prognosticBalanceOnDay != null && day.isCurrentMonth) {
                        Text(
                            text = day.prognosticBalanceOnDay.roundToInt().toString(),
                            color = textColor.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthHeader(
    currentMonth: String, 
    onPrevMonth: () -> Unit, 
    onNextMonth: () -> Unit,
    onSyncClick: () -> Unit,
    isSyncing: Boolean,
    hasPermission: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrevMonth) {
            AppIcon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource("prev_month"))
        }
        Text(text = currentMonth, style = MaterialTheme.typography.headlineMedium)
        Row {
            if (hasPermission) {
                IconButton(onClick = onSyncClick, enabled = !isSyncing) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = "Sync calendar")
                    }
                }
            }
            IconButton(onClick = onNextMonth) {
                AppIcon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource("next_month"))
            }
        }
    }
}

@Composable
fun EventFilters(
    selectedFilter: EventFilter,
    onFilterSelected: (EventFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedFilter == EventFilter.ALL,
            onClick = { onFilterSelected(EventFilter.ALL) },
            label = { Text(stringResource("filter_all")) }
        )
        FilterChip(
            selected = selectedFilter == EventFilter.INCOME,
            onClick = { onFilterSelected(EventFilter.INCOME) },
            label = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(stringResource("filter_income"))
                }
            }
        )
        FilterChip(
            selected = selectedFilter == EventFilter.EXPENSE,
            onClick = { onFilterSelected(EventFilter.EXPENSE) },
            label = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(stringResource("filter_expense"))
                }
            }
        )
    }
}

@Composable
fun DayStatisticsCard(statistics: DayStatistics, currencySymbol: String) {
    AuroraGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${stringResource("statistics_for")} ${statistics.date.dayOfMonth}.${statistics.date.monthValue}.${statistics.date.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource("filter_income"),
                        style = MaterialTheme.typography.bodySmall,
                        color = AuroraMint
                    )
                    Text(
                        text = "+${statistics.totalIncome} $currencySymbol",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AuroraMint
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource("filter_expense"),
                        style = MaterialTheme.typography.bodySmall,
                        color = AuroraPurple
                    )
                    Text(
                        text = "-${statistics.totalExpense} $currencySymbol",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AuroraPurple
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource("net_amount"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${if (statistics.netAmount >= 0) "+" else ""}${statistics.netAmount} $currencySymbol",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (statistics.netAmount >= 0) AuroraMint else AuroraPurple
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${stringResource("planned_events")}: ${statistics.plannedEventsCount}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (statistics.systemEventsCount > 0) {
                    Text(
                        text = "${stringResource("system_calendar")}: ${statistics.systemEventsCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = BariTheme.colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun WeekDaysRow() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
        val days = listOf(
            stringResource("weekday_mon"),
            stringResource("weekday_tue"),
            stringResource("weekday_wed"),
            stringResource("weekday_thu"),
            stringResource("weekday_fri"),
            stringResource("weekday_sat"),
            stringResource("weekday_sun")
        )
        days.forEach { day ->
            Text(text = day, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun EventItem(event: PlannedEvent, currencySymbol: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = event.title, modifier = Modifier.weight(1f))
        Text(text = "${event.amount} $currencySymbol")
    }
}