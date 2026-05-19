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
import com.dam2.haru_petcare.databinding.DialogPerfilBinding
import com.dam2.haru_petcare.ui.alertas.AlertasFragment
import com.dam2.haru_petcare.ui.citas.CitasFragment
import com.dam2.haru_petcare.ui.mapa.MapaFragment
import com.dam2.haru_petcare.ui.mascotas.MascotaFragment
import com.dam2.haru_petcare.ui.mascotas.VetMascotasFragment
import com.dam2.haru_petcare.util.Constants
import com.dam2.haru_petcare.util.SessionManager
import com.dam2.haru_petcare.ui.auth.LoginActivity
import com.dam2.haru_petcare.util.ThemeManager
import com.google.android.material.bottomsheet.BottomSheetDialog

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
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
        binding.toolbarMain.inflateMenu(R.menu.menu_main)
        pedirPermisoNotificaciones()

        if (savedInstanceState == null) cargarFragment(fragmentoMascotas())
        configurarNavegacion()
        configurarMenu()

        val tabDestino = intent.getIntExtra("tab_destino", -1)
        if (tabDestino != -1) binding.bottomNavView.selectedItemId = tabDestino
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tabDestino = intent.getIntExtra("tab_destino", -1)
        if (tabDestino != -1) binding.bottomNavView.selectedItemId = tabDestino
    }

    private fun configurarMenu() {
        binding.toolbarMain.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_perfil -> { mostrarPerfilSheet(); true }
                else -> false
            }
        }
    }

    private fun mostrarPerfilSheet() {
        val sheet = BottomSheetDialog(this)
        val sheetBinding = DialogPerfilBinding.inflate(layoutInflater)
        sheet.setContentView(sheetBinding.root)

        // Rellenar datos
        val nombre = sessionManager.getNombre()
        sheetBinding.tvInicialAvatar.text  = nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        sheetBinding.tvNombrePerfil.text   = nombre
        sheetBinding.tvEmailPerfil.text    = sessionManager.getEmail()
        sheetBinding.tvRolPerfil.text      = when (sessionManager.getRol()) {
            Constants.ROL_VETERINARIO -> "Veterinario/a"
            Constants.ROL_CLINICA     -> "Clínica"
            else                      -> "Dueño/a"
        }

        // Marcar el tema activo
        val modoActual = ThemeManager.getModoGuardado(this)
        val botonActivo = when (modoActual) {
            ThemeManager.MODO_CLARO  -> sheetBinding.btnTemaClaro.id
            ThemeManager.MODO_OSCURO -> sheetBinding.btnTemaOscuro.id
            else                     -> sheetBinding.btnTemaSistema.id
        }
        sheetBinding.toggleTema.check(botonActivo)

        // Cambio de tema
        sheetBinding.toggleTema.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val modo = when (checkedId) {
                sheetBinding.btnTemaClaro.id  -> ThemeManager.MODO_CLARO
                sheetBinding.btnTemaOscuro.id -> ThemeManager.MODO_OSCURO
                else                          -> ThemeManager.MODO_SISTEMA
            }
            ThemeManager.cambiarModo(this, modo)
        }

        // Cerrar sesión
        sheetBinding.btnCerrarSesionPerfil.setOnClickListener {
            sheet.dismiss()
            mostrarDialogoCerrarSesion()
        }

        sheet.show()
    }

    private fun mostrarDialogoCerrarSesion() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Seguro que quieres salir?")
            .setPositiveButton("Salir") { _, _ -> cerrarSesion() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun fragmentoMascotas(): Fragment =
        if (sessionManager.getRol() == Constants.ROL_DUENO) MascotaFragment()
        else VetMascotasFragment()

    private fun configurarNavegacion() {
        binding.bottomNavView.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_mascotas -> fragmentoMascotas()
                R.id.nav_citas    -> CitasFragment()
                R.id.nav_mapa     -> MapaFragment()
                R.id.nav_alertas  -> AlertasFragment()
                else              -> return@setOnItemSelectedListener false
            }
            cargarFragment(fragment)
            true
        }
    }

    private fun cargarFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    fun cerrarSesion() {
        sessionManager.cerrarSesion()
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
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

    override fun onDestroy() { super.onDestroy() }
}