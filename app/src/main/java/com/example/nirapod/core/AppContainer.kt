package com.example.nirapod.core

import android.content.Context
import com.example.nirapod.data.repository.AiRepository
import com.example.nirapod.data.repository.AuthRepository
import com.example.nirapod.data.repository.DemoAiRepository
import com.example.nirapod.data.repository.DemoAuthRepository
import com.example.nirapod.data.repository.DemoReportRepository
import com.example.nirapod.data.repository.DemoSosRepository
import com.example.nirapod.data.repository.FirebaseAiRepository
import com.example.nirapod.data.repository.FirebaseAuthRepository
import com.example.nirapod.data.repository.FirestoreReportRepository
import com.example.nirapod.data.repository.FirestoreSosRepository
import com.example.nirapod.data.repository.ImageStorageRepository
import com.example.nirapod.data.repository.LocalImageStorageRepository
import com.example.nirapod.data.repository.ReportRepository
import com.example.nirapod.data.repository.SosRepository
import com.example.nirapod.data.repository.SupabaseImageStorageRepository
import com.example.nirapod.util.LocationClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AppContainer(context: Context) {
    val demoSession = DemoSession(context)
    val locationClient = LocationClient(context)

    val authRepository: AuthRepository
    val reportRepository: ReportRepository
    val sosRepository: SosRepository
    val aiRepository: AiRepository

    init {
        if (AppConfig.firebaseConfigured) {
            val auth = FirebaseAuth.getInstance()
            val firestore = FirebaseFirestore.getInstance()
            val imageStorage: ImageStorageRepository = if (AppConfig.supabaseConfigured) {
                SupabaseImageStorageRepository(context)
            } else {
                LocalImageStorageRepository(context)
            }
            authRepository = FirebaseAuthRepository(auth, firestore)
            reportRepository = FirestoreReportRepository(firestore, imageStorage)
            sosRepository = FirestoreSosRepository(firestore)
            aiRepository = FirebaseAiRepository(context)
        } else {
            authRepository = DemoAuthRepository(demoSession)
            reportRepository = DemoReportRepository()
            sosRepository = DemoSosRepository()
            aiRepository = DemoAiRepository()
        }
    }
}
