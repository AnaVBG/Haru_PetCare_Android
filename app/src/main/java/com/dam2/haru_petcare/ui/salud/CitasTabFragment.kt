package com.dam2.haru_petcare.ui.salud

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dam2.haru_petcare.databinding.FragmentCitasTabBinding
import com.dam2.haru_petcare.model.CitaDTO
import com.dam2.haru_petcare.network.HaruApiService
import com.dam2.haru_petcare.network.RetrofitClient
import com.dam2.haru_petcare.ui.citas.CitaAdapter
import com.dam2.haru_petcare.util.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.dam2.haru_petcare.util.Constants

class CitasTabFragment : Fragment() {

    private var _binding: FragmentCitasTabBinding? = null
    private val binding get() = _binding!!

    private lateinit var api: HaruApiService
    private lateinit var sessionManager: SessionManager
    private lateinit var citaAdapter: CitaAdapter

    private var idMascota: Long = -1L

    companion object {
        private const val ARG_ID_MASCOTA = "id_mascota"

        fun newInstance(idMascota: Long): CitasTabFragment {
            return CitasTabFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_ID_MASCOTA, idMascota)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        idMascota = arguments?.getLong(ARG_ID_MASCOTA, -1L) ?: -1L
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
            override fun onResponse(
                call: Call<List<CitaDTO>>,
                response: Response<List<CitaDTO>>
            ) {
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
                binding.progressBarCitasTab.visibility = View.GONE
                mostrarError("Sin conexión: ${t.message}")
            }
        })
    }

    private fun mostrarError(mensaje: String) {
        if (isAdded) Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}