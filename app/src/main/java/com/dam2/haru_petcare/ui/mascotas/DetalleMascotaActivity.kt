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
import com.dam2.haru_petcare.network.HaruApiService
import com.dam2.haru_petcare.network.RetrofitClient
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

    // ID de la mascota recibido desde MascotasFragment via Intent
    private var idMascota: Long = -1L
    private var nombreMascota: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalleMascotaBinding.inflate(layoutInflater)
        sessionManager = SessionManager(this)
        setContentView(binding.root)

        // Recuperamos los datos del Intent
        // '?:' proporciona el valor por defecto si el extra no existe
        idMascota    = intent.getLongExtra(Constants.EXTRA_MASCOTA_ID, -1L)
        nombreMascota = intent.getStringExtra("mascota_nombre") ?: "Mascota"

        if (idMascota == -1L) {
            Toast.makeText(this, "Error: mascota no encontrada", Toast.LENGTH_SHORT).show()
            finish() // Cerramos si no hay ID válido
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
        // El título se muestra en el CollapsingToolbarLayout
        binding.collapsingToolbar.title = nombreMascota

        // El botón de volver atrás cierra esta Activity
        binding.toolbarDetalle.setNavigationOnClickListener { finish() }
    }

    private fun configurarHistorialRecyclerView() {
        historialAdapter = HistorialAdapter()
        binding.rvHistorial.apply {
            layoutManager = LinearLayoutManager(this@DetalleMascotaActivity)
            adapter = historialAdapter
            // nestedScrollingEnabled = false ya está en el XML
            // para que el scroll sea del NestedScrollView padre
        }
    }

    private fun configurarBotones() {
        binding.btnDescargarPdf.setOnClickListener {
            descargarPdf()
        }

        binding.btnVerCitas.setOnClickListener {
            // Navegamos a la pestaña de citas con el ID de esta mascota
            // Por ahora mostramos Toast — en la Vertical 3 lo implementamos
            Toast.makeText(this, "Citas de $nombreMascota — próximamente", Toast.LENGTH_SHORT).show()
        }

        binding.btnActivarAlerta.setOnClickListener {
            // Vertical 5: alertas de pérdida
            Toast.makeText(this, "Alerta de pérdida — próximamente", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * En esta versión mostramos los datos que ya tenemos del Intent.
     * Cuando tengamos un endpoint GET /api/mascotas/{id} en el backend,
     * haremos aquí la llamada Retrofit completa.
     *
     * Por ahora mostramos los datos básicos y cargamos la foto si hay URL.
     */
    private fun cargarDatosMascota() {
        // Los datos básicos ya los tenemos del Fragment anterior
        // Solo necesitamos cargar la foto con Glide si hay URL
        // En la siguiente iteración añadiremos GET /api/mascotas/{id}

        // Placeholder: emoji de pata como fondo de la imagen
        binding.ivFotoMascota.setBackgroundColor(
            resources.getColor(R.color.haru_teal, theme)
        )
    }

    /**
     * Carga el historial médico de la mascota.
     * GET /api/historial/mascota/{idMascota}
     */
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

                    // Mostrar estado vacío si no hay registros
                    binding.tvSinHistorial.visibility =
                        if (historial.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvHistorial.visibility =
                        if (historial.isEmpty()) View.GONE else View.VISIBLE

                } else {
                    binding.progressBarHistorial.visibility = View.GONE
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

    /**
     * Descarga el PDF del historial médico y lo abre con el visor del sistema.
     * GET /api/pdf/historial/{idMascota}
     *
     * El servidor devuelve bytes crudos (ResponseBody), no JSON.
     * Los guardamos en el almacenamiento externo y los abrimos con un Intent.
     */
    private fun descargarPdf() {
        Toast.makeText(this, "Descargando PDF...", Toast.LENGTH_SHORT).show()
        binding.btnDescargarPdf.isEnabled = false

        val api = RetrofitClient
            .getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)

        api.descargarHistorialPdf(idMascota).enqueue(object : Callback<ResponseBody> {

            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                binding.btnDescargarPdf.isEnabled = true

                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        guardarYAbrirPdf(body)
                    }
                } else {
                    Toast.makeText(
                        this@DetalleMascotaActivity,
                        "Error al descargar PDF",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                binding.btnDescargarPdf.isEnabled = true
                Toast.makeText(
                    this@DetalleMascotaActivity,
                    "Error de conexión: ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    /**
     * Guarda el PDF en el directorio de caché y lo abre con el visor del sistema.
     *
     * Usamos el directorio de caché (cacheDir) para no necesitar permisos
     * de almacenamiento externo en Android 10+.
     * FileProvider expone el archivo de forma segura a otras apps (el visor de PDF).
     */
    private fun guardarYAbrirPdf(body: ResponseBody) {
        try {
            val archivo = File(cacheDir, "historial_${nombreMascota}_$idMascota.pdf")

            // Escribimos los bytes recibidos en el archivo
            FileOutputStream(archivo).use { output ->
                body.byteStream().use { input ->
                    input.copyTo(output)
                }
            }

            // Abrimos el PDF con el visor instalado en el dispositivo
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.provider", // debe coincidir con el FileProvider del Manifest
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