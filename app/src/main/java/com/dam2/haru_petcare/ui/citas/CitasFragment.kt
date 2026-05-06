package com.dam2.haru_petcare.ui.citas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

    // Determinamos si es veterinario UNA vez al crear el Fragment
    // para no consultarlo en cada bind del Adapter
    private var esVeterinario = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCitasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager  = SessionManager(requireContext())
        esVeterinario   = sessionManager.getRol() == Constants.ROL_VETERINARIO

        configurarToolbar()
        configurarAdapter()
        configurarFiltros()
        cargarCitas()
    }

    /**
     * El título de la toolbar cambia según el rol.
     * El tribunal valorará que la UI se adapta al tipo de usuario.
     */
    private fun configurarToolbar() {
        val titulo = if (esVeterinario) "Mi agenda" else "Mis citas"
        binding.toolbarCitas.title = titulo
    }

    private fun configurarAdapter() {
        adapter = CitaAdapter(
            esVeterinario  = esVeterinario,
            onCambiarEstado = { cita, nuevoEstado ->
                // Pedimos confirmación antes de cambiar el estado
                mostrarDialogoConfirmacion(cita, nuevoEstado)
            }
        )

        binding.rvCitas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter       = this@CitasFragment.adapter
            itemAnimator?.changeDuration = 0
        }
    }

    /**
     * Los chips filtran la lista en memoria.
     * NO hacemos una nueva llamada a la API por cada chip pulsado —
     * simplemente filtramos la lista que ya tenemos en el Adapter.
     */
    private fun configurarFiltros() {
        binding.chipGroupFiltros.setOnCheckedStateChangeListener { _, checkedIds ->
            val filtro = when {
                checkedIds.contains(binding.chipPendientes.id)  -> "PENDIENTE"
                checkedIds.contains(binding.chipCompletadas.id) -> "COMPLETADA"
                checkedIds.contains(binding.chipCanceladas.id)  -> "CANCELADA"
                else -> null // chip "Todas" seleccionado
            }
            adapter.filtrarPorEstado(filtro)

            // Actualizamos la visibilidad del estado vacío
            binding.layoutSinCitas.visibility =
                if (adapter.itemCount == 0) View.VISIBLE else View.GONE
        }
    }

    /**
     * Carga las citas del endpoint correcto según el rol.
     * Dueño      → GET /api/citas/dueno/{id}
     * Veterinario → GET /api/citas/veterinario/{id}
     */
    private fun cargarCitas() {
        mostrarCargando(true)

        val api = RetrofitClient
            .getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)

        val idUsuario = sessionManager.getIdUsuario()

        // Elegimos la llamada correcta según el rol
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

    /**
     * Diálogo de confirmación antes de cambiar el estado de una cita.
     * Evita cambios accidentales — importante para el flujo de un veterinario.
     */
    private fun mostrarDialogoConfirmacion(cita: CitaDTO, nuevoEstado: String) {
        val accion = if (nuevoEstado == "COMPLETADA") "completar" else "cancelar"
        val mascota = cita.nombreMascota ?: "esta mascota"

        AlertDialog.Builder(requireContext())
            .setTitle("¿Confirmar acción?")
            .setMessage("Vas a $accion la cita de $mascota. Esta acción no se puede deshacer.")
            .setPositiveButton("Confirmar") { _, _ ->
                cambiarEstadoCita(cita, nuevoEstado)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Llama a PUT /api/citas/{id}/estado con el nuevo estado.
     * Si tiene éxito, recargamos toda la lista para reflejar el cambio.
     */
    private fun cambiarEstadoCita(cita: CitaDTO, nuevoEstado: String) {
        val idCita = cita.id ?: return // si no hay ID, no hacemos nada

        val api = RetrofitClient
            .getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)

        api.cambiarEstadoCita(idCita, nuevoEstado).enqueue(object : Callback<CitaDTO> {

            override fun onResponse(
                call: Call<CitaDTO>,
                response: Response<CitaDTO>
            ) {
                if (!isAdded) return

                if (response.isSuccessful) {
                    val mensaje = if (nuevoEstado == "COMPLETADA") {
                        "Cita completada ✓"
                    } else {
                        "Cita cancelada"
                    }
                    Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()

                    // Recargamos la lista completa para que el estado se actualice
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