package com.dam2.haru_petcare.ui.salud

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dam2.haru_petcare.databinding.BottomSheetAnadirHistorialBinding
import com.dam2.haru_petcare.databinding.FragmentHistorialTabBinding
import com.dam2.haru_petcare.model.HistorialInsertarDTO
import com.dam2.haru_petcare.model.HistorialMedicoDTO
import com.dam2.haru_petcare.network.HaruApiService
import com.dam2.haru_petcare.network.RetrofitClient
import com.dam2.haru_petcare.ui.mascotas.HistorialAdapter
import com.dam2.haru_petcare.util.SessionManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class HistorialTabFragment : Fragment() {

    private var _binding: FragmentHistorialTabBinding? = null
    private val binding get() = _binding!!

    private lateinit var api: HaruApiService
    private lateinit var sessionManager: SessionManager
    private lateinit var historialAdapter: HistorialAdapter

    private var idMascota: Long = -1L

    companion object {
        private const val ARG_ID_MASCOTA = "id_mascota"

        fun newInstance(idMascota: Long): HistorialTabFragment {
            return HistorialTabFragment().apply {
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
        _binding = FragmentHistorialTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        api = RetrofitClient
            .getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)

        configurarRecyclerView()
        cargarHistorial()

        binding.btnExportarPdfTab.setOnClickListener { descargarPdf() }

        val rol = sessionManager.getRol()
        if (rol == "VETERINARIO" || rol == "CLINICA") {
            binding.fabAnadirHistorialTab.visibility = View.VISIBLE
            binding.fabAnadirHistorialTab.setOnClickListener {
                abrirBottomSheetAnadirHistorial()
            }
        }
    }

    private fun configurarRecyclerView() {
        historialAdapter = HistorialAdapter()
        binding.rvHistorialTab.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistorialTab.adapter = historialAdapter
        binding.rvHistorialTab.isNestedScrollingEnabled = false
    }

    private fun cargarHistorial() {
        if (idMascota == -1L) return

        binding.progressBarHistorialTab.visibility = View.VISIBLE
        binding.tvSinHistorialTab.visibility = View.GONE
        binding.rvHistorialTab.visibility = View.GONE
        binding.btnExportarPdfTab.visibility = View.GONE

        api.getHistorial(idMascota).enqueue(object : Callback<List<HistorialMedicoDTO>> {
            override fun onResponse(
                call: Call<List<HistorialMedicoDTO>>,
                response: Response<List<HistorialMedicoDTO>>
            ) {
                if (_binding == null) return
                binding.progressBarHistorialTab.visibility = View.GONE

                if (!response.isSuccessful) {
                    mostrarError("Error al cargar historial (${response.code()})")
                    return
                }

                val registros = response.body() ?: emptyList()

                if (registros.isEmpty()) {
                    binding.tvSinHistorialTab.visibility = View.VISIBLE
                    binding.rvHistorialTab.visibility = View.GONE
                    binding.btnExportarPdfTab.visibility = View.GONE
                } else {
                    historialAdapter.setRegistros(registros)
                    binding.rvHistorialTab.visibility = View.VISIBLE
                    binding.tvSinHistorialTab.visibility = View.GONE
                    binding.btnExportarPdfTab.visibility = View.VISIBLE
                }
            }

            override fun onFailure(call: Call<List<HistorialMedicoDTO>>, t: Throwable) {
                if (_binding == null) return
                binding.progressBarHistorialTab.visibility = View.GONE
                mostrarError("Sin conexión: ${t.message}")
            }
        })
    }

    private fun abrirBottomSheetAnadirHistorial() {
        val dialog = BottomSheetDialog(requireContext())
        val bs = BottomSheetAnadirHistorialBinding.inflate(layoutInflater)
        dialog.setContentView(bs.root)

        val tipos = listOf("Vacuna", "Desparasitación", "Consulta", "Cirugía", "Análisis", "Otro")
        bs.actvTipo.setAdapter(
            android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tipos)
        )

        bs.btnGuardarHistorial.setOnClickListener {
            val tipo = bs.actvTipo.text.toString().trim()
            val descripcion = bs.etDescripcion.text?.toString()?.trim() ?: ""

            bs.tilTipo.error = null
            bs.tilDescripcion.error = null

            if (tipo.isEmpty()) { bs.tilTipo.error = "Selecciona el tipo"; return@setOnClickListener }
            if (descripcion.isEmpty()) { bs.tilDescripcion.error = "Escribe una descripción"; return@setOnClickListener }

            bs.progressBarHistorialAdd.visibility = View.VISIBLE
            bs.btnGuardarHistorial.isEnabled = false

            val dto = HistorialInsertarDTO(
                tipoRegistro = tipo,
                descripcion  = descripcion,
                idMascota    = idMascota
            )

            api.crearRegistroHistorial(dto).enqueue(object : Callback<HistorialMedicoDTO> {
                override fun onResponse(call: Call<HistorialMedicoDTO>, response: Response<HistorialMedicoDTO>) {
                    if (_binding == null) return
                    bs.progressBarHistorialAdd.visibility = View.GONE
                    bs.btnGuardarHistorial.isEnabled = true
                    if (response.isSuccessful) {
                        dialog.dismiss()
                        mostrarError("Registro añadido")
                        cargarHistorial()
                    } else {
                        mostrarError("Error al guardar (${response.code()})")
                    }
                }
                override fun onFailure(call: Call<HistorialMedicoDTO>, t: Throwable) {
                    if (_binding == null) return
                    bs.progressBarHistorialAdd.visibility = View.GONE
                    bs.btnGuardarHistorial.isEnabled = true
                    mostrarError("Sin conexión: ${t.message}")
                }
            })
        }

        dialog.show()
    }

    private fun descargarPdf() {
        binding.btnExportarPdfTab.isEnabled = false
        Toast.makeText(requireContext(), "Descargando PDF...", Toast.LENGTH_SHORT).show()

        api.descargarHistorialPdf(idMascota).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (_binding == null) return
                binding.btnExportarPdfTab.isEnabled = true
                if (response.isSuccessful) {
                    response.body()?.let { guardarYAbrirPdf(it) }
                } else {
                    mostrarError("Error al descargar PDF (${response.code()})")
                }
            }
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                if (_binding == null) return
                binding.btnExportarPdfTab.isEnabled = true
                mostrarError("Sin conexión: ${t.message}")
            }
        })
    }

    private fun guardarYAbrirPdf(body: ResponseBody) {
        try {
            val archivo = File(requireContext().cacheDir, "historial_${idMascota}.pdf")
            archivo.outputStream().use { output -> body.byteStream().copyTo(output) }
            val uri = FileProvider.getUriForFile(
                requireContext(), "${requireContext().packageName}.provider", archivo)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            mostrarError("No se pudo abrir el PDF: ${e.message}")
        }
    }

    private fun mostrarError(mensaje: String) {
        if (isAdded) Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}