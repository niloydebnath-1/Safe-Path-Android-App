package com.example.nirapod.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nirapod.core.UiResult
import com.example.nirapod.data.model.AppUser
import com.example.nirapod.data.model.UserRole
import com.example.nirapod.data.repository.AuthRepository
import com.example.nirapod.data.repository.DemoAuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _state =
        MutableStateFlow<UiResult<AppUser>>(
            UiResult.Idle
        )

    val state: StateFlow<UiResult<AppUser>> =
        _state.asStateFlow()

    val pendingUsers: Flow<List<AppUser>> =
        repository.observePendingUsers()

    private val _adminActionState =
        MutableStateFlow<UiResult<Unit>>(
            UiResult.Idle
        )

    val adminActionState:
            StateFlow<UiResult<Unit>> =
        _adminActionState.asStateFlow()

    fun login(
        email: String,
        password: String
    ) = viewModelScope.launch {

        _state.value = UiResult.Loading

        repository
            .login(email, password)
            .onSuccess {
                _state.value =
                    UiResult.Success(it)
            }
            .onFailure {
                _state.value =
                    UiResult.Error(
                        it.message ?: "Login failed"
                    )
            }
    }

    fun register(
        name: String,
        email: String,
        password: String,
        role: String,
        authorityType: String
    ) = viewModelScope.launch {

        _state.value = UiResult.Loading

        repository
            .register(
                name,
                email,
                password,
                role,
                authorityType
            )
            .onSuccess {
                _state.value =
                    UiResult.Success(it)
            }
            .onFailure {
                _state.value =
                    UiResult.Error(
                        it.message
                            ?: "Registration failed"
                    )
            }
    }

    fun updateApproval(
        userId: String,
        approvalStatus: String
    ) = viewModelScope.launch {

        _adminActionState.value =
            UiResult.Loading

        repository
            .updateApproval(
                userId,
                approvalStatus
            )
            .onSuccess {
                _adminActionState.value =
                    UiResult.Success(Unit)
            }
            .onFailure {
                _adminActionState.value =
                    UiResult.Error(
                        it.message
                            ?: "Approval update failed"
                    )
            }
    }

    fun demoLogin(role: UserRole) {
        val demoRepository =
            repository as? DemoAuthRepository

        if (demoRepository == null) {
            _state.value =
                UiResult.Error(
                    "Demo login is available only " +
                            "when Firebase is not configured"
                )
        } else {
            _state.value =
                UiResult.Success(
                    demoRepository.loginAs(role)
                )
        }
    }

    suspend fun currentUser(): AppUser? {
        return repository.currentUser()
    }

    fun logout() {
        repository.logout()
    }

    fun clearState() {
        _state.value = UiResult.Idle
    }

    fun clearAdminActionState() {
        _adminActionState.value =
            UiResult.Idle
    }
}