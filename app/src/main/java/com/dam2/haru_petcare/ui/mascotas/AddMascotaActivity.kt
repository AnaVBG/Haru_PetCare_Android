package com.dam2.haru_petcare.ui.mascotas

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dam2.haru_petcare.databinding.ActivityAddMascotaBinding
import com.dam2.haru_petcare.model.MascotaDTO
import com.dam2.haru_petcare.model.MascotaInsertarDTO
import com.dam2.haru_petcare.network.HaruApiService
import com.dam2.haru_petcare.network.RetrofitClient
import com.dam2.haru_petcare.util.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDate
import java.util.Calendar

class AddMascotaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddMascotaBinding
    private lateinit var sessionManager: SessionManager

    // Guardamos la fecha seleccionada en formato ISO (YYYY-MM-DD)
    // que es lo que espera el backend
    private var fechaSeleccionada: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddMascotaBinding.inflate(layoutInflater)
        sessionManager = SessionManager(this)
        setContentView(binding.root)

        configurarToolbar()
        configurarDatePicker()
        configurarBotonGuardar()
    }

    private fun configurarToolbar() {
        setSupportActionBar(binding.toolbarAddMascota)
        binding.toolbarAddMascota.setNavigationOnClickListener { finish() }
    }

    /**
     * Configura el campo de fecha para abrir el DatePicker del sistema
     * al pulsarlo, en vez de dejar que el usuario escriba manualmente.
     *
     * DatePickerDialog: diálogo nativo de Android para seleccionar fechas.
     * Usamos Calendar para inicializarlo en la fecha actual.
     */
    private fun configurarDatePicker() {
        val abrirDatePicker = View.OnClickListener {
            val calendario = Calendar.getInstance()

            DatePickerDialog(
                this,
                { _, anio, mes, dia ->
                    // mes + 1 porque Calendar usa 0-11 para los meses
                    // pero nosotros queremos 1-12
                    val mesFormateado = String.format("%02d", mes + 1)
                    val diaFormateado = String.format("%02d", dia)

                    // Guardamos en formato ISO para el backend: YYYY-MM-DD
                    fechaSeleccionada = "$anio-$mesFormateado-$diaFormateado"

                    // Mostramos en formato legible para el usuario: DD/MM/YYYY
                    binding.etFechaNacimiento.setText("$diaFormateado/$mesFormateado/$anio")
                },
                calendario.get(Calendar.YEAR),
                calendario.get(Calendar.MONTH),
                calendario.get(Calendar.DAY_OF_MONTH)
            ).also { picker ->
                // No permitimos fechas futuras — una mascota no puede
                // nacer en el futuro
                picker.datePicker.maxDate = System.currentTimeMillis()
            }.show()
        }

        // El DatePicker se abre tanto al pulsar el campo como el icono
        binding.etFechaNacimiento.setOnClickListener(abrirDatePicker)
        binding.tilFechaNacimiento.setEndIconOnClickListener(abrirDatePicker)
    }

    private fun configurarBotonGuardar() {
        binding.btnGuardarMascota.setOnClickListener { intentarGuardar() }
    }

    private fun intentarGuardar() {
        val nombre  = binding.etNombreMascota.text.toString().trim()
        val especie = binding.etEspecie.text.toString().trim()
        val raza    = binding.etRaza.text.toString().trim()

        // Validaciones
        limpiarErrores()

        if (nombre.isEmpty()) {
            binding.tilNombreMascota.error = "El nombre es obligatorio"
            return
        }
        if (especie.isEmpty()) {
            binding.tilEspecie.error = "La especie es obligatoria"
            return
        }
        if (fechaSeleccionada.isEmpty()) {
            binding.tilFechaNacimiento.error = "Selecciona la fecha de nacimiento"
            return
        }

        mostrarCargando(true)

        val dto = MascotaInsertarDTO(
            nombre = nombre,
            especie = especie,
            raza = raza.ifEmpty { "Desconocida" },
            fechaNacimiento = LocalDate.parse(fechaSeleccionada),
            duenoId = sessionManager.getIdUsuario()
        )

        val api = RetrofitClient
            .getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)

        api.insertarMascota(dto).enqueue(object : Callback<MascotaDTO> {

            override fun onResponse(
                call: Call<MascotaDTO>,
                response: Response<MascotaDTO>
            ) {
                mostrarCargando(false)

                if (response.isSuccessful) {
                    val mascotaCreada = response.body()
                    Toast.makeText(
                        this@AddMascotaActivity,
                        "¡${mascotaCreada?.nombre} añadida correctamente! 🐾",
                        Toast.LENGTH_LONG
                    ).show()
                    // Cerramos la Activity y volvemos a MascotasFragment
                    // que recargará la lista automáticamente en onResume
                    setResult(RESULT_OK)
                    finish()
                } else {
                    mostrarError("Error al guardar (${response.code()})")
                }
            }

            override fun onFailure(call: Call<MascotaDTO>, t: Throwable) {
                mostrarCargando(false)
                mostrarError("Sin conexión: ${t.message}")
            }
        })
    }

    private fun limpiarErrores() {
        binding.tilNombreMascota.error = null
        binding.tilEspecie.error = null
        binding.tilFechaNacimiento.error = null
    }

    private fun mostrarCargando(cargando: Boolean) {
        binding.progressBarAniadir.visibility = if (cargando) View.VISIBLE else View.GONE
        binding.btnGuardarMascota.isEnabled = !cargando
    }

    private fun mostrarError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}