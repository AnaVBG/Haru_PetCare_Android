package com.dam2.haru_petcare.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.dam2.haru_petcare.R
import com.dam2.haru_petcare.databinding.ActivityMainBinding
import com.dam2.haru_petcare.ui.alertas.AlertasFragment
import com.dam2.haru_petcare.ui.citas.CitasFragment
import com.dam2.haru_petcare.ui.mapa.MapaFragment
import com.dam2.haru_petcare.ui.mascotas.MascotasFragment
import com.dam2.haru_petcare.util.SessionManager
import com.dam2.haru_petcare.ui.auth.LoginActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding        = ActivityMainBinding.inflate(layoutInflater)
        sessionManager = SessionManager(this)
        setContentView(binding.root)

        // Fragment inicial al arrancar
        if (savedInstanceState == null) {
            cargarFragment(MascotasFragment())
        }

        configurarNavegacion()
    }

    private fun configurarNavegacion() {
        binding.bottomNavView.setOnItemSelectedListener { item ->
            // 'when' es el 'switch' de Kotlin, pero más potente
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_mascotas -> MascotasFragment()
                R.id.nav_citas    -> CitasFragment()
                R.id.nav_mapa     -> MapaFragment()
                R.id.nav_alertas  -> AlertasFragment()
                else              -> return@setOnItemSelectedListener false
                // 'return@setOnItemSelectedListener' es un return etiquetado:
                // sale del lambda, no de la función entera
            }
            cargarFragment(fragment)
            true // indica al BottomNav que marque el ítem como seleccionado
        }
    }

    private fun cargarFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    fun cerrarSesion() {
        sessionManager.cerrarSesion()
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                // 'apply { }' configura el Intent sin necesidad de variable temporal
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                // 'or' en Kotlin = '|' (OR bit a bit) en Java
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}