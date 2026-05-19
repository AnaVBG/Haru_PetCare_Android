package com.dam2.haru_petcare.ui.citas

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dam2.haru_petcare.databinding.ActivityCrearCitaBinding
import com.dam2.haru_petcare.model.CitaDTO
import com.dam2.haru_petcare.model.CitaInsertarDTO
import com.dam2.haru_petcare.network.HaruApiService
import com.dam2.haru_petcare.network.RetrofitClient
import com.dam2.haru_petcare.util.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

class CrearCitaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCrearCitaBinding
    private lateinit var sessionManager: SessionManager

    private var idMascota: Long = -1L
    private var idDueno: Long = -1L
    private var fechaHoraSeleccionada: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearCitaBinding.inflate(layoutInflater)
        sessionManager = SessionManager(this)
        setContentView(binding.root)

        idMascota = intent.getLongExtra("idMascota", -1L)
        idDueno   = intent.getLongExtra("idDueno", -1L)
        val nombreMascota = intent.getStringExtra("nombreMascota") ?: "Mascota"

        configurarToolbar()
        binding.tvMascotaCrearCita.text = "Cita para $nombreMascota"

        binding.cardFechaHora.setOnClickListener { mostrarDatePicker() }
        binding.btnGuardarCita.setOnClickListener { guardarCita() }
    }

    private fun configurarToolbar() {
        setSupportActionBar(binding.toolbarCrearCita)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarCrearCita.setNavigationOnClickListener { finish() }
    }

    private fun mostrarDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                mostrarTimePicker(year, month, day)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis()
        }.show()
    }

    private fun mostrarTimePicker(year: Int, month: Int, day: Int) {
        val cal = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hour, minute ->
                fechaHoraSeleccionada = String.format(
                    "%04d-%02d-%02dT%02d:%02d:00",
                    year, month + 1, day, hour, minute
                )
                binding.tvFechaHora.text = String.format(
                    "%02d/%02d/%04d a las %02d:%02d",
                    day, month + 1, year, hour, minute
                )
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun guardarCita() {
        val motivo = binding.etMotivo.text?.toString()?.trim()

        if (fechaHoraSeleccionada == null) {
            Toast.makeText(this, "Selecciona la fecha y hora", Toast.LENGTH_SHORT).show()
            return
        }
        if (motivo.isNullOrBlank()) {
            binding.tilMotivo.error = "El motivo es obligatorio"
            return
        }
        binding.tilMotivo.error = null

        if (idMascota == -1L || idDueno == -1L) {
            Toast.makeText(this, "Datos de la mascota incompletos", Toast.LENGTH_SHORT).show()
            return
        }

        val dto = CitaInsertarDTO(
            fechaCita    = fechaHoraSeleccionada!!,
            motivo       = motivo,
            idMascota    = idMascota,
            idVeterinario = sessionManager.getId(),
            idDueno      = idDueno
        )

        binding.btnGuardarCita.isEnabled = false

        RetrofitClient.getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)
            .crearCita(dto)
            .enqueue(object : Callback<CitaDTO> {

                override fun onResponse(call: Call<CitaDTO>, response: Response<CitaDTO>) {
                    binding.btnGuardarCita.isEnabled = true
                    if (response.isSuccessful) {
                        Toast.makeText(this@CrearCitaActivity, "Cita guardada", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@CrearCitaActivity,
                            "Error al guardar (${response.code()})", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<CitaDTO>, t: Throwable) {
                    binding.btnGuardarCita.isEnabled = true
                    Toast.makeText(this@CrearCitaActivity,
                        "Sin conexión: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
    }
}