package com.dam2.haru_petcare.network

import com.dam2.haru_petcare.model.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface HaruApiService {

    // ── AUTH ──────────────────────────────────────────────────────────────
    @POST("api/auth/registro")
    fun registrar(@Body dto: UsuarioRegistroDTO): Call<LoginResponseDTO>

    @POST("api/auth/login")
    fun login(@Body dto: LoginRequestDTO): Call<LoginResponseDTO>

    @PUT("api/auth/fcm/{idUsuario}")
    fun actualizarTokenFcm(@Path("idUsuario") id: Long, @Body token: String): Call<Void>

    // ── USUARIOS ──────────────────────────────────────────────────────────
    @GET("api/usuarios/clinicas")
    fun getClincias(): Call<List<ClinicaDTO>>

    @GET("api/usuarios/buscar")
    fun buscarUsuarioPorEmail(@Query("email") email: String): Call<UsuarioDTO>

    // ── MASCOTAS ──────────────────────────────────────────────────────────
    @GET("api/mascotas/dueno/{id}")
    fun getMascotasPorDueno(@Path("id") idDueno: Long): Call<List<MascotaDTO>>

    @GET("api/mascotas/todas")
    fun buscarTodasMascotas(
        @Query("idUsuario") idUsuario: Long,
        @Query("especie") especie: String?,
        @Query("buscar") buscar: String?
    ): Call<List<MascotaDTO>>

    @POST("api/mascotas/inserta")
    fun insertarMascota(@Body dto: MascotaInsertarDTO): Call<MascotaDTO>

    @GET("api/mascotas/{id}")
    fun getMascotaPorId(@Path("id") id: Long): Call<MascotaDTO>

    @Multipart
    @POST("api/mascotas/{id}/foto")
    fun subirFotoMascota(
        @Path("id") idMascota: Long,
        @Part foto: MultipartBody.Part
    ): Call<MascotaDTO>

    @POST("api/mascotas/vincular-clinica")
    fun vincularMascotaClinica(@Body dto: VincularMascotaClinicaDTO): Call<List<MascotaDTO>>

    @POST("api/mascotas/crear-dueno-mascota")
    fun crearDuenoConMascota(@Body dto: CrearDuenoConMascotaDTO): Call<MascotaDTO>

    // ── HISTORIAL ─────────────────────────────────────────────────────────
    @GET("api/historial/mascota/{id}")
    fun getHistorial(@Path("id") idMascota: Long): Call<List<HistorialMedicoDTO>>

    // ── CITAS ─────────────────────────────────────────────────────────────
    @GET("api/citas/dueno/{id}")
    fun getCitasDueno(@Path("id") idDueno: Long): Call<List<CitaDTO>>

    @GET("api/citas/veterinario/{id}")
    fun getAgendaVeterinario(@Path("id") idVeterinario: Long): Call<List<CitaDTO>>

    @POST("api/citas")
    fun crearCita(@Body dto: CitaInsertarDTO): Call<CitaDTO>

    @PUT("api/citas/{id}/estado")
    fun cambiarEstadoCita(@Path("id") idCita: Long, @Body estado: String): Call<CitaDTO>

    @GET("api/usuarios/veterinarios-clinica/{idClinica}")
    fun getVeterinariosDeClinica(@Path("idClinica") idClinica: Long): Call<List<UsuarioDTO>>

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
}