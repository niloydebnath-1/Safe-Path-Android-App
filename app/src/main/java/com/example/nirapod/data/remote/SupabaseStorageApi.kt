package com.example.nirapod.data.remote

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface SupabaseStorageApi {
    @POST("storage/v1/object/{bucket}/{path}")
    suspend fun upload(
        @Path("bucket") bucket: String,
        @Path(value = "path", encoded = true) path: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("x-upsert") upsert: String = "true",
        @Body body: RequestBody
    ): Response<Unit>
}
