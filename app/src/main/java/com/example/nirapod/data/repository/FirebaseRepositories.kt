package com.example.nirapod.data.repository

import android.net.Uri
import com.example.nirapod.data.model.AppUser
import com.example.nirapod.data.model.ApprovalStatus
import com.example.nirapod.data.model.AuthorityType
import com.example.nirapod.data.model.UserRole
import com.example.nirapod.data.model.HazardReport
import com.example.nirapod.data.model.SosAlert
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override suspend fun login(
        email: String,
        password: String
    ): Result<AppUser> = runCatching {

        val cleanEmail = email.trim()

        require(cleanEmail.isNotBlank()) {
            "Email is required"
        }

        require(password.isNotBlank()) {
            "Password is required"
        }

        val authResult = auth
            .signInWithEmailAndPassword(
                cleanEmail,
                password
            )
            .await()

        val uid = authResult.user?.uid
            ?: error("Authentication succeeded without a user")

        val document = firestore
            .collection("users")
            .document(uid)
            .get()
            .await()

        val appUser = document.toObject(
            AppUser::class.java
        )

        if (appUser == null) {
            auth.signOut()
            error(
                "User profile was not found in Firestore. " +
                        "Delete this account from Firebase Authentication " +
                        "and register again."
            )
        }

        appUser
    }

    override suspend fun register(
        name: String,
        email: String,
        password: String,
        role: String,
        authorityType: String
    ): Result<AppUser> {

        var createdUid: String? = null

        return try {
            val cleanName = name.trim()
            val cleanEmail = email.trim()

            require(cleanName.isNotBlank()) {
                "Full name is required"
            }

            require(cleanEmail.isNotBlank()) {
                "Email is required"
            }

            require(password.length >= 6) {
                "Password must contain at least 6 characters"
            }

            val selectedRole = UserRole.entries
                .firstOrNull { it.name == role }
                ?: error("Invalid account role")

            val selectedAuthorityType =
                if (selectedRole == UserRole.AUTHORITY) {
                    AuthorityType.entries.firstOrNull {
                        it.name == authorityType
                    } ?: AuthorityType.NONE
                } else {
                    AuthorityType.NONE
                }

            require(
                selectedRole != UserRole.AUTHORITY ||
                        selectedAuthorityType != AuthorityType.NONE
            ) {
                "Please select an authority type"
            }

            val approvalStatus =
                when (selectedRole) {
                    UserRole.CITIZEN ->
                        ApprovalStatus.APPROVED

                    UserRole.AUTHORITY,
                    UserRole.ADMIN ->
                        ApprovalStatus.PENDING
                }

            val authResult = auth
                .createUserWithEmailAndPassword(
                    cleanEmail,
                    password
                )
                .await()

            val firebaseUser = authResult.user
                ?: error("Account creation failed")

            createdUid = firebaseUser.uid

            val appUser = AppUser(
                uid = firebaseUser.uid,
                name = cleanName,
                email = cleanEmail,
                role = selectedRole.name,
                authorityType = selectedAuthorityType.name,
                approvalStatus = approvalStatus.name
            )

            firestore
                .collection("users")
                .document(firebaseUser.uid)
                .set(appUser)
                .await()

            Result.success(appUser)

        } catch (error: Exception) {

            val firebaseUser = auth.currentUser

            if (
                createdUid != null &&
                firebaseUser?.uid == createdUid
            ) {
                try {
                    firebaseUser.delete().await()
                } catch (_: Exception) {
                    // Ignore cleanup error.
                }
            }

            auth.signOut()

            Result.failure(error)
        }
    }

    override suspend fun currentUser(): AppUser? {
        val uid = auth.currentUser?.uid
            ?: return null

        val document = firestore
            .collection("users")
            .document(uid)
            .get()
            .await()

        val appUser = document.toObject(
            AppUser::class.java
        )

        if (appUser == null) {
            auth.signOut()
        }

        return appUser
    }

    override suspend fun saveFcmToken(
        token: String
    ) {
        val uid = auth.currentUser?.uid
            ?: return

        firestore
            .collection("users")
            .document(uid)
            .update("fcmToken", token)
            .await()
    }

    override fun observePendingUsers():
            Flow<List<AppUser>> = callbackFlow {

        val listener = firestore
            .collection("users")
            .whereEqualTo(
                "approvalStatus",
                ApprovalStatus.PENDING.name
            )
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val users = snapshot
                    ?.documents
                    .orEmpty()
                    .mapNotNull { document ->
                        document
                            .toObject(AppUser::class.java)
                            ?.copy(uid = document.id)
                    }
                    .sortedBy { it.createdAt }

                trySend(users)
            }

        awaitClose {
            listener.remove()
        }
    }

    override suspend fun updateApproval(
        userId: String,
        approvalStatus: String
    ): Result<Unit> = runCatching {

        require(
            approvalStatus == ApprovalStatus.APPROVED.name ||
                    approvalStatus == ApprovalStatus.REJECTED.name
        ) {
            "Invalid approval status"
        }

        firestore
            .collection("users")
            .document(userId)
            .update(
                mapOf(
                    "approvalStatus" to approvalStatus
                )
            )
            .await()

        Unit
    }

    override fun logout() {
        auth.signOut()
    }
}

class FirestoreReportRepository(
    private val firestore: FirebaseFirestore,
    private val imageStorage: ImageStorageRepository
) : ReportRepository {
    private val collection get() = firestore.collection("reports")

    override fun observeReports(): Flow<List<HazardReport>> = callbackFlow {
        val listener = collection.orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().mapNotNull { doc ->
                    doc.toObject(HazardReport::class.java)?.copy(id = doc.id)
                })
            }
        awaitClose { listener.remove() }
    }

    override fun observeMyReports(userId: String): Flow<List<HazardReport>> = callbackFlow {
        val listener = collection.whereEqualTo("reporterId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val reports = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    doc.toObject(HazardReport::class.java)?.copy(id = doc.id)
                }.sortedByDescending { it.createdAt }
                trySend(reports)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getReport(reportId: String): Result<HazardReport> = runCatching {
        val doc = collection.document(reportId).get().await()
        doc.toObject(HazardReport::class.java)?.copy(id = doc.id) ?: error("Report not found")
    }

    override suspend fun submitReport(report: HazardReport, imageUri: Uri?): Result<String> = runCatching {
        val id = collection.document().id.ifBlank { UUID.randomUUID().toString() }
        val imageUrl = if (imageUri != null) {
            imageStorage.uploadImage(imageUri, report.reporterId).getOrThrow()
        } else ""
        val saved = report.copy(
            id = id,
            imageUrl = imageUrl,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        collection.document(id).set(saved).await()
        id
    }

    override suspend fun updateStatus(reportId: String, status: String, authorityId: String): Result<Unit> = runCatching {
        collection.document(reportId).update(
            mapOf(
                "status" to status,
                "assignedAuthority" to authorityId,
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
        Unit
    }

    override suspend fun confirmReport(reportId: String, userId: String): Result<Unit> = runCatching {
        val reportRef = collection.document(reportId)
        val confirmationRef = reportRef.collection("confirmations").document(userId)
        firestore.runTransaction { transaction ->
            if (!transaction.get(confirmationRef).exists()) {
                transaction.set(confirmationRef, mapOf("userId" to userId, "createdAt" to System.currentTimeMillis()))
                transaction.update(reportRef, "confirmations", FieldValue.increment(1))
            }
        }.await()
        Unit
    }
}

class FirestoreSosRepository(
    private val firestore: FirebaseFirestore
) : SosRepository {
    private val collection get() = firestore.collection("sos_alerts")

    override fun observeActiveSos(): Flow<List<SosAlert>> = callbackFlow {
        val listener = collection.whereEqualTo("status", "ACTIVE")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    doc.toObject(SosAlert::class.java)?.copy(id = doc.id)
                }.sortedByDescending { it.createdAt }
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun sendSos(alert: SosAlert): Result<String> = runCatching {
        val id = collection.document().id
        collection.document(id).set(alert.copy(id = id)).await()
        id
    }

    override suspend fun resolveSos(id: String): Result<Unit> = runCatching {
        collection.document(id).update("status", "RESOLVED").await()
        Unit
    }
}
