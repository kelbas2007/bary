package com.example.bary.ui.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bary.data.model.Achievement
import com.example.bary.data.model.UserProgress
import com.example.bary.repository.GamificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AchievementsUiState(
    val allAchievements: List<Achievement> = emptyList(),
    val userProgress: UserProgress? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val gamificationRepository: GamificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AchievementsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                gamificationRepository.getUserProgress(),
                gamificationRepository.getAchievementsFlow() // Assuming this method exists
            ) { userProgress, allAchievements ->
                _uiState.update {
                    it.copy(
                        userProgress = userProgress,
                        allAchievements = allAchievements,
                        isLoading = false
                    )
                }
            }.collect { }
        }
    }
}
