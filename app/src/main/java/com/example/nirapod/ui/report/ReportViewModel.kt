package com.example.nirapod.ui.report

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nirapod.core.UiResult
import com.example.nirapod.data.model.AiAnalysis
import com.example.nirapod.data.model.HazardReport
import com.example.nirapod.data.repository.AiRepository
import com.example.nirapod.data.repository.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReportViewModel(
    private val reportsRepository: ReportRepository,
    private val aiRepository: AiRepository
) : ViewModel() {
    private val _reports = MutableStateFlow<List<HazardReport>>(emptyList())
    val reports: StateFlow<List<HazardReport>> = _reports.asStateFlow()

    private val _myReports = MutableStateFlow<List<HazardReport>>(emptyList())
    val myReports: StateFlow<List<HazardReport>> = _myReports.asStateFlow()

    private val _detail = MutableStateFlow<UiResult<HazardReport>>(UiResult.Idle)
    val detail: StateFlow<UiResult<HazardReport>> = _detail.asStateFlow()

    private val _aiState = MutableStateFlow<UiResult<AiAnalysis>>(UiResult.Idle)
    val aiState: StateFlow<UiResult<AiAnalysis>> = _aiState.asStateFlow()

    private val _submitState = MutableStateFlow<UiResult<String>>(UiResult.Idle)
    val submitState: StateFlow<UiResult<String>> = _submitState.asStateFlow()

    private val _actionState = MutableStateFlow<UiResult<Unit>>(UiResult.Idle)
    val actionState: StateFlow<UiResult<Unit>> = _actionState.asStateFlow()

    init {
        observeAllReports()
    }

    private fun observeAllReports() = viewModelScope.launch {
        runCatching {
            reportsRepository.observeReports().collect { _reports.value = it }
        }
    }

    fun observeMyReports(userId: String) = viewModelScope.launch {
        runCatching {
            reportsRepository.observeMyReports(userId).collect { _myReports.value = it }
        }
    }

    fun loadReport(reportId: String) = viewModelScope.launch {
        _detail.value = UiResult.Loading
        reportsRepository.getReport(reportId)
            .onSuccess { _detail.value = UiResult.Success(it) }
            .onFailure { _detail.value = UiResult.Error(it.message ?: "Unable to load report") }
    }

    fun analyze(imageUri: Uri?, description: String) = viewModelScope.launch {
        _aiState.value = UiResult.Loading
        aiRepository.analyze(imageUri, description)
            .onSuccess { _aiState.value = UiResult.Success(it) }
            .onFailure { _aiState.value = UiResult.Error(it.message ?: "AI analysis failed") }
    }

    fun submit(report: HazardReport, imageUri: Uri?) = viewModelScope.launch {
        _submitState.value = UiResult.Loading
        reportsRepository.submitReport(report, imageUri)
            .onSuccess { _submitState.value = UiResult.Success(it) }
            .onFailure { _submitState.value = UiResult.Error(it.message ?: "Report submission failed") }
    }

    fun confirm(reportId: String, userId: String) = viewModelScope.launch {
        _actionState.value = UiResult.Loading
        reportsRepository.confirmReport(reportId, userId)
            .onSuccess {
                _actionState.value = UiResult.Success(Unit)
                loadReport(reportId)
            }
            .onFailure { _actionState.value = UiResult.Error(it.message ?: "Confirmation failed") }
    }

    fun updateStatus(reportId: String, status: String, authorityId: String) = viewModelScope.launch {
        _actionState.value = UiResult.Loading
        reportsRepository.updateStatus(reportId, status, authorityId)
            .onSuccess {
                _actionState.value = UiResult.Success(Unit)
                loadReport(reportId)
            }
            .onFailure { _actionState.value = UiResult.Error(it.message ?: "Status update failed") }
    }

    fun clearAiState() { _aiState.value = UiResult.Idle }
    fun clearSubmitState() { _submitState.value = UiResult.Idle }
    fun clearActionState() { _actionState.value = UiResult.Idle }
}
