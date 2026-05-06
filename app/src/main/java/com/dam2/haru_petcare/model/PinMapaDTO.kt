package com.dam2.haru_petcare.model

data class PinMapaDTO(
    val id: Long?,
    val tipo: String?,
    val latitud: Double?,
    val longitud: Double?,
    val descripcion: String?,
    val fechaCreacion: String?,
    val idUsuario: Long?,
    val nombreUsuario: String?
)