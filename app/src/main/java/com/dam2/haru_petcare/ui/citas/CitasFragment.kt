package com.dam2.haru_petcare.ui.citas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dam2.haru_petcare.databinding.FragmentCitasBinding
import com.dam2.haru_petcare.model.CitaDTO
import com.dam2.haru_petcare.network.HaruApiService
import com.dam2.haru_petcare.network.RetrofitClient
import com.dam2.haru_petcare.util.Constants
import com.dam2.haru_petcare.util.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CitasFragment : Fragment() {

    private var _binding: FragmentCitasBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: CitaAdapter

    private var esVeterinario = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCitasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        esVeterinario  = sessionManager.getRol() == Constants.ROL_VETERINARIO

        configurarAdapter()
        configurarFiltros()
        cargarCitas()
    }

    private fun configurarAdapter() {
        adapter = CitaAdapter(
            esVeterinario   = esVeterinario,
            onCambiarEstado = { cita, nuevoEstado ->
                mostrarDialogoConfirmacion(cita, nuevoEstado)
            }
        )

        binding.rvCitas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter       = this@CitasFragment.adapter
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

        val api = RetrofitClient
            .getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)

        val idUsuario = sessionManager.getIdUsuario()

        val call: Call<List<CitaDTO>> = if (esVeterinario) {
            api.getAgendaVeterinario(idUsuario)
        } else {
            api.getCitasDueno(idUsuario)
        }

        call.enqueue(object : Callback<List<CitaDTO>> {

            override fun onResponse(
                call: Call<List<CitaDTO>>,
                response: Response<List<CitaDTO>>
            ) {
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

        val api = RetrofitClient
            .getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)

        api.cambiarEstadoCita(idCita, nuevoEstado).enqueue(object : Callback<CitaDTO> {

            override fun onResponse(call: Call<CitaDTO>, response: Response<CitaDTO>) {
                if (!isAdded) return
                if (response.isSuccessful) {
                    val mensaje = if (nuevoEstado == "COMPLETADA") "Cita completada ✓" else "Cita cancelada"
                    Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
                    cargarCitas()
                } else {
                    mostrarError("Error al actualizar (${response.code()})")
                }
            }

            override fun onFailure(call: Call<CitaDTO>, t: Throwable) {
                if (!isAdded) return
                mostrarCargando(false)
                mostrarError("Sin conexión: ${t.message}")
            }
        })
    }

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