package com.dam2.haru_petcare.model

import java.time.LocalDate

data class MascotaInsertarDTO(
    val nombre: String,
    val especie: String,
    val raza: String,
    val fechaNacimiento: LocalDate,
    val duenoId: Long
)