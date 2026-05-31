package com.dam2.haru_petcare.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.dam2.haru_petcare.R
import com.dam2.haru_petcare.databinding.ActivityMainBinding
import com.dam2.haru_petcare.ui.alertas.AlertasFragment
import com.dam2.haru_petcare.ui.auth.LoginActivity
import com.dam2.haru_petcare.ui.mascotas.MascotaFragment
import com.dam2.haru_petcare.ui.mapa.MapaFragment
import com.dam2.haru_petcare.ui.mascotas.VetMascotasFragment
import com.dam2.haru_petcare.ui.perfil.PerfilFragment
import com.dam2.haru_petcare.ui.salud.SaludFragment
import com.dam2.haru_petcare.util.SessionManager
import com.dam2.haru_petcare.util.ThemeManager

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager

    private val permisosNotificacionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        sessionManager = SessionManager(this)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarMain)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        pedirPermisoNotificaciones()

        if (savedInstanceState == null) {
            val rol = sessionManager.getRol()
            val fragmentoInicio = if (rol == "CLINICA" || rol == "VETERINARIO") VetMascotasFragment() else MascotaFragment()
            cargarFragment(fragmentoInicio)
            supportActionBar?.title = "Inicio"
        }

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

    private fun configurarNavegacion() {
        binding.bottomNavView.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_inicio -> {
                    val rol = sessionManager.getRol()
                    if (rol == "CLINICA" || rol == "VETERINARIO") VetMascotasFragment() else MascotaFragment()
                }
                R.id.nav_salud -> {
                    SaludFragment()
                }
                R.id.nav_mapa -> {
                    MapaFragment()
                }
                R.id.nav_alertas -> {
                    AlertasFragment()
                }
                R.id.nav_perfil -> {
                    PerfilFragment()
                }
                else -> return@setOnItemSelectedListener false
            }
            cargarFragment(fragment)
            true
        }
    }

    fun cargarFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

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

    private fun mostrarDialogoTema() {
        val opciones = arrayOf(
            "Seguir ajuste del sistema",
            "Modo claro",
            "Modo oscuro"
        )
        val modoActual = ThemeManager.getModoGuardado(this)
        AlertDialog.Builder(this)
            .setTitle("Apariencia")
            .setSingleChoiceItems(opciones, modoActual) { dialog, which ->
                ThemeManager.cambiarModo(this, which)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoCerrarSesion() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Seguro que quieres salir?")
            .setPositiveButton("Salir") { _, _ -> cerrarSesion() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    fun cerrarSesion() {
        sessionManager.cerrarSesion()
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}