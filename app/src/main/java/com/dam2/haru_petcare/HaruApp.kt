package com.dam2.haru_petcare

import android.app.Application
import com.dam2.haru_petcare.util.ThemeManager

class HaruApp : Application() {

    companion object {
        lateinit var instance: HaruApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        ThemeManager.aplicarTemaGuardado(this)
    }
}