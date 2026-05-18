package com.dam2.haru_petcare.network

import com.dam2.haru_petcare.model.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

/**
 * En Kotlin las interfaces son idénticas a Java en concepto,
 * pero la sintaxis es más limpia: 'fun' en vez de tipo de retorno primero.
 */
interface HaruApiService {

    // ── AUTH ──────────────────────────────────────────────────────────────
    @POST("api/auth/registro")
    fun registrar(@Body dto: UsuarioRegistroDTO): Call<LoginResponseDTO>

    @POST("api/auth/login")
    fun login(@Body dto: LoginRequestDTO): Call<LoginResponseDTO>

    @PUT("api/auth/fcm/{idUsuario}")
    fun actualizarTokenFcm(@Path("idUsuario") id: Long, @Body token: String): Call<Void>

    // ── MASCOTAS ──────────────────────────────────────────────────────────
    @GET("api/mascotas/dueno/{id}")
    fun getMascotasPorDueno(@Path("id") idDueno: Long): Call<List<MascotaDTO>>

    @POST("api/mascotas/inserta")
    fun insertarMascota(@Body dto: MascotaInsertarDTO): Call<MascotaDTO>

    // ── HISTORIAL ─────────────────────────────────────────────────────────
    @GET("api/historial/mascota/{id}")
    fun getHistorial(@Path("id") idMascota: Long): Call<List<HistorialMedicoDTO>>

    // ── CITAS ─────────────────────────────────────────────────────────────
    @GET("api/citas/dueno/{id}")
    fun getCitasDueno(@Path("id") idDueno: Long): Call<List<CitaDTO>>

    @GET("api/citas/veterinario/{id}")
    fun getAgendaVeterinario(@Path("id") idVeterinario: Long): Call<List<CitaDTO>>

    @POST("api/citas")
    fun crearCita(@Body dto: CitaDTO): Call<CitaDTO>

    @PUT("api/citas/{id}/estado")
    fun cambiarEstadoCita(@Path("id") idCita: Long, @Body estado: String): Call<CitaDTO>

    // ── MAPA ──────────────────────────────────────────────────────────────
    @GET("api/pines")
    fun getPines(): Call<List<PinMapaDTO>>

    @POST("api/pines")
    fun crearPin(@Body dto: PinInsertarDTO): Call<PinMapaDTO>

    @DELETE("api/pines/{id}")
    fun borrarPin(@Path("id") idPin: Long): Call<Void>

    // ── ALERTAS ───────────────────────────────────────────────────────────
    @GET("api/alertas/activas")
    fun getAlertasActivas(): Call<List<AlertaPerdidaDTO>>

    @POST("api/alertas")
    fun crearAlerta(@Body dto: AlertaInsertarDTO): Call<AlertaPerdidaDTO>

    @PUT("api/alertas/{id}/resolver")
    fun resolverAlerta(@Path("id") idAlerta: Long): Call<Void>

    // ── PDF ───────────────────────────────────────────────────────────────
    @GET("api/pdf/historial/{id}")
    fun descargarHistorialPdf(@Path("id") idMascota: Long): Call<ResponseBody>

    @GET("api/mascotas/{id}")
    fun getMascotaPorId(@Path("id") id: Long): Call<MascotaDTO>

}