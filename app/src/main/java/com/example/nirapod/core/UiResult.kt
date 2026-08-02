package com.example.nirapod.core

sealed class UiResult<out T> {
    data object Idle : UiResult<Nothing>()
    data object Loading : UiResult<Nothing>()
    data class Success<T>(val data: T) : UiResult<T>()
    data class Error(val message: String) : UiResult<Nothing>()
}
