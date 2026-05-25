package com.dam2.haru_petcare.ui.salud

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dam2.haru_petcare.databinding.FragmentVacunasTabBinding
import com.dam2.haru_petcare.model.HistorialMedicoDTO
import com.dam2.haru_petcare.network.HaruApiService
import com.dam2.haru_petcare.network.RetrofitClient
import com.dam2.haru_petcare.ui.mascotas.HistorialAdapter
import com.dam2.haru_petcare.util.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VacunasTabFragment : Fragment() {

    private var _binding: FragmentVacunasTabBinding? = null
    private val binding get() = _binding!!

    private lateinit var api: HaruApiService
    private lateinit var sessionManager: SessionManager
    private lateinit var vacunasAdapter: HistorialAdapter

    private var idMascota: Long = -1L

    companion object {
        private const val ARG_ID_MASCOTA = "id_mascota"

        fun newInstance(idMascota: Long): VacunasTabFragment {
            return VacunasTabFragment().apply {
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
        _binding = FragmentVacunasTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        api = RetrofitClient
            .getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)

        configurarRecyclerView()
        cargarVacunas()
    }

    private fun configurarRecyclerView() {
        vacunasAdapter = HistorialAdapter()
        binding.rvVacunasTab.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVacunasTab.adapter = vacunasAdapter
        binding.rvVacunasTab.isNestedScrollingEnabled = false
    }

    private fun cargarVacunas() {
        if (idMascota == -1L) return

        binding.progressBarVacunasTab.visibility = View.VISIBLE
        binding.tvSinVacunasTab.visibility = View.GONE
        binding.rvVacunasTab.visibility = View.GONE

        api.getHistorial(idMascota).enqueue(object : Callback<List<HistorialMedicoDTO>> {
            override fun onResponse(
                call: Call<List<HistorialMedicoDTO>>,
                response: Response<List<HistorialMedicoDTO>>
            ) {
                binding.progressBarVacunasTab.visibility = View.GONE

                if (!response.isSuccessful) {
                    mostrarError("Error al cargar vacunas (${response.code()})")
                    return
                }

                val vacunas = (response.body() ?: emptyList())
                    .filter { it.tipoRegistro?.uppercase() == "VACUNA" }

                if (vacunas.isEmpty()) {
                    binding.tvSinVacunasTab.visibility = View.VISIBLE
                    binding.rvVacunasTab.visibility = View.GONE
                } else {
                    vacunasAdapter.setRegistros(vacunas)
                    binding.rvVacunasTab.visibility = View.VISIBLE
                    binding.tvSinVacunasTab.visibility = View.GONE
                }
            }

            override fun onFailure(call: Call<List<HistorialMedicoDTO>>, t: Throwable) {
                binding.progressBarVacunasTab.visibility = View.GONE
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