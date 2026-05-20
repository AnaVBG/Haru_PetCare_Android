package com.dam2.haru_petcare.ui.citas

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dam2.haru_petcare.databinding.ActivityCrearCitaBinding
import com.dam2.haru_petcare.model.CitaDTO
import com.dam2.haru_petcare.model.CitaInsertarDTO
import com.dam2.haru_petcare.model.MascotaDTO
import com.dam2.haru_petcare.model.UsuarioDTO
import com.dam2.haru_petcare.network.HaruApiService
import com.dam2.haru_petcare.network.RetrofitClient
import com.dam2.haru_petcare.util.Constants
import com.dam2.haru_petcare.util.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

class CrearCitaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCrearCitaBinding
    private lateinit var sessionManager: SessionManager

    // Datos fijos cuando viene desde DetalleMascota (veterinario)
    private var idMascotaFija: Long  = -1L
    private var idDuenoFijo: Long    = -1L

    // Datos seleccionables cuando es clínica
    private var veterinarios: List<UsuarioDTO> = emptyList()
    private var mascotas: List<MascotaDTO>     = emptyList()
    private var idVeterinarioSeleccionado: Long = -1L
    private var idMascotaSeleccionada: Long     = -1L
    private var idDuenoSeleccionado: Long       = -1L

    private var fechaHoraSeleccionada: String? = null
    private val esClinica get() = sessionManager.getRol() == Constants.ROL_CLINICA

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearCitaBinding.inflate(layoutInflater)
        sessionManager = SessionManager(this)
        setContentView(binding.root)

        idMascotaFija = intent.getLongExtra("idMascota", -1L)
        idDuenoFijo   = intent.getLongExtra("idDueno",   -1L)
        val nombreMascota = intent.getStringExtra("nombreMascota") ?: ""

        configurarToolbar()

        if (esClinica) {
            // La clínica elige veterinario y mascota desde dropdowns
            binding.tvMascotaCrearCita.visibility    = View.GONE
            binding.tilVeterinario.visibility        = View.VISIBLE
            binding.tilMascotaSelector.visibility    = View.VISIBLE
            cargarVeterinariosDeClinica()
            cargarMascotasDeClinica()
        } else {
            // Veterinario: mascota y dueño vienen fijos por Intent
            binding.tvMascotaCrearCita.text          = "Cita para $nombreMascota"
            binding.tilVeterinario.visibility        = View.GONE
            binding.tilMascotaSelector.visibility    = View.GONE
        }

        binding.cardFechaHora.setOnClickListener { mostrarDatePicker() }
        binding.btnGuardarCita.setOnClickListener { guardarCita() }
    }

    private fun configurarToolbar() {
        setSupportActionBar(binding.toolbarCrearCita)
        binding.toolbarCrearCita.setNavigationOnClickListener { finish() }
    }

    // ── Carga de datos para clínica ───────────────────────────────────────

    private fun cargarVeterinariosDeClinica() {
        val api = RetrofitClient.getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)

        api.getVeterinariosDeClinica(sessionManager.getIdUsuario())
            .enqueue(object : Callback<List<UsuarioDTO>> {

                override fun onResponse(
                    call: Call<List<UsuarioDTO>>,
                    response: Response<List<UsuarioDTO>>
                ) {
                    if (!response.isSuccessful) return
                    veterinarios = response.body() ?: emptyList()

                    val nombres = veterinarios.map { it.nombre ?: "—" }
                    val adapter = ArrayAdapter(
                        this@CrearCitaActivity,
                        android.R.layout.simple_dropdown_item_1line,
                        nombres
                    )
                    binding.actvVeterinario.setAdapter(adapter)
                    binding.actvVeterinario.setOnItemClickListener { _, _, pos, _ ->
                        idVeterinarioSeleccionado = veterinarios[pos].id ?: -1L
                    }
                }

                override fun onFailure(call: Call<List<UsuarioDTO>>, t: Throwable) {
                    Toast.makeText(this@CrearCitaActivity,
                        "Error al cargar veterinarios", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun cargarMascotasDeClinica() {
        val api = RetrofitClient.getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)

        api.buscarTodasMascotas(
            idUsuario = sessionManager.getIdUsuario(),
            especie   = null,
            buscar    = null
        ).enqueue(object : Callback<List<MascotaDTO>> {

            override fun onResponse(
                call: Call<List<MascotaDTO>>,
                response: Response<List<MascotaDTO>>
            ) {
                if (!response.isSuccessful) return
                mascotas = response.body() ?: emptyList()

                val nombres = mascotas.map { "${it.nombre} (${it.nombreDueno})" }
                val adapter = ArrayAdapter(
                    this@CrearCitaActivity,
                    android.R.layout.simple_dropdown_item_1line,
                    nombres
                )
                binding.actvMascota.setAdapter(adapter)
                binding.actvMascota.setOnItemClickListener { _, _, pos, _ ->
                    idMascotaSeleccionada = mascotas[pos].id    ?: -1L
                    idDuenoSeleccionado   = mascotas[pos].duenoId ?: -1L
                }
            }

            override fun onFailure(call: Call<List<MascotaDTO>>, t: Throwable) {
                Toast.makeText(this@CrearCitaActivity,
                    "Error al cargar mascotas", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ── Fecha y hora ──────────────────────────────────────────────────────

    private fun mostrarDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day -> mostrarTimePicker(year, month, day) },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply { datePicker.minDate = System.currentTimeMillis() }.show()
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

    // ── Guardar ───────────────────────────────────────────────────────────

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

        // Resolver IDs según el rol
        val idMascota: Long
        val idVeterinario: Long
        val idDueno: Long

        if (esClinica) {
            if (idVeterinarioSeleccionado == -1L) {
                Toast.makeText(this, "Selecciona un veterinario", Toast.LENGTH_SHORT).show()
                return
            }
            if (idMascotaSeleccionada == -1L) {
                Toast.makeText(this, "Selecciona una mascota", Toast.LENGTH_SHORT).show()
                return
            }
            idMascota     = idMascotaSeleccionada
            idVeterinario = idVeterinarioSeleccionado
            idDueno       = idDuenoSeleccionado
        } else {
            if (idMascotaFija == -1L || idDuenoFijo == -1L) {
                Toast.makeText(this, "Datos de mascota incompletos", Toast.LENGTH_SHORT).show()
                return
            }
            idMascota     = idMascotaFija
            idVeterinario = sessionManager.getIdUsuario()
            idDueno       = idDuenoFijo
        }

        binding.btnGuardarCita.isEnabled = false
        binding.progressBarCita.visibility = View.VISIBLE

        val dto = CitaInsertarDTO(
            fechaCita     = fechaHoraSeleccionada!!,
            motivo        = motivo,
            idMascota     = idMascota,
            idVeterinario = idVeterinario,
            idDueno       = idDueno
        )

        RetrofitClient.getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)
            .crearCita(dto)
            .enqueue(object : Callback<CitaDTO> {

                override fun onResponse(call: Call<CitaDTO>, response: Response<CitaDTO>) {
                    binding.btnGuardarCita.isEnabled   = true
                    binding.progressBarCita.visibility = View.GONE
                    if (response.isSuccessful) {
                        Toast.makeText(this@CrearCitaActivity,
                            "Cita guardada", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(this@CrearCitaActivity,
                            "Error al guardar (${response.code()})", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<CitaDTO>, t: Throwable) {
                    binding.btnGuardarCita.isEnabled   = true
                    binding.progressBarCita.visibility = View.GONE
                    Toast.makeText(this@CrearCitaActivity,
                        "Sin conexión: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
    }
}