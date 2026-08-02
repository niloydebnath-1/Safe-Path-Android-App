package com.example.nirapod.data.model

enum class UserRole { CITIZEN, AUTHORITY, ADMIN }

enum class AuthorityType {
    NONE,
    CITY_CORPORATION,
    DISASTER_MANAGEMENT_BOARD,
    POLICE
}

enum class ApprovalStatus { PENDING, APPROVED, REJECTED }
enum class ReportStatus { SUBMITTED, RECEIVED, VERIFIED, ASSIGNED, IN_PROGRESS, RESOLVED, REJECTED }
enum class Severity { LOW, MEDIUM, HIGH, CRITICAL }

data class AppUser(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = UserRole.CITIZEN.name,
    val authorityType: String = AuthorityType.NONE.name,
    val approvalStatus: String = ApprovalStatus.APPROVED.name,
    val fcmToken: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun userRole(): UserRole =
        runCatching { UserRole.valueOf(role) }.getOrDefault(UserRole.CITIZEN)

    fun canAccessAuthorityDashboard(): Boolean =
        userRole() in setOf(UserRole.AUTHORITY, UserRole.ADMIN) &&
                approvalStatus == ApprovalStatus.APPROVED.name

    fun roleLabel(): String {
        val base = when (userRole()) {
            UserRole.CITIZEN -> "Citizen"
            UserRole.AUTHORITY -> "Authority"
            UserRole.ADMIN -> "Admin"
        }

        val authority = when (authorityType) {
            AuthorityType.CITY_CORPORATION.name -> "City Corporation"
            AuthorityType.DISASTER_MANAGEMENT_BOARD.name -> "Disaster Management Board"
            AuthorityType.POLICE.name -> "Police"
            else -> ""
        }

        return buildString {
            append(base)
            if (authority.isNotBlank()) append(" • ").append(authority)
            if (approvalStatus != ApprovalStatus.APPROVED.name) {
                append(" • ").append(approvalStatus)
            }
        }
    }
}

data class HazardReport(
    val id: String = "",
    val reporterId: String = "",
    val reporterName: String = "",
    val category: String = "Other",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val imageUrl: String = "",
    val status: String = ReportStatus.SUBMITTED.name,
    val severity: String = Severity.MEDIUM.name,
    val aiCategory: String = "",
    val aiSeverity: String = "",
    val aiSummary: String = "",
    val assignedAuthority: String = "",
    val confirmations: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun reportStatus(): ReportStatus =
        runCatching { ReportStatus.valueOf(status) }.getOrDefault(ReportStatus.SUBMITTED)
}

data class SosAlert(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis()
)

data class AiAnalysis(
    val category: String = "Other",
    val severity: String = Severity.MEDIUM.name,
    val summary: String = "",
    val risk: String = "",
    val suggestedAuthority: String = "City Corporation"
)
