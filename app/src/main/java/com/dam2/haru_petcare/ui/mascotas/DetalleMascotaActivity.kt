package com.dam2.haru_petcare.ui.mascotas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.dam2.haru_petcare.R
import com.dam2.haru_petcare.databinding.ActivityDetalleMascotaBinding
import com.dam2.haru_petcare.model.HistorialMedicoDTO
import com.dam2.haru_petcare.model.MascotaDTO
import com.dam2.haru_petcare.network.HaruApiService
import com.dam2.haru_petcare.network.RetrofitClient
import com.dam2.haru_petcare.ui.main.MainActivity
import com.dam2.haru_petcare.util.Constants
import com.dam2.haru_petcare.util.SessionManager
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class DetalleMascotaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetalleMascotaBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var historialAdapter: HistorialAdapter

    private var idMascota: Long = -1L
    private var nombreMascota: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalleMascotaBinding.inflate(layoutInflater)
        sessionManager = SessionManager(this)
        setContentView(binding.root)

        idMascota     = intent.getLongExtra(Constants.EXTRA_MASCOTA_ID, -1L)
        nombreMascota = intent.getStringExtra("mascota_nombre") ?: "Mascota"

        if (idMascota == -1L) {
            Toast.makeText(this, "Error: mascota no encontrada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        configurarToolbar()
        configurarHistorialRecyclerView()
        configurarBotones()
        cargarDatosMascota()
        cargarHistorial()
    }

    private fun configurarToolbar() {
        setSupportActionBar(binding.toolbarDetalle)
        binding.collapsingToolbar.title = nombreMascota
        binding.toolbarDetalle.setNavigationOnClickListener { finish() }
    }

    private fun configurarHistorialRecyclerView() {
        historialAdapter = HistorialAdapter()
        binding.rvHistorial.apply {
            layoutManager = LinearLayoutManager(this@DetalleMascotaActivity)
            adapter = historialAdapter
        }
    }

    private fun configurarBotones() {
        binding.btnDescargarPdf.setOnClickListener {
            descargarPdf()
        }

        binding.btnVerCitas.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("tab_destino", R.id.nav_citas)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        binding.btnActivarAlerta.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("tab_destino", R.id.nav_alertas)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
    }

    private fun cargarDatosMascota() {
        val api = RetrofitClient
            .getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)

        api.getMascotaPorId(idMascota).enqueue(object : Callback<MascotaDTO> {

            override fun onResponse(call: Call<MascotaDTO>, response: Response<MascotaDTO>) {
                if (response.isSuccessful) {
                    response.body()?.let { mascota ->
                        binding.tvDetalleEspecie.text  = mascota.especie ?: "—"
                        binding.tvDetalleRaza.text     = mascota.raza    ?: "—"
                        binding.tvDetalleFechaNac.text = formatearFecha(mascota.fechaNacimiento)
                        binding.collapsingToolbar.title = mascota.nombre ?: nombreMascota

                        if (!mascota.fotoUrl.isNullOrBlank()) {
                            Glide.with(this@DetalleMascotaActivity)
                                .load(mascota.fotoUrl)
                                .centerCrop()
                                .into(binding.ivFotoMascota)
                        }
                    }
                }
            }

            override fun onFailure(call: Call<MascotaDTO>, t: Throwable) { }
        })
    }

    private fun formatearFecha(fechaIso: String?): String {
        if (fechaIso == null) return "—"
        return try {
            val partes = fechaIso.split("-")
            "${partes[2]}/${partes[1]}/${partes[0]}"
        } catch (e: Exception) { fechaIso }
    }

    private fun cargarHistorial() {
        binding.progressBarHistorial.visibility = View.VISIBLE

        val api = RetrofitClient
            .getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)

        api.getHistorial(idMascota).enqueue(object : Callback<List<HistorialMedicoDTO>> {

            override fun onResponse(
                call: Call<List<HistorialMedicoDTO>>,
                response: Response<List<HistorialMedicoDTO>>
            ) {
                binding.progressBarHistorial.visibility = View.GONE

                if (response.isSuccessful) {
                    val historial = response.body() ?: emptyList()
                    historialAdapter.setRegistros(historial)

                    binding.tvSinHistorial.visibility =
                        if (historial.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvHistorial.visibility =
                        if (historial.isEmpty()) View.GONE else View.VISIBLE
                } else {
                    Toast.makeText(
                        this@DetalleMascotaActivity,
                        "Error al cargar historial (${response.code()})",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<List<HistorialMedicoDTO>>, t: Throwable) {
                binding.progressBarHistorial.visibility = View.GONE
                Toast.makeText(
                    this@DetalleMascotaActivity,
                    "Sin conexión: ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun descargarPdf() {
        Toast.makeText(this, "Descargando PDF...", Toast.LENGTH_SHORT).show()
        binding.btnDescargarPdf.isEnabled = false

        val api = RetrofitClient
            .getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)

        api.descargarHistorialPdf(idMascota).enqueue(object : Callback<ResponseBody> {

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                binding.btnDescargarPdf.isEnabled = true
                if (response.isSuccessful) {
                    response.body()?.let { guardarYAbrirPdf(it) }
                } else {
                    Toast.makeText(this@DetalleMascotaActivity, "Error al descargar PDF", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                binding.btnDescargarPdf.isEnabled = true
                Toast.makeText(this@DetalleMascotaActivity, "Error de conexión: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun guardarYAbrirPdf(body: ResponseBody) {
        try {
            val archivo = File(cacheDir, "historial_${nombreMascota}_$idMascota.pdf")

            FileOutputStream(archivo).use { output ->
                body.byteStream().use { input ->
                    input.copyTo(output)
                }
            }

            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                archivo
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            }

            startActivity(Intent.createChooser(intent, "Abrir PDF con..."))

        } catch (e: Exception) {
            Toast.makeText(this, "Error al abrir el PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}