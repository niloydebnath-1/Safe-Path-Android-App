package com.example.nirapod.ui.authority

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nirapod.core.UiResult
import com.example.nirapod.data.model.SosAlert
import com.example.nirapod.data.repository.SosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SosViewModel(
    private val repository: SosRepository
) : ViewModel() {
    private val _alerts = MutableStateFlow<List<SosAlert>>(emptyList())
    val alerts: StateFlow<List<SosAlert>> = _alerts.asStateFlow()

    private val _state = MutableStateFlow<UiResult<String>>(UiResult.Idle)
    val state: StateFlow<UiResult<String>> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { repository.observeActiveSos().collect { _alerts.value = it } }
        }
    }

    fun send(alert: SosAlert) = viewModelScope.launch {
        _state.value = UiResult.Loading
        repository.sendSos(alert)
            .onSuccess { _state.value = UiResult.Success(it) }
            .onFailure { _state.value = UiResult.Error(it.message ?: "SOS could not be sent") }
    }

    fun resolve(id: String) = viewModelScope.launch {
        repository.resolveSos(id)
    }

    fun clearState() { _state.value = UiResult.Idle }
}
