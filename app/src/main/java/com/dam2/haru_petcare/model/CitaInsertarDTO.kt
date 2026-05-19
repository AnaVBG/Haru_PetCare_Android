package com.dam2.haru_petcare.model

data class CitaInsertarDTO(
    val fechaCita: String,   // formato "yyyy-MM-ddTHH:mm:ss"
    val motivo: String,
    val idMascota: Long,
    val idVeterinario: Long,
    val idDueno: Long
)