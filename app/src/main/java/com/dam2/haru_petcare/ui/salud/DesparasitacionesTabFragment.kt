package com.dam2.haru_petcare.ui.salud

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dam2.haru_petcare.databinding.FragmentDesparasitacionesTabBinding
import com.dam2.haru_petcare.model.HistorialMedicoDTO
import com.dam2.haru_petcare.network.HaruApiService
import com.dam2.haru_petcare.network.RetrofitClient
import com.dam2.haru_petcare.ui.mascotas.HistorialAdapter
import com.dam2.haru_petcare.util.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DesparasitacionesTabFragment : Fragment() {

    private var _binding: FragmentDesparasitacionesTabBinding? = null
    private val binding get() = _binding!!

    private lateinit var api: HaruApiService
    private lateinit var sessionManager: SessionManager
    private lateinit var desparAdapter: HistorialAdapter

    private var idMascota: Long = -1L

    companion object {
        private const val ARG_ID_MASCOTA = "id_mascota"

        fun newInstance(idMascota: Long): DesparasitacionesTabFragment {
            return DesparasitacionesTabFragment().apply {
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
        _binding = FragmentDesparasitacionesTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        api = RetrofitClient
            .getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)

        configurarRecyclerView()
        cargarDesparasitaciones()
    }

    private fun configurarRecyclerView() {
        binding.rvDesparTab.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDesparTab.adapter = desparAdapter
        binding.rvDesparTab.isNestedScrollingEnabled = false
    }

    private fun cargarDesparasitaciones() {
        if (idMascota == -1L) return

        binding.progressBarDesparTab.visibility = View.VISIBLE
        binding.tvSinDesparTab.visibility = View.GONE
        binding.rvDesparTab.visibility = View.GONE

        api.getHistorial(idMascota).enqueue(object : Callback<List<HistorialMedicoDTO>> {
            override fun onResponse(
                call: Call<List<HistorialMedicoDTO>>,
                response: Response<List<HistorialMedicoDTO>>
            ) {
                binding.progressBarDesparTab.visibility = View.GONE

                if (!response.isSuccessful) {
                    mostrarError("Error al cargar desparasitaciones (${response.code()})")
                    return
                }

                val despar = (response.body() ?: emptyList())
                    .filter { it.tipoRegistro?.uppercase() == "DESPARASITACION" }

                if (despar.isEmpty()) {
                    binding.tvSinDesparTab.visibility = View.VISIBLE
                    binding.rvDesparTab.visibility = View.GONE
                } else {
                    desparAdapter.setRegistros(despar)
                    binding.rvDesparTab.visibility = View.VISIBLE
                    binding.tvSinDesparTab.visibility = View.GONE
                }
            }

            override fun onFailure(call: Call<List<HistorialMedicoDTO>>, t: Throwable) {
                binding.progressBarDesparTab.visibility = View.GONE
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