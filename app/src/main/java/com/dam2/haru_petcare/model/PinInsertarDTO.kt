package com.dam2.haru_petcare.model

data class PinInsertarDTO(
    val tipo: String,
    val latitud: Double,
    val longitud: Double,
    val descripcion: String,
    val idUsuario: Long
)
