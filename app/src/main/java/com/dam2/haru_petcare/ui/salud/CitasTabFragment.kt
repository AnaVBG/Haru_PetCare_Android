package com.dam2.haru_petcare.ui.salud

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dam2.haru_petcare.databinding.BottomSheetNuevaCitaBinding
import com.dam2.haru_petcare.databinding.FragmentCitasTabBinding
import com.dam2.haru_petcare.model.CitaDTO
import com.dam2.haru_petcare.model.CitaInsertarDTO
import com.dam2.haru_petcare.model.UsuarioDTO
import com.dam2.haru_petcare.network.HaruApiService
import com.dam2.haru_petcare.network.RetrofitClient
import com.dam2.haru_petcare.ui.salud.CitaAdapter
import com.dam2.haru_petcare.util.Constants
import com.dam2.haru_petcare.util.SessionManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

class CitasTabFragment : Fragment() {

    private var _binding: FragmentCitasTabBinding? = null
    private val binding get() = _binding!!

    private lateinit var api: HaruApiService
    private lateinit var sessionManager: SessionManager
    private lateinit var citaAdapter: CitaAdapter

    private var idMascota: Long = -1L
    private var duenoId: Long = -1L

    companion object {
        private const val ARG_ID_MASCOTA = "id_mascota"
        private const val ARG_ID_DUENO   = "id_dueno"

        fun newInstance(idMascota: Long, duenoId: Long = -1L): CitasTabFragment {
            return CitasTabFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_ID_MASCOTA, idMascota)
                    putLong(ARG_ID_DUENO, duenoId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        idMascota = arguments?.getLong(ARG_ID_MASCOTA, -1L) ?: -1L
        duenoId   = arguments?.getLong(ARG_ID_DUENO, -1L)   ?: -1L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCitasTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        api = RetrofitClient
            .getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)

        val esVet = sessionManager.getRol() == Constants.ROL_VETERINARIO ||
                sessionManager.getRol() == Constants.ROL_CLINICA

        if (esVet) {
            binding.cardBannerCitas.visibility = View.GONE
            binding.fabCrearCitaTab.visibility = View.VISIBLE
            binding.fabCrearCitaTab.setOnClickListener { abrirBottomSheetCrearCita() }
        }

        configurarRecyclerView()
        cargarCitas()
    }

    private fun configurarRecyclerView() {
        citaAdapter = CitaAdapter(
            esVeterinario   = false,
            onCambiarEstado = { _, _ -> }
        )
        binding.rvCitasTab.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCitasTab.adapter = citaAdapter
        binding.rvCitasTab.isNestedScrollingEnabled = false
    }

    private fun cargarCitas() {
        binding.progressBarCitasTab.visibility = View.VISIBLE
        binding.tvSinCitasTab.visibility = View.GONE
        binding.rvCitasTab.visibility = View.GONE

        val idUsuario = sessionManager.getIdUsuario()
        val call: Call<List<CitaDTO>> = when (sessionManager.getRol()) {
            Constants.ROL_VETERINARIO -> api.getAgendaVeterinario(idUsuario)
            Constants.ROL_CLINICA     -> api.getCitasClinica(idUsuario)
            else                      -> api.getCitasDueno(idUsuario)
        }

        call.enqueue(object : Callback<List<CitaDTO>> {
            override fun onResponse(call: Call<List<CitaDTO>>, response: Response<List<CitaDTO>>) {
                if (!isAdded || _binding == null) return
                binding.progressBarCitasTab.visibility = View.GONE

                if (!response.isSuccessful) {
                    mostrarError("Error al cargar citas (${response.code()})")
                    return
                }

                val citas = (response.body() ?: emptyList())
                    .filter { it.idMascota == idMascota }

                if (citas.isEmpty()) {
                    binding.tvSinCitasTab.visibility = View.VISIBLE
                    binding.rvCitasTab.visibility = View.GONE
                } else {
                    citaAdapter.setCitas(citas)
                    binding.rvCitasTab.visibility = View.VISIBLE
                    binding.tvSinCitasTab.visibility = View.GONE
                }
            }

            override fun onFailure(call: Call<List<CitaDTO>>, t: Throwable) {
                if (!isAdded || _binding == null) return
                binding.progressBarCitasTab.visibility = View.GONE
                mostrarError("Sin conexión: ${t.message}")
            }
        })
    }

    private fun abrirBottomSheetCrearCita() {
        val dialog = BottomSheetDialog(requireContext())
        val bs = BottomSheetNuevaCitaBinding.inflate(layoutInflater)
        dialog.setContentView(bs.root)

        // La mascota ya se conoce — ocultamos ese campo
        bs.tilMascota.visibility = View.GONE

        val esClinica = sessionManager.getRol() == Constants.ROL_CLINICA
        var idVeterinarioSeleccionado: Long = -1L
        var fechaHoraSeleccionada: String? = null

        if (esClinica) {
            bs.tilVeterinario.visibility = View.VISIBLE
            api.getVeterinariosDeClinica(sessionManager.getIdUsuario())
                .enqueue(object : Callback<List<UsuarioDTO>> {
                    override fun onResponse(
                        call: Call<List<UsuarioDTO>>,
                        response: Response<List<UsuarioDTO>>
                    ) {
                        if (!isAdded) return
                        val vets = response.body() ?: return
                        val nombres = vets.map { it.nombre ?: "—" }
                        bs.actvVeterinario.setAdapter(
                            ArrayAdapter(requireContext(),
                                android.R.layout.simple_dropdown_item_1line, nombres)
                        )
                        bs.actvVeterinario.setOnItemClickListener { _, _, pos, _ ->
                            idVeterinarioSeleccionado = vets[pos].id ?: -1L
                        }
                    }
                    override fun onFailure(call: Call<List<UsuarioDTO>>, t: Throwable) {
                        if (!isAdded) return
                        Toast.makeText(requireContext(),
                            "Error al cargar veterinarios", Toast.LENGTH_SHORT).show()
                    }
                })
        }

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

            bs.btnGuardarCita.isEnabled      = false
            bs.progressBarAnadir.visibility  = View.VISIBLE

            api.crearCita(
                CitaInsertarDTO(
                    fechaCita     = fechaHoraSeleccionada!!,
                    motivo        = motivo,
                    idMascota     = idMascota,
                    idVeterinario = idVeterinario,
                    idDueno       = duenoId
                )
            ).enqueue(object : Callback<CitaDTO> {
                override fun onResponse(call: Call<CitaDTO>, response: Response<CitaDTO>) {
                    if (!isAdded || _binding == null) return
                    bs.btnGuardarCita.isEnabled     = true
                    bs.progressBarAnadir.visibility = View.GONE
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(),
                            "Cita creada correctamente", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        cargarCitas()
                    } else {
                        Toast.makeText(requireContext(),
                            "Error al guardar (${response.code()})", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<CitaDTO>, t: Throwable) {
                    if (!isAdded || _binding == null) return
                    bs.btnGuardarCita.isEnabled     = true
                    bs.progressBarAnadir.visibility = View.GONE
                    Toast.makeText(requireContext(),
                        "Sin conexión: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        }

        dialog.show()
    }

    private fun mostrarError(mensaje: String) {
        if (isAdded) Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}