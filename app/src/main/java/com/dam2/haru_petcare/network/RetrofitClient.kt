package com.dam2.haru_petcare.network

import com.dam2.haru_petcare.util.Constants
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object RetrofitClient {

    fun getClient(token: String? = null): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(buildGson()))
            .client(buildOkHttpClient(token))
            .build()
    }

    private fun buildGson() = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java,
            JsonSerializer<LocalDate> { src, _, _ ->
                JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE))
            }
        )
        .registerTypeAdapter(LocalDate::class.java,
            JsonDeserializer { json, _, _ ->
                LocalDate.parse(json.asString, DateTimeFormatter.ISO_LOCAL_DATE)
            }
        )
        .registerTypeAdapter(LocalDateTime::class.java,
            JsonDeserializer { json, _, _ ->
                LocalDateTime.parse(json.asString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            }
        )
        .create()

    private fun buildOkHttpClient(token: String?): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = if (!token.isNullOrBlank()) {  // ← filtra null Y string vacío
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else {
                    chain.request()
                }
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .build()
    }
}