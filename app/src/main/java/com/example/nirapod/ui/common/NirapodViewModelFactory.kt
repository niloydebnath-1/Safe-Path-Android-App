package com.example.nirapod.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.nirapod.NirapodApplication
import com.example.nirapod.ui.auth.AuthViewModel
import com.example.nirapod.ui.authority.SosViewModel
import com.example.nirapod.ui.report.ReportViewModel

class NirapodViewModelFactory(
    private val application: NirapodApplication
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(AuthViewModel::class.java) ->
            AuthViewModel(application.container.authRepository) as T
        modelClass.isAssignableFrom(ReportViewModel::class.java) ->
            ReportViewModel(application.container.reportRepository, application.container.aiRepository) as T
        modelClass.isAssignableFrom(SosViewModel::class.java) ->
            SosViewModel(application.container.sosRepository) as T
        else -> error("Unknown ViewModel class: ${modelClass.name}")
    }
}
