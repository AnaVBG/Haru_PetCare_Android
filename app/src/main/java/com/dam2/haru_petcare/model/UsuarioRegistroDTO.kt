package com.dam2.haru_petcare.model

data class UsuarioRegistroDTO(
    val nombre: String,
    val email: String,
    val password: String,
    val rol: String,
    val telefono: String,
    val idClinica: Long? = 0
)