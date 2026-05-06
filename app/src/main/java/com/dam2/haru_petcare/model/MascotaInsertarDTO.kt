package com.dam2.haru_petcare.model

data class MascotaInsertarDTO(
    val nombre: String,
    val especie: String,
    val raza: String,
    val fechaNacimiento: String,
    val duenoId: Long
)