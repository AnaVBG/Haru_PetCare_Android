package com.dam2.haru_petcare.model

data class LoginResponseDTO(
    val idUsuario: Long?,
    val nombre: String?,
    val email: String?,
    val rol: String?,
    val telefono: String?,
    val token: String?
)