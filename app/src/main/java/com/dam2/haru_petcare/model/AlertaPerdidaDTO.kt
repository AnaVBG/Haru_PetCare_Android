package com.dam2.haru_petcare.model

data class AlertaPerdidaDTO(
    val id: Long?,
    val ultimaUbicacionLat: Double?,
    val ultimaUbicacionLng: Double?,
    val mensajeAdicional: String?,
    val activa: Boolean?,
    val fechaAlerta: String?,
    val nombreMascota: String?,
    val fotoUrlMascota: String?,
    val nombreDueno: String?,
    val telefonoDueno: String?
)
