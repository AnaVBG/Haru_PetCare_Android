package com.dam2.haru_petcare.ui.mascotas

import android.app.Activity
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dam2.haru_petcare.databinding.BottomSheetOpcionesMascotaBinding
import com.dam2.haru_petcare.databinding.BottomSheetVincularDuenoBinding
import com.dam2.haru_petcare.databinding.FragmentVetMascotasBinding
import com.dam2.haru_petcare.model.MascotaDTO
import com.dam2.haru_petcare.model.VincularMascotaClinicaDTO
import com.dam2.haru_petcare.network.HaruApiService
import com.dam2.haru_petcare.network.RetrofitClient
import com.dam2.haru_petcare.util.Constants
import com.dam2.haru_petcare.util.SessionManager
import com.google.android.material.bottomsheet.BottomSheetDialog
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

    private val crearDuenoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) buscarMascotas()
    }

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
        configurarFab()
        buscarMascotas()
    }

    private fun configurarFab() {
        binding.fabAnadirMascota.setOnClickListener {
            mostrarOpcionesBottomSheet()
        }
    }

    private fun mostrarOpcionesBottomSheet() {
        val dialog    = BottomSheetDialog(requireContext())
        val bsBinding = BottomSheetOpcionesMascotaBinding.inflate(layoutInflater)
        dialog.setContentView(bsBinding.root)

        bsBinding.btnVincularExistente.setOnClickListener {
            dialog.dismiss()
            mostrarVincularDuenoBottomSheet()
        }

        bsBinding.btnCrearNuevo.setOnClickListener {
            dialog.dismiss()
            crearDuenoLauncher.launch(
                Intent(requireContext(), CrearDuenoMascotaActivity::class.java)
            )
        }

        dialog.show()
    }

    private fun mostrarVincularDuenoBottomSheet() {
        val dialog    = BottomSheetDialog(requireContext())
        val bsBinding = BottomSheetVincularDuenoBinding.inflate(layoutInflater)
        dialog.setContentView(bsBinding.root)

        // Buscar dueño al pulsar el icono de búsqueda o al confirmar teclado
        bsBinding.tilEmailDueno.setEndIconOnClickListener {
            buscarDuenoPorEmail(bsBinding, dialog)
        }
        bsBinding.etEmailDueno.setOnEditorActionListener { _, _, _ ->
            buscarDuenoPorEmail(bsBinding, dialog)
            true
        }

        dialog.show()
    }

    private fun buscarDuenoPorEmail(
        bsBinding: BottomSheetVincularDuenoBinding,
        dialog: BottomSheetDialog
    ) {
        val email = bsBinding.etEmailDueno.text.toString().trim()
        if (email.isEmpty()) {
            bsBinding.tilEmailDueno.error = "Introduce el email del dueño"
            return
        }
        bsBinding.tilEmailDueno.error = null
        bsBinding.progressBarBusqueda.visibility = View.VISIBLE
        bsBinding.layoutResultadoDueno.visibility = View.GONE

        val api = RetrofitClient.getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)

        api.buscarUsuarioPorEmail(email).enqueue(object : Callback<com.dam2.haru_petcare.model.UsuarioDTO> {

            override fun onResponse(
                call: Call<com.dam2.haru_petcare.model.UsuarioDTO>,
                response: Response<com.dam2.haru_petcare.model.UsuarioDTO>
            ) {
                if (!isAdded) return
                bsBinding.progressBarBusqueda.visibility = View.GONE

                if (response.isSuccessful) {
                    val dueno = response.body() ?: return
                    bsBinding.tvNombreDuenoEncontrado.text = dueno.nombre ?: "Sin nombre"
                    bsBinding.tvEmailDuenoEncontrado.text  = dueno.email  ?: ""
                    bsBinding.tvMascotasDuenoEncontrado.text =
                        "${dueno.totalMascotas ?: 0} mascota(s) registrada(s)"
                    bsBinding.layoutResultadoDueno.visibility = View.VISIBLE

                    bsBinding.btnVincularTodas.setOnClickListener {
                        dialog.dismiss()
                        vincularDueno(dueno.id ?: return@setOnClickListener)
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "No se encontró ningún dueño con ese email",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(
                call: Call<com.dam2.haru_petcare.model.UsuarioDTO>,
                t: Throwable
            ) {
                if (!isAdded) return
                bsBinding.progressBarBusqueda.visibility = View.GONE
                Toast.makeText(requireContext(), "Sin conexión: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun vincularDueno(idDueno: Long) {
        val dto = VincularMascotaClinicaDTO(
            idDueno   = idDueno,
            idMascota = null,   // null = todas las mascotas del dueño
            idClinica = sessionManager.getIdUsuario()
        )

        RetrofitClient.getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)
            .vincularMascotaClinica(dto)
            .enqueue(object : Callback<List<MascotaDTO>> {

                override fun onResponse(
                    call: Call<List<MascotaDTO>>,
                    response: Response<List<MascotaDTO>>
                ) {
                    if (!isAdded) return
                    if (response.isSuccessful) {
                        val n = response.body()?.size ?: 0
                        Toast.makeText(
                            requireContext(),
                            "$n mascota(s) vinculada(s) a la clínica",
                            Toast.LENGTH_LONG
                        ).show()
                        buscarMascotas()
                    } else {
                        Toast.makeText(requireContext(),
                            "Error al vincular (${response.code()})", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<MascotaDTO>>, t: Throwable) {
                    if (!isAdded) return
                    Toast.makeText(requireContext(),
                        "Sin conexión: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
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
            requireContext(), android.R.layout.simple_dropdown_item_1line, especies
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

        RetrofitClient.getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)
            .buscarTodasMascotas(
                idUsuario = sessionManager.getIdUsuario(),
                especie   = especieSeleccionada,
                buscar    = buscar?.ifBlank { null }
            ).enqueue(object : Callback<List<MascotaDTO>> {

                override fun onResponse(
                    call: Call<List<MascotaDTO>>,
                    response: Response<List<MascotaDTO>>
                ) {
                    if (!isAdded) return
                    binding.progressBar.visibility = View.GONE
                    if (response.isSuccessful) {
                        val lista = response.body() ?: emptyList()
                        adapter.setLista(lista)
                        binding.tvSinResultados.visibility =
                            if (lista.isEmpty()) View.VISIBLE else View.GONE
                    } else {
                        Toast.makeText(context, "Error al cargar (${response.code()})",
                            Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<MascotaDTO>>, t: Throwable) {
                    if (!isAdded) return
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