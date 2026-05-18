package com.dam2.haru_petcare.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * ThemeManager: guarda y aplica la preferencia de tema del usuario.
 *
 * AppCompatDelegate.setDefaultNightMode() cambia el tema globalmente
 * en toda la app sin necesidad de reiniciar las Activities.
 *
 * Modos disponibles:
 * - MODE_NIGHT_NO:        siempre claro
 * - MODE_NIGHT_YES:       siempre oscuro
 * - MODE_NIGHT_FOLLOW_SYSTEM: sigue el ajuste del sistema (recomendado)
 */
object ThemeManager {

    private const val PREF_NAME  = "haru_theme"
    private const val KEY_TEMA   = "modo_oscuro"

    // Valores posibles del modo guardado
    const val MODO_SISTEMA = 0
    const val MODO_CLARO   = 1
    const val MODO_OSCURO  = 2

    /**
     * Aplica el tema guardado al arrancar la app.
     * Llamar desde HaruApp.onCreate() para que se aplique antes
     * de que se cree cualquier Activity.
     */
    fun aplicarTemaGuardado(context: Context) {
        val modo = getModoGuardado(context)
        aplicarModo(modo)
    }

    /**
     * Cambia el tema y guarda la preferencia.
     * El cambio es inmediato — no hace falta reiniciar la app.
     */
    fun cambiarModo(context: Context, modo: Int) {
        guardarModo(context, modo)
        aplicarModo(modo)
    }

    fun getModoGuardado(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_TEMA, MODO_SISTEMA)
    }

    private fun guardarModo(context: Context, modo: Int) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_TEMA, modo)
            .apply()
    }

    private fun aplicarModo(modo: Int) {
        val nightMode = when (modo) {
            MODO_CLARO  -> AppCompatDelegate.MODE_NIGHT_NO
            MODO_OSCURO -> AppCompatDelegate.MODE_NIGHT_YES
            else        -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
}