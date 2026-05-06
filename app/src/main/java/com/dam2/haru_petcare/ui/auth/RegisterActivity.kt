package com.dam2.haru_petcare.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dam2.haru_petcare.databinding.ActivityRegisterBinding
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

/**
 * RegisterActivity: pantalla de registro de nuevo usuario.
 *
 * Llama a POST /api/auth/registro.
 * Si el registro es exitoso, el backend devuelve directamente
 * el JWT igual que en el login — así el usuario queda logueado
 * automáticamente sin tener que volver a la pantalla de login.
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding        = ActivityRegisterBinding.inflate(layoutInflater)
        sessionManager = SessionManager(this)
        setContentView(binding.root)

        configurarListeners()
    }

    private fun configurarListeners() {
        binding.btnRegistrar.setOnClickListener { intentarRegistro() }

        // Volver al login cerrando esta Activity
        binding.btnVolver.setOnClickListener { finish() }
    }

    private fun intentarRegistro() {
        // 1. Leer campos
        val nombre            = binding.etNombre.text.toString().trim()
        val email             = binding.etEmail.text.toString().trim()
        val telefono          = binding.etTelefono.text.toString().trim()
        val password          = binding.etPassword.text.toString().trim()
        val confirmarPassword = binding.etConfirmarPassword.text.toString().trim()

        // 2. Validaciones — limpiamos errores anteriores primero
        limpiarErrores()

        if (nombre.isEmpty()) {
            binding.tilNombre.error = "El nombre es obligatorio"
            return
        }
        if (email.isEmpty()) {
            binding.tilEmail.error = "El email es obligatorio"
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            // Patterns.EMAIL_ADDRESS es una clase de Android que valida
            // el formato del email con una expresión regular estándar
            binding.tilEmail.error = "El formato del email no es válido"
            return
        }
        if (password.isEmpty()) {
            binding.tilPassword.error = "La contraseña es obligatoria"
            return
        }
        if (password.length < 6) {
            binding.tilPassword.error = "Mínimo 6 caracteres"
            return
        }
        if (password != confirmarPassword) {
            binding.tilConfirmarPassword.error = "Las contraseñas no coinciden"
            return
        }

        // 3. Determinar el rol seleccionado en el ToggleGroup
        val rol = when (binding.toggleGroupRol.checkedButtonId) {
            binding.btnRolVeterinario.id -> Constants.ROL_VETERINARIO
            else                         -> Constants.ROL_DUENO
        }

        // 4. Construir el DTO y llamar a la API
        val dto = UsuarioRegistroDTO(
            nombre   = nombre,
            email    = email,
            password = password,
            rol      = rol,
            telefono = telefono
        )

        mostrarCargando(true)

        // Registro es endpoint público — sin token
        val api = RetrofitClient.getClient().create(HaruApiService::class.java)

        api.registrar(dto).enqueue(object : Callback<LoginResponseDTO> {

            override fun onResponse(
                call: Call<LoginResponseDTO>,
                response: Response<LoginResponseDTO>
            ) {
                mostrarCargando(false)

                if (response.isSuccessful) {
                    response.body()?.let { datos ->
                        // Registro exitoso → guardamos sesión y vamos a MainActivity
                        // El usuario no necesita hacer login por separado
                        sessionManager.guardarSesion(
                            id     = datos.idUsuario ?: -1L,
                            nombre = datos.nombre    ?: "",
                            email  = datos.email     ?: "",
                            rol    = datos.rol       ?: "",
                            token  = datos.token     ?: ""
                        )
                        Toast.makeText(
                            this@RegisterActivity,
                            "¡Bienvenido a Haru, ${datos.nombre}!",
                            Toast.LENGTH_SHORT
                        ).show()
                        navegarAMain()
                    }
                } else {
                    // 409 Conflict = email ya registrado (según tu AuthService)
                    val mensaje = when (response.code()) {
                        409  -> "Este email ya está registrado"
                        400  -> "Datos incorrectos, revisa el formulario"
                        else -> "Error al registrar (${response.code()})"
                    }
                    Toast.makeText(
                        this@RegisterActivity, mensaje, Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<LoginResponseDTO>, t: Throwable) {
                mostrarCargando(false)
                Toast.makeText(
                    this@RegisterActivity,
                    "Error de conexión: ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
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
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }
}