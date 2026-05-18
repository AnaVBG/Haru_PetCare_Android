package com.dam2.haru_petcare.ui.mascotas

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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

class MascotaFragment : Fragment() {

    private var _binding: FragmentMascotasBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: MascotaAdapter

    /**
     * Launcher para abrir AddMascotaActivity y recibir el resultado.
     *
     * ActivityResultContracts.StartActivityForResult es la forma moderna
     * de manejar resultados entre Activities — reemplaza al deprecado
     * startActivityForResult() + onActivityResult().
     *
     * Cuando AddMascotaActivity llama a setResult(RESULT_OK) y finish(),
     * este launcher recibe el resultado y recarga la lista de mascotas.
     */
    private val addMascotaLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // La mascota se guardó correctamente — recargamos la lista
            cargarMascotas()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
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
            val intent = Intent(requireContext(), AddMascotaActivity::class.java)
            addMascotaLauncher.launch(intent)
        }
    }

    private fun configurarRecyclerView() {
        adapter = MascotaAdapter { mascota ->
            val intent = Intent(requireContext(), DetalleMascotaActivity::class.java).apply {
                putExtra(Constants.EXTRA_MASCOTA_ID, mascota.id)
                putExtra("mascota_nombre", mascota.nombre)
            }
            startActivity(intent)
        }

        binding.rvMascotas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MascotaFragment.adapter
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