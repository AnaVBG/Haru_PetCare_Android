package com.dam2.haru_petcare.model

data class CitaDTO(
    val id: Long?,
    val fechaCita: String?,
    val motivo: String?,
    val estado: String?,
    val idMascota: Long?,
    val nombreMascota: String?,
    val nombreVeterinario: String?,
    val idDueno: Long?
)