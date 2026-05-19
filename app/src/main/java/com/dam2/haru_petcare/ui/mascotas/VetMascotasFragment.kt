package com.dam2.haru_petcare.ui.mascotas

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dam2.haru_petcare.databinding.FragmentVetMascotasBinding
import com.dam2.haru_petcare.model.MascotaDTO
import com.dam2.haru_petcare.network.HaruApiService
import com.dam2.haru_petcare.network.RetrofitClient
import com.dam2.haru_petcare.util.Constants
import com.dam2.haru_petcare.util.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VetMascotasFragment : Fragment() {

    private var _binding: FragmentVetMascotasBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: VetMascotaAdapter
    private val handler = Handler(Looper.getMainLooper())
    private var especieSeleccionada: String? = null

    private val especies = listOf("Todos", "Perro", "Gato", "Pájaro", "Conejo", "Reptil", "Otro")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVetMascotasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        configurarAdapter()
        configurarEspecieDropdown()
        configurarBusqueda()
        buscarMascotas()
    }

    private fun configurarAdapter() {
        adapter = VetMascotaAdapter { mascota ->
            val intent = Intent(requireContext(), DetalleMascotaActivity::class.java).apply {
                putExtra(Constants.EXTRA_MASCOTA_ID, mascota.id)
                putExtra("mascota_nombre", mascota.nombre)
                putExtra("idDueno", mascota.duenoId)
            }
            startActivity(intent)
        }
        binding.rvMascotas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMascotas.adapter = adapter
    }

    private fun configurarEspecieDropdown() {
        val dropdownAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            especies
        )
        binding.actvEspecie.setAdapter(dropdownAdapter)
        binding.actvEspecie.setText("Todos", false)

        binding.actvEspecie.setOnItemClickListener { _, _, position, _ ->
            especieSeleccionada = if (position == 0) null else especies[position]
            buscarMascotas()
        }
    }

    private fun configurarBusqueda() {
        binding.etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed({ buscarMascotas() }, 400)
            }
        })
    }

    private fun buscarMascotas() {
        val buscar = binding.etBuscar.text?.toString()?.trim()

        binding.progressBar.visibility = View.VISIBLE
        binding.tvSinResultados.visibility = View.GONE

        val api = RetrofitClient
            .getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)

        api.buscarTodasMascotas(especieSeleccionada, buscar?.ifBlank { null })
            .enqueue(object : Callback<List<MascotaDTO>> {

                override fun onResponse(
                    call: Call<List<MascotaDTO>>,
                    response: Response<List<MascotaDTO>>
                ) {
                    binding.progressBar.visibility = View.GONE
                    if (response.isSuccessful) {
                        val lista = response.body() ?: emptyList()
                        adapter.setLista(lista)
                        binding.tvSinResultados.visibility =
                            if (lista.isEmpty()) View.VISIBLE else View.GONE
                    } else {
                        Toast.makeText(context, "Error al cargar mascotas", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<MascotaDTO>>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "Sin conexión: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }
}