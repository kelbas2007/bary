package com.example.bary.ui

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * EventBus для триггеров Бари.
 * Используется для связи между ViewModels и BariViewModel.
 */
@Singleton
class BariEventBus @Inject constructor() {
    private val _triggers = MutableSharedFlow<BariTrigger>(extraBufferCapacity = 10)
    val triggers: SharedFlow<BariTrigger> = _triggers.asSharedFlow()

    fun emit(trigger: BariTrigger) {
        _triggers.tryEmit(trigger)
    }
}





