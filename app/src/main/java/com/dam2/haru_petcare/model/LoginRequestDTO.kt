package com.dam2.haru_petcare.model

/**
 * En Kotlin, 'data class' genera automáticamente:
 * - Constructor con todos los parámetros
 * - toString(), equals(), hashCode(), copy()
 * - Getters (las propiedades 'val' son de solo lectura)
 *
 * Equivale a una clase Java con 20 líneas de boilerplate
 * en solo 1 línea de Kotlin.
 */
data class LoginRequestDTO(
    val email: String,
    val password: String
)