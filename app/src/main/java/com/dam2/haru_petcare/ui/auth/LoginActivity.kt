package com.dam2.haru_petcare.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dam2.haru_petcare.databinding.ActivityLoginBinding
import com.dam2.haru_petcare.model.LoginRequestDTO
import com.dam2.haru_petcare.model.LoginResponseDTO
import com.dam2.haru_petcare.network.HaruApiService
import com.dam2.haru_petcare.network.RetrofitClient
import com.dam2.haru_petcare.ui.main.MainActivity
import com.dam2.haru_petcare.util.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    // En Kotlin, 'lateinit var' indica que la variable se inicializará
    // antes de usarse, pero no en el constructor. Es la forma idiomática
    // de manejar el binding en Activities.
    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding        = ActivityLoginBinding.inflate(layoutInflater)
        sessionManager = SessionManager(this)
        setContentView(binding.root) // Kotlin: .root en vez de .getRoot()

        // Si ya hay sesión activa, saltamos directamente a MainActivity
        if (sessionManager.estaLogueado()) {
            navegarAMain()
            return
        }

        configurarListeners()
    }

    private fun configurarListeners() {
        // En Kotlin, los listeners se pasan como lambdas — mucho más limpio
        // que 'new OnClickListener() { @Override void onClick... }'
        binding.btnLogin.setOnClickListener { intentarLogin() }

        binding.btnIrARegistro.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun intentarLogin() {
        // 'trim()' elimina espacios al inicio y al final
        val email    = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Validación — el operador 'return' dentro de una función
        // corta la ejecución igual que en Java
        if (email.isEmpty()) {
            binding.tilEmail.error = "El email es obligatorio"
            return
        }
        if (password.isEmpty()) {
            binding.tilPassword.error = "La contraseña es obligatoria"
            return
        }

        // Limpiamos errores previos asignando null
        binding.tilEmail.error    = null
        binding.tilPassword.error = null

        mostrarCargando(true)

        // Creamos el DTO con la data class — no hace falta 'new'
        val loginDto = LoginRequestDTO(email, password)

        // Creamos el servicio sin token (login es endpoint público)
        val api = RetrofitClient.getClient().create(HaruApiService::class.java)

        api.login(loginDto).enqueue(object : Callback<LoginResponseDTO> {
            // 'object : Callback<T>' es la forma Kotlin de implementar
            // una interfaz anónima — equivale a 'new Callback<T>() {}' en Java

            override fun onResponse(
                call: Call<LoginResponseDTO>,
                response: Response<LoginResponseDTO>
            ) {
                mostrarCargando(false)

                if (response.isSuccessful) {
                    // El operador '?.' (safe call) llama a la función
                    // solo si el objeto no es null — evita NPE sin if
                    response.body()?.let { datos ->
                        // 'let { }' ejecuta el bloque con 'datos' como receptor
                        // solo si body() no era null
                        sessionManager.guardarSesion(
                            id       = datos.idUsuario  ?: -1L,
                            nombre   = datos.nombre     ?: "",
                            email    = datos.email      ?: "",
                            rol      = datos.rol        ?: "",
                            token    = datos.token      ?: "",
                            telefono = datos.telefono   ?: ""
                        )
                        navegarAMain()
                    } ?: run {
                        // Si body() era null pese a isSuccessful (raro pero posible)
                        mostrarError("Respuesta vacía del servidor")
                    }
                } else {
                    mostrarError("Credenciales incorrectas (${response.code()})")
                }
            }

            override fun onFailure(call: Call<LoginResponseDTO>, t: Throwable) {
                mostrarCargando(false)
                mostrarError("Error de conexión: ${t.message}")
                // Si ves "Failed to connect to /10.0.2.2:8080"
                // → el servidor Spring Boot no está arrancado
            }
        })
    }

    private fun mostrarCargando(cargando: Boolean) {
        // Kotlin permite el operador ternario inline con 'if'
        binding.progressBarLogin.visibility = if (cargando) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !cargando
    }

    private fun mostrarError(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
    }

    private fun navegarAMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish() // Cierra LoginActivity para que no quede en la pila
    }

    override fun onDestroy() {
        super.onDestroy()
        // En Kotlin el binding se anula así para evitar memory leaks
        // (técnicamente deberíamos usar una var nullable, pero con
        // lateinit esto es suficiente para el TFG)
    }
}