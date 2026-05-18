package com.dam2.haru_petcare.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.dam2.haru_petcare.R
import com.dam2.haru_petcare.databinding.ActivityMainBinding
import com.dam2.haru_petcare.ui.alertas.AlertasFragment
import com.dam2.haru_petcare.ui.citas.CitasFragment
import com.dam2.haru_petcare.ui.mapa.MapaFragment
import com.dam2.haru_petcare.ui.mascotas.MascotaFragment
import com.dam2.haru_petcare.util.SessionManager
import com.dam2.haru_petcare.ui.auth.LoginActivity
import com.dam2.haru_petcare.util.ThemeManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager

    private val permisosNotificacionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> /* el usuario decidió — acepte o no, continuamos */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        sessionManager = SessionManager(this)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarMain)
        pedirPermisoNotificaciones()

        if (savedInstanceState == null) cargarFragment(MascotaFragment())
        configurarNavegacion()
        configurarMenu()
    }

    private fun configurarMenu() {
        binding.toolbarMain.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_tema -> {
                    mostrarDialogoTema()
                    true
                }
                R.id.action_cerrar_sesion -> {
                    mostrarDialogoCerrarSesion()
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Diálogo con tres opciones: Sistema, Claro, Oscuro.
     * La opción actual aparece marcada con un radio button.
     */
    private fun mostrarDialogoTema() {
        val opciones = arrayOf(
            "Seguir ajuste del sistema",
            "Modo claro",
            "Modo oscuro"
        )

        val modoActual = ThemeManager.getModoGuardado(this)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Apariencia")
            .setSingleChoiceItems(opciones, modoActual) { dialog, which ->
                ThemeManager.cambiarModo(this, which)
                dialog.dismiss()
                // No hace falta recrear la Activity —
                // AppCompatDelegate lo gestiona automáticamente
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoCerrarSesion() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Seguro que quieres salir?")
            .setPositiveButton("Salir") { _, _ -> cerrarSesion() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Añade este método y llámalo desde onCreate()
    private fun pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permisosNotificacionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun configurarNavegacion() {
        binding.bottomNavView.setOnItemSelectedListener { item ->
            // 'when' es el 'switch' de Kotlin, pero más potente
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_mascotas -> MascotaFragment()
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