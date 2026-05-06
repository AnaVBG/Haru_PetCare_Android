package com.dam2.haru_petcare.util

/**
 * Constantes globales de la app.
 * En Kotlin usamos 'object' en lugar de una clase con constructor privado.
 * 'object' crea un Singleton automáticamente — no hace falta instanciarlo.
 */
object Constants {
    // Emulador → 10.0.2.2 | Dispositivo físico → IP local de tu PC
    const val BASE_URL = "http://192.168.0.15:8080/"

    const val ROL_DUENO       = "DUENO"
    const val ROL_VETERINARIO = "VETERINARIO"

    // Claves para pasar datos entre Activities con Intent
    const val EXTRA_MASCOTA_ID = "mascota_id"
    const val EXTRA_CITA_ID    = "cita_id"
}