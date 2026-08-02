package com.example.nirapod.data.repository

import android.content.Context
import android.net.Uri
import com.example.nirapod.BuildConfig
import com.example.nirapod.data.remote.SupabaseStorageApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.UUID

class SupabaseImageStorageRepository(
    private val context: Context
) : ImageStorageRepository {
    private val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/') + "/"
    private val api: SupabaseStorageApi by lazy {
        val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = okhttp3.OkHttpClient.Builder().addInterceptor(logger).build()
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .build()
            .create(SupabaseStorageApi::class.java)
    }

    override suspend fun uploadImage(uri: Uri, ownerId: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val mime = resolver.getType(uri) ?: "image/jpeg"
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("Unable to read selected image")
            val extension = when (mime) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }
            val path = "$ownerId/${System.currentTimeMillis()}-${UUID.randomUUID()}.$extension"
            val response = api.upload(
                bucket = BuildConfig.SUPABASE_BUCKET,
                path = path,
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                authorization = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}",
                body = bytes.toRequestBody(mime.toMediaTypeOrNull())
            )
            if (!response.isSuccessful) error("Image upload failed: HTTP ${response.code()}")
            "${baseUrl}storage/v1/object/public/${BuildConfig.SUPABASE_BUCKET}/$path"
        }
    }
}
