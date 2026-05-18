package com.dam2.haru_petcare

import android.app.Application
import com.dam2.haru_petcare.util.ThemeManager

/**
 * En Kotlin, 'class' hereda con ':' en vez de 'extends'.
 * No hay llaves de constructor si no añadimos nada al onCreate.
 */
class HaruApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemeManager.aplicarTemaGuardado(this)
    }
}