package com.dam2.haru_petcare.network

import com.dam2.haru_petcare.util.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * En Kotlin, el Singleton se implementa con 'object'.
 * No hace falta el patrón getInstance() de Java —
 * Kotlin lo garantiza a nivel de lenguaje.
 */
object RetrofitClient {

    /**
     * Devuelve una instancia de Retrofit con el token JWT inyectado.
     * @param token JWT del usuario. Null para login/registro (endpoints públicos).
     */
    fun getClient(token: String? = null): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(buildOkHttpClient(token))
            .build()
    }

    private fun buildOkHttpClient(token: String?): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()

                // Si no hay token, la petición sale tal cual
                val request = if (token != null) {
                    original.newBuilder()
                        .header("Authorization", "Bearer $token") // String template de Kotlin
                        .build()
                } else {
                    original
                }

                chain.proceed(request)
            }
            .addInterceptor(logging)
            .build()
    }
}