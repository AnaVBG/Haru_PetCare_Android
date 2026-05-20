package com.dam2.haru_petcare.ui.mascotas

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dam2.haru_petcare.databinding.ActivityCrearDuenoMascotaBinding
import com.dam2.haru_petcare.model.CrearDuenoConMascotaDTO
import com.dam2.haru_petcare.model.MascotaDTO
import com.dam2.haru_petcare.network.HaruApiService
import com.dam2.haru_petcare.network.RetrofitClient
import com.dam2.haru_petcare.util.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDate
import java.util.Calendar

class CrearDuenoMascotaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCrearDuenoMascotaBinding
    private lateinit var sessionManager: SessionManager
    private var fechaSeleccionada: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearDuenoMascotaBinding.inflate(layoutInflater)
        sessionManager = SessionManager(this)
        setContentView(binding.root)

        binding.toolbarCrearDuenoMascota.setNavigationOnClickListener { finish() }

        val abrirDatePicker = View.OnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, anio, mes, dia ->
                    val m = String.format("%02d", mes + 1)
                    val d = String.format("%02d", dia)
                    fechaSeleccionada = "$anio-$m-$d"
                    binding.etFechaNacimiento.setText("$d/$m/$anio")
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).also { it.datePicker.maxDate = System.currentTimeMillis() }.show()
        }
        binding.etFechaNacimiento.setOnClickListener(abrirDatePicker)
        binding.tilFechaNacimiento.setEndIconOnClickListener(abrirDatePicker)

        binding.btnGuardarDuenoMascota.setOnClickListener { intentarGuardar() }
    }

    private fun intentarGuardar() {
        val nombre    = binding.etNombreDueno.text.toString().trim()
        val email     = binding.etEmailDueno.text.toString().trim()
        val telefono  = binding.etTelefonoDueno.text.toString().trim()
        val password  = binding.etPasswordDueno.text.toString().trim()
        val nomMascota = binding.etNombreMascota.text.toString().trim()
        val especie   = binding.etEspecie.text.toString().trim()
        val raza      = binding.etRaza.text.toString().trim()

        // Validaciones
        binding.tilNombreDueno.error   = null
        binding.tilEmailDueno.error    = null
        binding.tilPasswordDueno.error = null
        binding.tilNombreMascota.error = null
        binding.tilEspecie.error       = null
        binding.tilFechaNacimiento.error = null

        if (nombre.isEmpty())     { binding.tilNombreDueno.error = "Obligatorio"; return }
        if (email.isEmpty())      { binding.tilEmailDueno.error  = "Obligatorio"; return }
        if (password.isEmpty())   { binding.tilPasswordDueno.error = "Obligatorio"; return }
        if (nomMascota.isEmpty()) { binding.tilNombreMascota.error = "Obligatorio"; return }
        if (especie.isEmpty())    { binding.tilEspecie.error = "Obligatorio"; return }
        if (fechaSeleccionada.isEmpty()) {
            binding.tilFechaNacimiento.error = "Selecciona una fecha"; return
        }

        mostrarCargando(true)

        val dto = CrearDuenoConMascotaDTO(
            nombre         = nombre,
            email          = email,
            password       = password,
            telefono       = telefono,
            nombreMascota  = nomMascota,
            especie        = especie,
            raza           = raza.ifEmpty { "Desconocida" },
            fechaNacimiento = LocalDate.parse(fechaSeleccionada),
            idClinica      = sessionManager.getIdUsuario()
        )

        RetrofitClient.getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)
            .crearDuenoConMascota(dto)
            .enqueue(object : Callback<MascotaDTO> {

                override fun onResponse(call: Call<MascotaDTO>, response: Response<MascotaDTO>) {
                    mostrarCargando(false)
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@CrearDuenoMascotaActivity,
                            "¡${response.body()?.nombre} añadida a la clínica!",
                            Toast.LENGTH_LONG
                        ).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(
                            this@CrearDuenoMascotaActivity,
                            "Error al crear (${response.code()})",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<MascotaDTO>, t: Throwable) {
                    mostrarCargando(false)
                    Toast.makeText(
                        this@CrearDuenoMascotaActivity,
                        "Sin conexión: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun mostrarCargando(cargando: Boolean) {
        binding.progressBarCrear.visibility     = if (cargando) View.VISIBLE else View.GONE
        binding.btnGuardarDuenoMascota.isEnabled = !cargando
    }
}