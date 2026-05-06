package com.dam2.haru_petcare.model

data class HistorialMedicoDTO(
    val id: Long?,
    val tipoRegistro: String?,
    val descripcion: String?,
    val fechaRegistro: String?,  // viene como "2024-03-15T10:30:00" del backend
    val idMascota: Long?
)
