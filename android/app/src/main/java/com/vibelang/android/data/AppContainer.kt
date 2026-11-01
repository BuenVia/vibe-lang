package com.vibelang.android.data

import android.content.Context
import com.vibelang.android.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/** Simple manual dependency container, created once by [com.vibelang.android.VibeLangApplication]. */
class AppContainer(context: Context) {

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        // Render's free tier sleeps when idle; the first request can take up to a minute.
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val api: VibeLangApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttp)
        .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(VibeLangApi::class.java)

    val settings = SettingsRepository(context.applicationContext)
}

val ApiJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/** Thrown by [requireSuccess] for non-2xx responses that are returned as Response<…>. */
class ApiException(message: String) : Exception(message)

/** For endpoints declared as Response<Unit> (deletes): throws a readable error unless the call succeeded. */
fun Response<*>.requireSuccess() {
    if (!isSuccessful) throw ApiException(problemDetail(errorBody()?.string()) ?: "Request failed (${code()})")
}

private fun problemDetail(body: String?): String? =
    body?.let { runCatching { ApiJson.decodeFromString<ApiProblem>(it) }.getOrNull() }
        ?.let { it.detail ?: it.title }

/** Turns any failure into a message that can be shown to the user. */
fun Throwable.userMessage(): String = when (this) {
    is HttpException -> problemDetail(response()?.errorBody()?.string()) ?: "Request failed (${code()})"
    is SocketTimeoutException -> "The server took too long to respond. It may be waking up, so try again."
    is IOException -> "Can't reach the server. Check your internet connection."
    else -> message ?: "Something went wrong"
}
