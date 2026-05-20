package com.dam2.haru_petcare.model

data class UsuarioDTO(
    val id: Long?,
    val nombre: String?,
    val email: String?,
    val rol: String?,
    val telefono: String?,
    val totalMascotas: Int?
)