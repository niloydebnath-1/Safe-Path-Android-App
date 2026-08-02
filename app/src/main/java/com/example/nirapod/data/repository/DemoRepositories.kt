package com.example.nirapod.data.repository

import android.content.Context
import android.net.Uri
import com.example.nirapod.core.DemoSession
import com.example.nirapod.data.model.AiAnalysis
import com.example.nirapod.data.model.AppUser
import com.example.nirapod.data.model.HazardReport
import com.example.nirapod.data.model.ReportStatus
import com.example.nirapod.data.model.Severity
import com.example.nirapod.data.model.SosAlert
import com.example.nirapod.data.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

class DemoAuthRepository(
    private val session: DemoSession
) : AuthRepository {
    override suspend fun login(email: String, password: String): Result<AppUser> {
        if (email.isBlank() || password.isBlank()) return Result.failure(IllegalArgumentException("Email and password are required"))
        val role = if (email.contains("authority", ignoreCase = true)) UserRole.AUTHORITY else UserRole.CITIZEN
        val user = AppUser(
            uid = "demo-${role.name.lowercase()}",
            name = if (role == UserRole.AUTHORITY) "Demo Authority" else "Demo Citizen",
            email = email,
            role = role.name
        )
        session.save(user)
        return Result.success(user)
    }

    override suspend fun register(name: String, email: String, password: String, role: String, authorityType: String): Result<AppUser> {
        if (name.isBlank() || email.isBlank() || password.length < 6) {
            return Result.failure(IllegalArgumentException("Enter a name, valid email, and a password of at least 6 characters"))
        }
        val user = AppUser(uid = "demo-${UUID.randomUUID()}", name = name, email = email, role = role)
        session.save(user)
        return Result.success(user)
    }

    override suspend fun currentUser(): AppUser? = session.get()
    override suspend fun saveFcmToken(token: String) = Unit
    override fun logout() = session.clear()

    fun loginAs(role: UserRole): AppUser {
        val user = AppUser(
            uid = "demo-${role.name.lowercase()}",
            name = if (role == UserRole.AUTHORITY) "Demo Authority" else "Demo Citizen",
            email = "${role.name.lowercase()}@demo.local",
            role = role.name
        )
        session.save(user)
        return user
    }
}

class DemoReportRepository : ReportRepository {
    private val reports = MutableStateFlow(
        listOf(
            HazardReport(
                id = "demo-1",
                reporterId = "demo-citizen",
                reporterName = "Demo Citizen",
                category = "Open Manhole",
                description = "An uncovered manhole is creating a serious risk beside the road.",
                latitude = 23.8103,
                longitude = 90.4125,
                status = ReportStatus.VERIFIED.name,
                severity = Severity.HIGH.name,
                aiCategory = "Open Manhole",
                aiSeverity = Severity.HIGH.name,
                aiSummary = "Uncovered manhole near pedestrian movement; urgent barrier and cover replacement recommended.",
                confirmations = 4
            ),
            HazardReport(
                id = "demo-2",
                reporterId = "demo-citizen-2",
                reporterName = "Community Member",
                category = "Snatching Hotspot",
                description = "Multiple phone snatching reports after evening hours.",
                latitude = 23.7937,
                longitude = 90.4066,
                status = ReportStatus.RECEIVED.name,
                severity = Severity.HIGH.name,
                aiCategory = "Crime Hotspot",
                aiSeverity = Severity.HIGH.name,
                aiSummary = "Repeated evening snatching reports; commuters should receive a caution alert.",
                confirmations = 7
            )
        )
    )

    override fun observeReports(): Flow<List<HazardReport>> = reports
    override fun observeMyReports(userId: String): Flow<List<HazardReport>> = reports.map { list -> list.filter { it.reporterId == userId } }

    override suspend fun getReport(reportId: String): Result<HazardReport> =
        reports.value.firstOrNull { it.id == reportId }?.let(Result.Companion::success)
            ?: Result.failure(NoSuchElementException("Report not found"))

    override suspend fun submitReport(report: HazardReport, imageUri: Uri?): Result<String> {
        val id = UUID.randomUUID().toString()
        val saved = report.copy(id = id, imageUrl = imageUri?.toString().orEmpty(), createdAt = System.currentTimeMillis())
        reports.value = listOf(saved) + reports.value
        return Result.success(id)
    }

    override suspend fun updateStatus(reportId: String, status: String, authorityId: String): Result<Unit> {
        reports.value = reports.value.map {
            if (it.id == reportId) it.copy(status = status, assignedAuthority = authorityId, updatedAt = System.currentTimeMillis()) else it
        }
        return Result.success(Unit)
    }

    override suspend fun confirmReport(reportId: String, userId: String): Result<Unit> {
        reports.value = reports.value.map {
            if (it.id == reportId) it.copy(confirmations = it.confirmations + 1) else it
        }
        return Result.success(Unit)
    }
}

class DemoSosRepository : SosRepository {
    private val alerts = MutableStateFlow<List<SosAlert>>(emptyList())
    override fun observeActiveSos(): Flow<List<SosAlert>> = alerts

    override suspend fun sendSos(alert: SosAlert): Result<String> {
        val id = UUID.randomUUID().toString()
        alerts.value = listOf(alert.copy(id = id)) + alerts.value
        return Result.success(id)
    }

    override suspend fun resolveSos(id: String): Result<Unit> {
        alerts.value = alerts.value.filterNot { it.id == id }
        return Result.success(Unit)
    }
}

class LocalImageStorageRepository(private val context: Context) : ImageStorageRepository {
    override suspend fun uploadImage(uri: Uri, ownerId: String): Result<String> = Result.success(uri.toString())
}

class DemoAiRepository : AiRepository {
    override suspend fun analyze(imageUri: Uri?, description: String): Result<AiAnalysis> {
        val text = description.lowercase()
        val category = when {
            "manhole" in text || "ঢাকনা" in text -> "Open Manhole"
            "pothole" in text || "গর্ত" in text -> "Damaged Road"
            "drain" in text || "ড্রেন" in text -> "Broken Drain"
            "water" in text || "জলাবদ্ধ" in text -> "Waterlogging"
            "snatch" in text || "ছিনতাই" in text -> "Crime Hotspot"
            "electric" in text || "বিদ্যুৎ" in text -> "Electrical Hazard"
            else -> "Other Hazard"
        }
        val severity = when {
            listOf("death", "critical", "huge", "open", "রাস্তায়", "বিপজ্জনক").any { it in text } -> Severity.HIGH.name
            else -> Severity.MEDIUM.name
        }
        return Result.success(
            AiAnalysis(
                category = category,
                severity = severity,
                summary = "AI demo analysis: $description".take(240),
                risk = "Possible risk to commuters and pedestrians",
                suggestedAuthority = if (category == "Crime Hotspot") "Police / Safety Authority" else "City Corporation"
            )
        )
    }
}
