package com.dam2.haru_petcare.model

data class AlertaInsertarDTO(
    val ultimaUbicacionLat: Double,
    val ultimaUbicacionLng: Double,
    val mensajeAdicional: String,
    val idMascota: Long,
    val idUsuario: Long
)
