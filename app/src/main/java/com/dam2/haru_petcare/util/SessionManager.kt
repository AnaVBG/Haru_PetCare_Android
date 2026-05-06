package com.dam2.haru_petcare.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestiona la sesión del usuario con SharedPreferences.
 *
 * En Kotlin las propiedades se declaran en el constructor primario.
 * 'private val' crea el campo y lo asigna en una sola línea.
 */
class SessionManager(context: Context) {

    companion object {
        // 'companion object' es el equivalente Kotlin de los campos 'static' de Java
        private const val PREF_NAME    = "haru_session"
        private const val KEY_TOKEN    = "jwt_token"
        private const val KEY_ID       = "usuario_id"
        private const val KEY_NOMBRE   = "usuario_nombre"
        private const val KEY_EMAIL    = "usuario_email"
        private const val KEY_ROL      = "usuario_rol"
        private const val KEY_LOGUEADO = "logueado"
    }

    // En Kotlin podemos inicializar propiedades directamente, sin constructor
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    /**
     * Guarda todos los datos tras un login exitoso.
     * 'apply { }' es un scope function de Kotlin: ejecuta el bloque
     * sobre el receptor (el editor) y devuelve el propio objeto.
     * Es más limpio que encadenar .putX().putX().apply()
     */
    fun guardarSesion(id: Long, nombre: String, email: String,
                      rol: String, token: String) {
        prefs.edit().apply {
            putBoolean(KEY_LOGUEADO, true)
            putLong(KEY_ID,          id)
            putString(KEY_NOMBRE,    nombre)
            putString(KEY_EMAIL,     email)
            putString(KEY_ROL,       rol)
            putString(KEY_TOKEN,     token)
            apply() // commit asíncrono
        }
    }

    // En Kotlin, las funciones de una sola expresión se pueden escribir con '='
    fun getToken(): String?   = prefs.getString(KEY_TOKEN, null)
    fun getIdUsuario(): Long  = prefs.getLong(KEY_ID, -1L)
    fun getNombre(): String   = prefs.getString(KEY_NOMBRE, "") ?: ""
    fun getRol(): String      = prefs.getString(KEY_ROL, "") ?: ""
    fun estaLogueado(): Boolean = prefs.getBoolean(KEY_LOGUEADO, false)

    fun cerrarSesion() {
        prefs.edit().clear().apply()
    }
}