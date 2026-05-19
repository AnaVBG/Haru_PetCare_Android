package com.dam2.haru_petcare.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dam2.haru_petcare.databinding.ActivityRegisterBinding
import com.dam2.haru_petcare.model.ClinicaDTO
import com.dam2.haru_petcare.model.LoginResponseDTO
import com.dam2.haru_petcare.model.UsuarioRegistroDTO
import com.dam2.haru_petcare.network.HaruApiService
import com.dam2.haru_petcare.network.RetrofitClient
import com.dam2.haru_petcare.ui.main.MainActivity
import com.dam2.haru_petcare.util.Constants
import com.dam2.haru_petcare.util.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var sessionManager: SessionManager

    private var clinicas: List<ClinicaDTO> = emptyList()
    private var idClinicaSeleccionada: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding        = ActivityRegisterBinding.inflate(layoutInflater)
        sessionManager = SessionManager(this)
        setContentView(binding.root)

        configurarListeners()
    }

    private fun configurarListeners() {
        binding.btnRegistrar.setOnClickListener { intentarRegistro() }
        binding.btnVolver.setOnClickListener { finish() }

        binding.toggleGroupRol.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                binding.btnRolVeterinario.id -> {
                    binding.layoutClinica.visibility = View.VISIBLE
                    cargarClinicas()
                }
                else -> {
                    binding.layoutClinica.visibility = View.GONE
                    idClinicaSeleccionada = null
                }
            }
        }
    }

    private fun cargarClinicas() {
        RetrofitClient.getClient().create(HaruApiService::class.java)
            .getClincias()
            .enqueue(object : Callback<List<ClinicaDTO>> {
                override fun onResponse(
                    call: Call<List<ClinicaDTO>>,
                    response: Response<List<ClinicaDTO>>
                ) {
                    if (response.isSuccessful) {
                        clinicas = response.body() ?: emptyList()
                        val nombres = clinicas.map { it.nombre ?: "" }
                        val adapter = ArrayAdapter(
                            this@RegisterActivity,
                            android.R.layout.simple_dropdown_item_1line,
                            nombres
                        )
                        binding.actvClinica.setAdapter(adapter)
                        binding.actvClinica.setOnItemClickListener { _, _, pos, _ ->
                            idClinicaSeleccionada = clinicas[pos].id
                        }
                        if (clinicas.isEmpty()) {
                            binding.tilClinica.helperText =
                                "No hay clínicas registradas todavía"
                        }
                    }
                }
                override fun onFailure(call: Call<List<ClinicaDTO>>, t: Throwable) {
                    binding.tilClinica.helperText = "No se pudieron cargar las clínicas"
                }
            })
    }

    private fun intentarRegistro() {
        val nombre            = binding.etNombre.text.toString().trim()
        val email             = binding.etEmail.text.toString().trim()
        val telefono          = binding.etTelefono.text.toString().trim()
        val password          = binding.etPassword.text.toString().trim()
        val confirmarPassword = binding.etConfirmarPassword.text.toString().trim()

        limpiarErrores()

        if (nombre.isEmpty())   { binding.tilNombre.error = "El nombre es obligatorio"; return }
        if (email.isEmpty())    { binding.tilEmail.error  = "El email es obligatorio";  return }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "El formato del email no es válido"; return
        }
        if (password.isEmpty()) { binding.tilPassword.error = "La contraseña es obligatoria"; return }
        if (password.length < 6) { binding.tilPassword.error = "Mínimo 6 caracteres"; return }
        if (password != confirmarPassword) {
            binding.tilConfirmarPassword.error = "Las contraseñas no coinciden"; return
        }

        val rol = when (binding.toggleGroupRol.checkedButtonId) {
            binding.btnRolVeterinario.id -> Constants.ROL_VETERINARIO
            binding.btnRolClinica.id     -> Constants.ROL_CLINICA
            else                         -> Constants.ROL_DUENO
        }

        val dto = UsuarioRegistroDTO(
            nombre     = nombre,
            email      = email,
            password   = password,
            rol        = rol,
            telefono   = telefono,
            idClinica  = if (rol == Constants.ROL_VETERINARIO) idClinicaSeleccionada else null
        )

        mostrarCargando(true)

        RetrofitClient.getClient().create(HaruApiService::class.java)
            .registrar(dto)
            .enqueue(object : Callback<LoginResponseDTO> {
                override fun onResponse(
                    call: Call<LoginResponseDTO>,
                    response: Response<LoginResponseDTO>
                ) {
                    mostrarCargando(false)
                    if (response.isSuccessful) {
                        response.body()?.let { datos ->
                            sessionManager.guardarSesion(
                                id     = datos.idUsuario ?: -1L,
                                nombre = datos.nombre    ?: "",
                                email  = datos.email     ?: "",
                                rol    = datos.rol       ?: "",
                                token  = datos.token     ?: ""
                            )
                            Toast.makeText(this@RegisterActivity,
                                "¡Bienvenido a Haru, ${datos.nombre}!", Toast.LENGTH_SHORT).show()
                            navegarAMain()
                        }
                    } else {
                        val mensaje = when (response.code()) {
                            409  -> "Este email ya está registrado"
                            400  -> "Datos incorrectos, revisa el formulario"
                            else -> "Error al registrar (${response.code()})"
                        }
                        Toast.makeText(this@RegisterActivity, mensaje, Toast.LENGTH_LONG).show()
                    }
                }
                override fun onFailure(call: Call<LoginResponseDTO>, t: Throwable) {
                    mostrarCargando(false)
                    Toast.makeText(this@RegisterActivity,
                        "Error de conexión: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun limpiarErrores() {
        binding.tilNombre.error            = null
        binding.tilEmail.error             = null
        binding.tilPassword.error          = null
        binding.tilConfirmarPassword.error = null
    }

    private fun mostrarCargando(cargando: Boolean) {
        binding.progressBarRegistro.visibility = if (cargando) View.VISIBLE else View.GONE
        binding.btnRegistrar.isEnabled         = !cargando
    }

    private fun navegarAMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }
}