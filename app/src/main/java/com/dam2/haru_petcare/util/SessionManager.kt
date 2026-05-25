package com.dam2.haru_petcare.util

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    companion object {
        private const val PREF_NAME    = "haru_session"
        private const val KEY_TOKEN    = "jwt_token"
        private const val KEY_ID       = "usuario_id"
        private const val KEY_NOMBRE   = "usuario_nombre"
        private const val KEY_EMAIL    = "usuario_email"
        private const val KEY_ROL      = "usuario_rol"
        private const val KEY_TELEFONO = "usuario_telefono"
        private const val KEY_LOGUEADO = "logueado"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun guardarSesion(
        id: Long,
        nombre: String,
        email: String,
        rol: String,
        token: String,
        telefono: String = ""
    ) {
        prefs.edit().apply {
            putBoolean(KEY_LOGUEADO,  true)
            putLong(KEY_ID,           id)
            putString(KEY_NOMBRE,     nombre)
            putString(KEY_EMAIL,      email)
            putString(KEY_ROL,        rol)
            putString(KEY_TOKEN,      token)
            putString(KEY_TELEFONO,   telefono)
            apply()
        }
    }

    fun getToken(): String?     = prefs.getString(KEY_TOKEN, null)
    fun getId(): Long           = prefs.getLong(KEY_ID, -1L)
    fun getIdUsuario(): Long    = prefs.getLong(KEY_ID, -1L)
    fun getNombre(): String     = prefs.getString(KEY_NOMBRE, "") ?: ""
    fun getEmail(): String      = prefs.getString(KEY_EMAIL, "") ?: ""
    fun getRol(): String        = prefs.getString(KEY_ROL, "") ?: ""
    fun getTelefono(): String   = prefs.getString(KEY_TELEFONO, "") ?: ""
    fun estaLogueado(): Boolean = prefs.getBoolean(KEY_LOGUEADO, false)

    fun cerrarSesion() {
        prefs.edit().clear().apply()
    }
}