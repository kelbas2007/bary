package com.example.bary.ui.add_transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bary.data.model.Category
import com.example.bary.data.model.PlannedEvent
import com.example.bary.data.model.RepeatRule
import com.example.bary.data.model.Template
import com.example.bary.data.model.Transaction
import com.example.bary.data.model.TransactionType
import com.example.bary.repository.CategoryRepository
import com.example.bary.repository.FinanceRepository
import com.example.bary.repository.GamificationRepository
import com.example.bary.ui.BariEventBus
import com.example.bary.ui.BariTrigger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class AddTransactionUiState(
    val amount: String = "",
    val note: String = "",
    val selectedCategory: Category? = null,
    val availableCategories: List<Category> = emptyList(),
    val date: LocalDate = LocalDate.now(),
    val type: TransactionType = TransactionType.EXPENSE,
    val isSaved: Boolean = false,
    val isEditing: Boolean = false,
    val suggestedTemplates: List<Template> = emptyList(),
    val isPlanned: Boolean = false, // Default to OFF
    val recurrenceRule: RepeatRule = RepeatRule.NONE,
    val amountError: String? = null,
    val categoryError: String? = null,
    val isValid: Boolean = false
)

sealed interface AddTransactionEvent {
    data class OnAmountChange(val amount: String) : AddTransactionEvent
    data class OnNoteChange(val note: String) : AddTransactionEvent
    data class OnCategoryChange(val category: Category) : AddTransactionEvent
    data class OnTemplateSelected(val template: Template) : AddTransactionEvent
    data class OnIsPlannedChanged(val isPlanned: Boolean) : AddTransactionEvent
    data class OnDateChanged(val date: LocalDate) : AddTransactionEvent
    data class OnRecurrenceChanged(val rule: RepeatRule) : AddTransactionEvent
    object SaveTransaction : AddTransactionEvent
}

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
    private val gamificationRepository: GamificationRepository,
    private val categoryRepository: CategoryRepository,
    private val bariEventBus: BariEventBus,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState = _uiState.asStateFlow()

    private var transactionId: Int? = savedStateHandle.get<String>("transactionId")?.toIntOrNull()

    init {
        val typeString: String? = savedStateHandle.get("type")
        val transactionType = typeString?.let { ts -> TransactionType.valueOf(ts) } ?: TransactionType.EXPENSE

        viewModelScope.launch {
            val categories = categoryRepository.getCategories(transactionType).first()
            val templates = financeRepository.getTransactionTemplates().first()
            _uiState.update { it.copy(availableCategories = categories, type = transactionType, suggestedTemplates = templates) }

            if (transactionId != null) {
                _uiState.update { it.copy(isEditing = true) }
                financeRepository.getTransactionById(transactionId!!).first()?.let { transaction ->
                    _uiState.update {
                        it.copy(
                            amount = transaction.amount.toString(),
                            selectedCategory = categories.find { c -> c.name == transaction.category },
                            note = transaction.note ?: "",
                            date = transaction.date
                        )
                    }
                }
            }
        }
    }

    fun onEvent(event: AddTransactionEvent) {
        when (event) {
            is AddTransactionEvent.OnAmountChange -> {
                _uiState.update { it.copy(amount = event.amount) }
                validateAmount(event.amount)
                updateValidationState()
            }
            is AddTransactionEvent.OnCategoryChange -> {
                _uiState.update { it.copy(selectedCategory = event.category) }
                validateCategory(event.category)
                updateValidationState()
            }
            is AddTransactionEvent.OnNoteChange -> _uiState.update { it.copy(note = event.note) }
            is AddTransactionEvent.OnTemplateSelected -> applyTemplate(event.template)
            is AddTransactionEvent.OnIsPlannedChanged -> _uiState.update { it.copy(isPlanned = event.isPlanned) }
            is AddTransactionEvent.OnDateChanged -> _uiState.update { it.copy(date = event.date) }
            is AddTransactionEvent.OnRecurrenceChanged -> _uiState.update { it.copy(recurrenceRule = event.rule) }
            AddTransactionEvent.SaveTransaction -> save()
        }
    }
    
    private fun validateAmount(amount: String) {
        val error = when {
            amount.isBlank() -> "Введите сумму"
            amount.toLongOrNull() == null -> "Введите корректное число"
            amount.toLongOrNull()!! <= 0 -> "Сумма должна быть больше 0"
            amount.toLongOrNull()!! > 1_000_000_000 -> "Сумма слишком большая"
            else -> null
        }
        _uiState.update { it.copy(amountError = error) }
    }
    
    private fun validateCategory(category: Category?) {
        val error = if (category == null) "Выберите категорию" else null
        _uiState.update { it.copy(categoryError = error) }
    }
    
    private fun updateValidationState() {
        val currentState = _uiState.value
        val isValid = currentState.amountError == null && 
                     currentState.categoryError == null &&
                     currentState.amount.isNotBlank() &&
                     currentState.selectedCategory != null
        _uiState.update { it.copy(isValid = isValid) }
    }

    private fun applyTemplate(template: Template) {
        _uiState.update {
            it.copy(
                amount = template.amount.toString(),
                note = template.description,
                selectedCategory = it.availableCategories.find { c -> c.name == template.categoryName }
            )
        }
    }

    private fun save() {
        // Validate before saving
        val currentState = _uiState.value
        validateAmount(currentState.amount)
        validateCategory(currentState.selectedCategory)
        updateValidationState()
        
        if (!_uiState.value.isValid) {
            return
        }
        
        viewModelScope.launch {
            try {
                if (currentState.isPlanned) {
                    savePlannedEvent(currentState)
                } else {
                    saveTransaction(currentState)
                }
                _uiState.update { it.copy(isSaved = true) }
            } catch (e: Exception) {
                // Log error - in production this should be handled properly
                e.printStackTrace()
            }
        }
    }

    private suspend fun saveTransaction(currentState: AddTransactionUiState) {
        val transaction = Transaction(
            id = transactionId ?: 0, // Let Room auto-generate if 0
            type = currentState.type,
            amount = currentState.amount.toLongOrNull() ?: 0L,
            category = currentState.selectedCategory?.name ?: "",
            note = currentState.note,
            date = LocalDate.now()
        )
        financeRepository.addTransaction(transaction)
        if (transactionId == null) {
            gamificationRepository.addXp(3)
            // Trigger Bari reaction
            bariEventBus.emit(BariTrigger.OnTransactionAdded)
        }
    }

    private suspend fun savePlannedEvent(currentState: AddTransactionUiState) {
        val plannedEvent = PlannedEvent(
            title = currentState.note.ifBlank { currentState.selectedCategory?.name ?: "Плановое событие" },
            type = currentState.type,
            amount = currentState.amount.toLongOrNull() ?: 0L,
            date = currentState.date,
            repeatRule = currentState.recurrenceRule
        )
        financeRepository.addPlannedEvent(plannedEvent)
    }
}