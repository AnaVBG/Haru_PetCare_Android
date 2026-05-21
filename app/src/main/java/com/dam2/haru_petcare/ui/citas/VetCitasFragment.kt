package com.dam2.haru_petcare.ui.citas

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dam2.haru_petcare.databinding.BottomSheetNuevaCitaBinding
import com.dam2.haru_petcare.databinding.FragmentVetCitasBinding
import com.dam2.haru_petcare.model.CitaDTO
import com.dam2.haru_petcare.model.CitaInsertarDTO
import com.dam2.haru_petcare.model.MascotaDTO
import com.dam2.haru_petcare.model.UsuarioDTO
import com.dam2.haru_petcare.network.HaruApiService
import com.dam2.haru_petcare.network.RetrofitClient
import com.dam2.haru_petcare.util.Constants
import com.dam2.haru_petcare.util.SessionManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

class VetCitasFragment : Fragment() {

    private var _binding: FragmentVetCitasBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: CitaAdapter

    private val esClinica get() = sessionManager.getRol() == Constants.ROL_CLINICA

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVetCitasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        configurarAdapter()
        configurarFiltros()
        cargarCitas()

        binding.fabAnadirCita.setOnClickListener { mostrarBottomSheetNuevaCita() }
    }

    // ── RecyclerView ──────────────────────────────────────────────────────

    private fun configurarAdapter() {
        adapter = CitaAdapter(
            esVeterinario   = true,
            onCambiarEstado = { cita, nuevoEstado ->
                mostrarDialogoConfirmacion(cita, nuevoEstado)
            }
        )
        binding.rvCitas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter       = this@VetCitasFragment.adapter
            itemAnimator?.changeDuration = 0
        }
    }

    private fun configurarFiltros() {
        binding.chipGroupFiltros.setOnCheckedStateChangeListener { _, checkedIds ->
            val filtro = when {
                checkedIds.contains(binding.chipPendientes.id)  -> "PENDIENTE"
                checkedIds.contains(binding.chipCompletadas.id) -> "COMPLETADA"
                checkedIds.contains(binding.chipCanceladas.id)  -> "CANCELADA"
                else -> null
            }
            adapter.filtrarPorEstado(filtro)
            binding.layoutSinCitas.visibility =
                if (adapter.itemCount == 0) View.VISIBLE else View.GONE
        }
    }

    private fun cargarCitas() {
        mostrarCargando(true)
        val api = RetrofitClient.getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)

        val call: Call<List<CitaDTO>> = if (esClinica) {
            api.getCitasClinica(sessionManager.getIdUsuario())
        } else {
            api.getAgendaVeterinario(sessionManager.getIdUsuario())
        }

        call.enqueue(object : Callback<List<CitaDTO>> {
            override fun onResponse(call: Call<List<CitaDTO>>, response: Response<List<CitaDTO>>) {
                if (!isAdded) return
                mostrarCargando(false)
                if (response.isSuccessful) {
                    val citas = response.body() ?: emptyList()
                    adapter.setCitas(citas)
                    binding.layoutSinCitas.visibility =
                        if (citas.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvCitas.visibility =
                        if (citas.isEmpty()) View.GONE else View.VISIBLE
                } else {
                    mostrarError("Error al cargar citas (${response.code()})")
                }
            }
            override fun onFailure(call: Call<List<CitaDTO>>, t: Throwable) {
                if (!isAdded) return
                mostrarCargando(false)
                mostrarError("Sin conexión: ${t.message}")
            }
        })
    }

    // ── Bottom Sheet añadir cita ──────────────────────────────────────────

    private fun mostrarBottomSheetNuevaCita() {
        val dialog = BottomSheetDialog(requireContext())
        val bs     = BottomSheetNuevaCitaBinding.inflate(layoutInflater)
        dialog.setContentView(bs.root)

        var veterinarios: List<UsuarioDTO> = emptyList()
        var mascotas: List<MascotaDTO>     = emptyList()
        var idVeterinarioSeleccionado: Long = -1L
        var idMascotaSeleccionada: Long     = -1L
        var idDuenoSeleccionado: Long       = -1L
        var fechaHoraSeleccionada: String?  = null

        if (esClinica) {
            bs.tilVeterinario.visibility = View.VISIBLE
            RetrofitClient.getClient(sessionManager.getToken())
                .create(HaruApiService::class.java)
                .getVeterinariosDeClinica(sessionManager.getIdUsuario())
                .enqueue(object : Callback<List<UsuarioDTO>> {
                    override fun onResponse(
                        call: Call<List<UsuarioDTO>>,
                        response: Response<List<UsuarioDTO>>
                    ) {
                        if (!isAdded) return
                        if (response.isSuccessful) {
                            veterinarios = response.body() ?: emptyList()
                            val nombres = veterinarios.map { it.nombre ?: "—" }
                            bs.actvVeterinario.setAdapter(
                                ArrayAdapter(requireContext(),
                                    android.R.layout.simple_dropdown_item_1line, nombres)
                            )
                            bs.actvVeterinario.setOnItemClickListener { _, _, pos, _ ->
                                idVeterinarioSeleccionado = veterinarios[pos].id ?: -1L
                            }
                        }
                    }
                    override fun onFailure(call: Call<List<UsuarioDTO>>, t: Throwable) {
                        if (!isAdded) return
                        Toast.makeText(requireContext(),
                            "Error al cargar veterinarios", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        RetrofitClient.getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)
            .buscarTodasMascotas(
                idUsuario = sessionManager.getIdUsuario(),
                especie   = null,
                buscar    = null
            )
            .enqueue(object : Callback<List<MascotaDTO>> {
                override fun onResponse(
                    call: Call<List<MascotaDTO>>,
                    response: Response<List<MascotaDTO>>
                ) {
                    if (!isAdded) return
                    if (response.isSuccessful) {
                        mascotas = response.body() ?: emptyList()
                        val nombres = mascotas.map { "${it.nombre} (${it.nombreDueno ?: ""})" }
                        bs.actvMascota.setAdapter(
                            ArrayAdapter(requireContext(),
                                android.R.layout.simple_dropdown_item_1line, nombres)
                        )
                        bs.actvMascota.setOnItemClickListener { _, _, pos, _ ->
                            idMascotaSeleccionada = mascotas[pos].id     ?: -1L
                            idDuenoSeleccionado   = mascotas[pos].duenoId ?: -1L
                        }
                    }
                }
                override fun onFailure(call: Call<List<MascotaDTO>>, t: Throwable) {
                    if (!isAdded) return
                    Toast.makeText(requireContext(),
                        "Error al cargar mascotas", Toast.LENGTH_SHORT).show()
                }
            })

        bs.cardFechaHora.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    TimePickerDialog(
                        requireContext(),
                        { _, hour, minute ->
                            fechaHoraSeleccionada = String.format(
                                "%04d-%02d-%02dT%02d:%02d:00",
                                year, month + 1, day, hour, minute
                            )
                            bs.tvFechaHora.text = String.format(
                                "%02d/%02d/%04d a las %02d:%02d",
                                day, month + 1, year, hour, minute
                            )
                        },
                        cal.get(Calendar.HOUR_OF_DAY),
                        cal.get(Calendar.MINUTE),
                        true
                    ).show()
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).apply { datePicker.minDate = System.currentTimeMillis() }.show()
        }

        bs.btnCancelarCita.setOnClickListener { dialog.dismiss() }

        bs.btnGuardarCita.setOnClickListener {
            val motivo = bs.etMotivo.text?.toString()?.trim()

            if (fechaHoraSeleccionada == null) {
                Toast.makeText(requireContext(),
                    "Selecciona la fecha y hora", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (motivo.isNullOrBlank()) {
                bs.tilMotivo.error = "El motivo es obligatorio"
                return@setOnClickListener
            }
            bs.tilMotivo.error = null

            if (idMascotaSeleccionada == -1L) {
                Toast.makeText(requireContext(),
                    "Selecciona una mascota", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val idVeterinario: Long = if (esClinica) {
                if (idVeterinarioSeleccionado == -1L) {
                    Toast.makeText(requireContext(),
                        "Selecciona un veterinario", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                idVeterinarioSeleccionado
            } else {
                sessionManager.getIdUsuario()
            }

            bs.btnGuardarCita.isEnabled        = false
            bs.progressBarAnadir.visibility    = View.VISIBLE

            RetrofitClient.getClient(sessionManager.getToken())
                .create(HaruApiService::class.java)
                .crearCita(
                    CitaInsertarDTO(
                        fechaCita     = fechaHoraSeleccionada!!,
                        motivo        = motivo,
                        idMascota     = idMascotaSeleccionada,
                        idVeterinario = idVeterinario,
                        idDueno       = idDuenoSeleccionado
                    )
                )
                .enqueue(object : Callback<CitaDTO> {
                    override fun onResponse(call: Call<CitaDTO>, response: Response<CitaDTO>) {
                        if (!isAdded) return
                        bs.btnGuardarCita.isEnabled     = true
                        bs.progressBarAnadir.visibility = View.GONE
                        if (response.isSuccessful) {
                            Toast.makeText(requireContext(),
                                "Cita guardada correctamente", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            cargarCitas()
                        } else {
                            Toast.makeText(requireContext(),
                                "Error al guardar (${response.code()})", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<CitaDTO>, t: Throwable) {
                        if (!isAdded) return
                        bs.btnGuardarCita.isEnabled     = true
                        bs.progressBarAnadir.visibility = View.GONE
                        Toast.makeText(requireContext(),
                            "Sin conexión: ${t.message}", Toast.LENGTH_LONG).show()
                    }
                })
        }

        dialog.show()
    }

    // ── Cambio de estado de cita ──────────────────────────────────────────

    private fun mostrarDialogoConfirmacion(cita: CitaDTO, nuevoEstado: String) {
        val accion  = if (nuevoEstado == "COMPLETADA") "completar" else "cancelar"
        val mascota = cita.nombreMascota ?: "esta mascota"
        AlertDialog.Builder(requireContext())
            .setTitle("¿Confirmar acción?")
            .setMessage("Vas a $accion la cita de $mascota. Esta acción no se puede deshacer.")
            .setPositiveButton("Confirmar") { _, _ -> cambiarEstadoCita(cita, nuevoEstado) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun cambiarEstadoCita(cita: CitaDTO, nuevoEstado: String) {
        val idCita = cita.id ?: return
        RetrofitClient.getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)
            .cambiarEstadoCita(idCita, nuevoEstado)
            .enqueue(object : Callback<CitaDTO> {
                override fun onResponse(call: Call<CitaDTO>, response: Response<CitaDTO>) {
                    if (!isAdded) return
                    if (response.isSuccessful) {
                        val msg = if (nuevoEstado == "COMPLETADA") "Cita completada ✓" else "Cita cancelada"
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                        cargarCitas()
                    } else {
                        mostrarError("Error al actualizar (${response.code()})")
                    }
                }
                override fun onFailure(call: Call<CitaDTO>, t: Throwable) {
                    if (!isAdded) return
                    mostrarError("Sin conexión: ${t.message}")
                }
            })
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun mostrarCargando(cargando: Boolean) {
        binding.progressBarCitas.visibility = if (cargando) View.VISIBLE else View.GONE
        binding.rvCitas.visibility          = if (cargando) View.GONE   else View.VISIBLE
    }

    private fun mostrarError(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}