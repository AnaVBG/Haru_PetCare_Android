package com.dam2.haru_petcare.ui.mascotas

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dam2.haru_petcare.databinding.FragmentMascotasBinding
import com.dam2.haru_petcare.model.MascotaDTO
import com.dam2.haru_petcare.network.HaruApiService
import com.dam2.haru_petcare.network.RetrofitClient
import com.dam2.haru_petcare.util.Constants
import com.dam2.haru_petcare.util.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MascotasFragment : Fragment() {

    private var _binding: FragmentMascotasBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: MascotaAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMascotasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        configurarRecyclerView()
        cargarMascotas()

        binding.fabAnadirMascota.setOnClickListener {
            // Vertical futura: añadir mascota
            Toast.makeText(requireContext(), "Próximamente: añadir mascota", Toast.LENGTH_SHORT).show()
        }
    }

    private fun configurarRecyclerView() {
        adapter = MascotaAdapter { mascota ->
            // Al tocar una mascota, abrimos DetalleMascotaActivity
            // y le pasamos el ID mediante Intent
            val intent = Intent(requireContext(), DetalleMascotaActivity::class.java).apply {
                putExtra(Constants.EXTRA_MASCOTA_ID, mascota.id)
                // También pasamos el nombre para mostrarlo en la toolbar del detalle
                putExtra("mascota_nombre", mascota.nombre)
            }
            startActivity(intent)
        }

        binding.rvMascotas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MascotasFragment.adapter
            // Animación de entrada suave al cargar
            itemAnimator?.changeDuration = 0
        }
    }

    private fun cargarMascotas() {
        mostrarCargando(true)

        val api = RetrofitClient
            .getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)

        api.getMascotasPorDueno(sessionManager.getIdUsuario())
            .enqueue(object : Callback<List<MascotaDTO>> {

                override fun onResponse(
                    call: Call<List<MascotaDTO>>,
                    response: Response<List<MascotaDTO>>
                ) {
                    if (!isAdded) return
                    mostrarCargando(false)

                    if (response.isSuccessful) {
                        val lista = response.body() ?: emptyList()
                        adapter.setMascotas(lista)

                        // Mostramos el estado vacío si no hay mascotas
                        binding.layoutSinMascotas.visibility =
                            if (lista.isEmpty()) View.VISIBLE else View.GONE
                        binding.rvMascotas.visibility =
                            if (lista.isEmpty()) View.GONE else View.VISIBLE

                    } else {
                        mostrarError("Error al cargar (${response.code()})")
                    }
                }

                override fun onFailure(call: Call<List<MascotaDTO>>, t: Throwable) {
                    if (!isAdded) return
                    mostrarCargando(false)
                    mostrarError("Sin conexión: ${t.message}")
                }
            })
    }

    private fun mostrarCargando(cargando: Boolean) {
        binding.progressBarMascotas.visibility = if (cargando) View.VISIBLE else View.GONE
        binding.rvMascotas.visibility          = if (cargando) View.GONE   else View.VISIBLE
    }

    private fun mostrarError(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}