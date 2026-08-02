package com.example.nirapod.data.repository

import com.example.nirapod.data.model.AppUser

class AuthRepositoryImpl : AuthRepository {
    override suspend fun login(
        email: String,
        password: String
    ): Result<AppUser> {
        TODO("Not yet implemented")
    }

    override suspend fun register(
        name: String,
        email: String,
        password: String,
        role: String,
        authorityType: String
    ): Result<AppUser> {
        TODO("Not yet implemented")
    }

    override suspend fun currentUser(): AppUser? {
        TODO("Not yet implemented")
    }

    override suspend fun saveFcmToken(token: String) {
        TODO("Not yet implemented")
    }

    override fun logout() {
        TODO("Not yet implemented")
    }
}