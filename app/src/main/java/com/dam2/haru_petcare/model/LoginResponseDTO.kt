package com.dam2.haru_petcare.model

/**
 * El '?' después del tipo indica que el campo puede ser null.
 * Kotlin nos OBLIGA a manejar los nulos explícitamente,
 * evitando NullPointerExceptions en tiempo de ejecución.
 *
 * Gson puede rellenar estos campos con null si no vienen en el JSON,
 * por eso los marcamos como nullable con '?'
 */
data class LoginResponseDTO(
    val idUsuario: Long?,
    val nombre: String?,
    val email: String?,
    val rol: String?,
    val token: String?
)