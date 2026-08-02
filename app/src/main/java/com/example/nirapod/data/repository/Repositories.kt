package com.example.nirapod.data.repository

import android.net.Uri
import com.example.nirapod.data.model.AiAnalysis
import com.example.nirapod.data.model.AppUser
import com.example.nirapod.data.model.HazardReport
import com.example.nirapod.data.model.SosAlert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface AuthRepository {

    suspend fun login(
        email: String,
        password: String
    ): Result<AppUser>

    suspend fun register(
        name: String,
        email: String,
        password: String,
        role: String,
        authorityType: String
    ): Result<AppUser>

    suspend fun currentUser(): AppUser?

    suspend fun saveFcmToken(token: String)

    fun observePendingUsers(): Flow<List<AppUser>> {
        return flowOf(emptyList())
    }

    suspend fun updateApproval(
        userId: String,
        approvalStatus: String
    ): Result<Unit> {
        return Result.failure(
            UnsupportedOperationException(
                "Admin operations are unavailable in demo mode"
            )
        )
    }

    fun logout()
}

interface ReportRepository {

    fun observeReports(): Flow<List<HazardReport>>

    fun observeMyReports(
        userId: String
    ): Flow<List<HazardReport>>

    suspend fun getReport(
        reportId: String
    ): Result<HazardReport>

    suspend fun submitReport(
        report: HazardReport,
        imageUri: Uri?
    ): Result<String>

    suspend fun updateStatus(
        reportId: String,
        status: String,
        authorityId: String
    ): Result<Unit>

    suspend fun confirmReport(
        reportId: String,
        userId: String
    ): Result<Unit>
}

interface SosRepository {

    fun observeActiveSos(): Flow<List<SosAlert>>

    suspend fun sendSos(
        alert: SosAlert
    ): Result<String>

    suspend fun resolveSos(
        id: String
    ): Result<Unit>
}

interface ImageStorageRepository {

    suspend fun uploadImage(
        uri: Uri,
        ownerId: String
    ): Result<String>
}

interface AiRepository {

    suspend fun analyze(
        imageUri: Uri?,
        description: String
    ): Result<AiAnalysis>
}