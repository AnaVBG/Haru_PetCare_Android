package com.dam2.haru_petcare.model

import java.time.LocalDate

data class CrearDuenoConMascotaDTO(
    val nombre: String,
    val email: String,
    val password: String,
    val telefono: String,
    val nombreMascota: String,
    val especie: String,
    val raza: String,
    val fechaNacimiento: LocalDate,
    val idClinica: Long
)