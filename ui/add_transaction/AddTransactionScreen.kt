package com.example.bary.ui.add_transaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.bary.data.model.Category
import com.example.bary.data.model.RepeatRule
import com.example.bary.data.model.Template
import com.example.bary.data.model.TransactionType
import com.example.bary.ui.components.AppIcon
import com.example.bary.ui.components.AuroraGlassCard
import com.example.bary.ui.i18n.stringResource
import com.example.bary.ui.i18n.LocalAppLanguage
import com.example.bary.ui.theme.AppThemeMode
import com.example.bary.ui.theme.BariTheme
import com.example.bary.ui.theme.AuroraMint
import com.example.bary.ui.theme.AuroraPurple
import com.example.bary.ui.theme.NeonYellow
import com.example.bary.ui.theme.AuroraBlue
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    navController: NavController,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    if (uiState.isSaved) {
        LaunchedEffect(uiState.isSaved) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (uiState.isEditing) stringResource("edit_transaction") else stringResource("new_transaction")) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { AppIcon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource("back_nav")) } }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (uiState.suggestedTemplates.isNotEmpty()) {
                Text(stringResource("templates"), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.suggestedTemplates) { template ->
                        TemplateItem(template = template, onClick = { viewModel.onEvent(AddTransactionEvent.OnTemplateSelected(template)) })
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // SCIFI или STANDARD версия поля ввода суммы
            val isScifi = BariTheme.mode == AppThemeMode.SCIFI
            if (isScifi) {
                SciFiAmountInput(
                    value = uiState.amount,
                    onValueChange = { viewModel.onEvent(AddTransactionEvent.OnAmountChange(it)) },
                    isIncome = uiState.type == TransactionType.INCOME,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                OutlinedTextField(
                    value = uiState.amount,
                    onValueChange = { viewModel.onEvent(AddTransactionEvent.OnAmountChange(it)) },
                    label = { Text(stringResource("amount")) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            Text(stringResource("category"), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 90.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(220.dp) 
            ) {
                items(uiState.availableCategories) { category ->
                    CategoryItemView(
                        category = category,
                        icon = mapCategoryNameToIcon(category.name),
                        isSelected = category.id == uiState.selectedCategory?.id,
                        onClick = { viewModel.onEvent(AddTransactionEvent.OnCategoryChange(category)) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = uiState.note,
                onValueChange = { viewModel.onEvent(AddTransactionEvent.OnNoteChange(it)) },
                label = { Text(stringResource("note_optional")) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource("plan_transaction"), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.weight(1f))
                Switch(checked = uiState.isPlanned, onCheckedChange = { viewModel.onEvent(AddTransactionEvent.OnIsPlannedChanged(it)) })
            }
            
            AnimatedVisibility(
                visible = uiState.isPlanned,
                label = "PlannedTransactionFields"
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource("date", "date" to uiState.date.toString()))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RepeatRule.values().forEach { rule ->
                            OutlinedButton(
                                onClick = { viewModel.onEvent(AddTransactionEvent.OnRecurrenceChanged(rule)) },
                                border = if(uiState.recurrenceRule == rule) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else ButtonDefaults.outlinedButtonBorder
                            ) {
                                Text(getRepeatRuleName(rule))
                            }
                        }
                    }
                }
            }

            if (showDatePicker) {
                val language = LocalAppLanguage.current
                val locale = java.util.Locale(language.code)
                // Устанавливаем локаль для DatePicker через CompositionLocalProvider
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = uiState.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = { 
                            datePickerState.selectedDateMillis?.let {
                                val selectedDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                                viewModel.onEvent(AddTransactionEvent.OnDateChanged(selectedDate))
                            }
                            showDatePicker = false 
                        }) {
                            Text(stringResource("ok"))
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))

            Button(
                onClick = { viewModel.onEvent(AddTransactionEvent.SaveTransaction) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(vertical = 8.dp),
                enabled = uiState.amount.isNotBlank() && uiState.selectedCategory != null
            ) {
                Text(stringResource("save"))
            }
        }
    }
}

@Composable
private fun TemplateItem(template: Template, onClick: () -> Unit) {
    AuroraGlassCard(
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = template.description,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun mapCategoryNameToIcon(categoryName: String): ImageVector {
    // Используем ID категории или имя для определения иконки
    // Категории могут быть на разных языках, поэтому используем имя как есть
    return when {
        categoryName.contains("Продукты", ignoreCase = true) || categoryName.contains("Food", ignoreCase = true) || categoryName.contains("Lebensmittel", ignoreCase = true) -> Icons.Default.ShoppingBag
        categoryName.contains("Еда", ignoreCase = true) || categoryName.contains("Meals", ignoreCase = true) || categoryName.contains("Mahlzeiten", ignoreCase = true) -> Icons.Default.Fastfood
        categoryName.contains("Игры", ignoreCase = true) || categoryName.contains("Games", ignoreCase = true) || categoryName.contains("Spiele", ignoreCase = true) -> Icons.Default.Games
        categoryName.contains("Развлечения", ignoreCase = true) || categoryName.contains("Entertainment", ignoreCase = true) || categoryName.contains("Unterhaltung", ignoreCase = true) -> Icons.Default.Cake
        categoryName.contains("Зарплата", ignoreCase = true) || categoryName.contains("Salary", ignoreCase = true) || categoryName.contains("Gehalt", ignoreCase = true) -> Icons.Default.Work
        categoryName.contains("Подарок", ignoreCase = true) || categoryName.contains("Gift", ignoreCase = true) || categoryName.contains("Geschenk", ignoreCase = true) -> Icons.Default.Redeem
        categoryName.contains("Накопления", ignoreCase = true) || categoryName.contains("Savings", ignoreCase = true) || categoryName.contains("Ersparnisse", ignoreCase = true) -> Icons.Default.Savings
        else -> Icons.Default.ShoppingBag
    }
}

@Composable
private fun getRepeatRuleName(rule: RepeatRule): String {
    return when (rule) {
        RepeatRule.NONE -> stringResource("repeat_none")
        RepeatRule.WEEKLY -> stringResource("repeat_weekly")
        RepeatRule.MONTHLY -> stringResource("repeat_monthly")
        RepeatRule.YEARLY -> stringResource("repeat_yearly")
    }
}

@Composable
fun CategoryItemView(category: Category, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val isScifi = BariTheme.mode == AppThemeMode.SCIFI
    val categoryColor = getCategoryColor(category.name)
    
    if (isScifi) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(
                    if (isSelected) 
                        BariTheme.colors.cardBackground.copy(alpha = 0.8f)
                    else 
                        BariTheme.colors.cardBackground.copy(alpha = 0.3f)
                )
                .then(
                    if (isSelected) 
                        Modifier.border(
                            2.dp,
                            categoryColor,
                            MaterialTheme.shapes.medium
                        )
                    else Modifier
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            // Свечение вокруг выбранной категории
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(8.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    categoryColor.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = category.name,
                    tint = if (isSelected) categoryColor else BariTheme.colors.textSecondary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) BariTheme.colors.textPrimary else BariTheme.colors.textSecondary,
                    fontSize = 10.sp
                )
            }
        }
    } else {
        // STANDARD версия
        val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
        val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        val border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .size(90.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(backgroundColor)
                .then(if (border != null) Modifier.border(border, MaterialTheme.shapes.medium) else Modifier)
                .clickable(onClick = onClick)
        ) {
            AppIcon(icon, contentDescription = category.name, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor
            )
        }
    }
}

@Composable
fun SciFiAmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    isIncome: Boolean,
    modifier: Modifier = Modifier
) {
    val underlineColor = if (isIncome) 
        BariTheme.colors.tertiary // Зеленая для дохода
    else 
        BariTheme.colors.secondary // Фиолетовая для расхода
    
    Column(modifier = modifier.padding(vertical = 16.dp)) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.displayLarge.copy(
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = BariTheme.colors.textPrimary,
                letterSpacing = 2.sp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Неоновая подчеркивающая линия
                    drawLine(
                        color = underlineColor,
                        start = Offset(0f, size.height - 4.dp.toPx()),
                        end = Offset(size.width, size.height - 4.dp.toPx()),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource("amount"),
            style = MaterialTheme.typography.labelSmall,
            color = BariTheme.colors.textSecondary.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun getCategoryColor(categoryName: String): Color {
    return when {
        categoryName.contains("Еда", ignoreCase = true) || 
        categoryName.contains("Meals", ignoreCase = true) || 
        categoryName.contains("Food", ignoreCase = true) -> NeonYellow
        categoryName.contains("Игры", ignoreCase = true) || 
        categoryName.contains("Games", ignoreCase = true) -> AuroraPurple
        categoryName.contains("Зарплата", ignoreCase = true) || 
        categoryName.contains("Salary", ignoreCase = true) -> AuroraBlue
        categoryName.contains("Подарок", ignoreCase = true) || 
        categoryName.contains("Gift", ignoreCase = true) -> AuroraMint
        else -> BariTheme.colors.tertiary
    }
}