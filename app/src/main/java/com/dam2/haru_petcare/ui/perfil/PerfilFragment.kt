package com.dam2.haru_petcare.ui.perfil

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.dam2.haru_petcare.databinding.FragmentPerfilBinding
import com.dam2.haru_petcare.ui.main.MainActivity
import com.dam2.haru_petcare.util.SessionManager
import com.dam2.haru_petcare.util.ThemeManager

class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        mostrarDatosPerfil()
        configurarListeners()
    }

    private fun mostrarDatosPerfil() {
        val nombre   = sessionManager.getNombre() ?: "Usuario"
        val email    = sessionManager.getEmail()  ?: "-"
        val telefono = sessionManager.getTelefono() ?: "-"
        val rol      = sessionManager.getRol()    ?: "DUENO"

        // Inicial del nombre para el avatar
        binding.tvAvatarInicial.text = nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "U"

        binding.tvNombrePerfil.text     = nombre
        binding.tvEmailPerfil.text      = email
        binding.tvTelefonoPerfil.text   = telefono
        binding.tvTipoCuentaPerfil.text = if (rol == "VETERINARIO") "Veterinario" else "Dueño de mascotas"

        // Texto del rol bajo el nombre
        binding.tvRolPerfil.text = if (rol == "VETERINARIO") "Cuenta veterinaria" else "Cuenta personal"
    }

    private fun configurarListeners() {
        binding.layoutCambiarTema.setOnClickListener {
            mostrarDialogoTema()
        }

        binding.btnCerrarSesionPerfil.setOnClickListener {
            mostrarDialogoCerrarSesion()
        }
    }

    private fun mostrarDialogoTema() {
        val opciones = arrayOf(
            "Seguir ajuste del sistema",
            "Modo claro",
            "Modo oscuro"
        )
        val modoActual = ThemeManager.getModoGuardado(requireContext())
        AlertDialog.Builder(requireContext())
            .setTitle("Apariencia")
            .setSingleChoiceItems(opciones, modoActual) { dialog, which ->
                ThemeManager.cambiarModo(requireContext(), which)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoCerrarSesion() {
        AlertDialog.Builder(requireContext())
            .setTitle("Cerrar sesión")
            .setMessage("¿Seguro que quieres salir?")
            .setPositiveButton("Salir") { _, _ ->
                (requireActivity() as MainActivity).cerrarSesion()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}