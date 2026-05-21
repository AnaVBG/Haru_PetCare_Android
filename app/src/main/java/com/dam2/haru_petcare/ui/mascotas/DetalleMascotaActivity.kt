package com.dam2.haru_petcare.ui.mascotas

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.dam2.haru_petcare.R
import com.dam2.haru_petcare.databinding.ActivityDetalleMascotaBinding
import com.dam2.haru_petcare.databinding.BottomSheetEditarMascotaBinding
import com.dam2.haru_petcare.model.HistorialMedicoDTO
import com.dam2.haru_petcare.model.MascotaActualizarDTO
import com.dam2.haru_petcare.model.MascotaDTO
import com.dam2.haru_petcare.network.HaruApiService
import com.dam2.haru_petcare.network.RetrofitClient
import com.dam2.haru_petcare.ui.main.MainActivity
import com.dam2.haru_petcare.util.Constants
import com.dam2.haru_petcare.util.SessionManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

class DetalleMascotaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetalleMascotaBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var historialAdapter: HistorialAdapter

    private var idMascota: Long = -1L
    private var nombreMascota: String = ""
    private var mascotaActual: MascotaDTO? = null

    private var editarBinding: BottomSheetEditarMascotaBinding? = null
    private var fotoUriEditar: Uri? = null
    private var fechaSeleccionadaEditar: String = ""

    private val seleccionarFotoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            fotoUriEditar = it
            editarBinding?.let { bs ->
                Glide.with(this).load(it).centerCrop().into(bs.civFotoEditar)
                bs.civFotoEditar.visibility = View.VISIBLE
                bs.tvInicialEditar.visibility = View.GONE
            }
        }
    }

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
        binding.fabEditar.setOnClickListener {
            mascotaActual?.let { abrirBottomSheetEditar(it) }
                ?: Toast.makeText(this, "Cargando datos...", Toast.LENGTH_SHORT).show()
        }

        binding.btnDescargarPdf.setOnClickListener { descargarPdf() }

        binding.btnVerCitas.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra("tab_destino", R.id.nav_citas)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
        }

        binding.btnActivarAlerta.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra("tab_destino", R.id.nav_alertas)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
        }
    }

    private fun cargarDatosMascota() {
        RetrofitClient.getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)
            .getMascotaPorId(idMascota)
            .enqueue(object : Callback<MascotaDTO> {
                override fun onResponse(call: Call<MascotaDTO>, response: Response<MascotaDTO>) {
                    if (response.isSuccessful) {
                        response.body()?.let { actualizarUiConMascota(it) }
                    }
                }
                override fun onFailure(call: Call<MascotaDTO>, t: Throwable) {}
            })
    }

    private fun actualizarUiConMascota(mascota: MascotaDTO) {
        mascotaActual = mascota
        nombreMascota = mascota.nombre ?: nombreMascota

        binding.tvDetalleEspecie.text   = mascota.especie ?: "—"
        binding.tvDetalleRaza.text      = mascota.raza    ?: "—"
        binding.tvDetalleFechaNac.text  = formatearFecha(mascota.fechaNacimiento)
        binding.collapsingToolbar.title = mascota.nombre ?: nombreMascota

        if (!mascota.fotoUrl.isNullOrBlank() && mascota.fotoUrl.startsWith("http")) {
            Glide.with(this).load(mascota.fotoUrl).centerCrop().into(binding.ivFotoMascota)
        }
    }

    // ── Bottom sheet editar ────────────────────────────────────────────────

    private fun abrirBottomSheetEditar(mascota: MascotaDTO) {
        fotoUriEditar = null
        fechaSeleccionadaEditar = mascota.fechaNacimiento ?: ""

        val dialog = BottomSheetDialog(this)
        val bs = BottomSheetEditarMascotaBinding.inflate(layoutInflater)
        editarBinding = bs
        dialog.setContentView(bs.root)
        dialog.setOnDismissListener { editarBinding = null }

        // Pre-rellenar campos
        bs.etNombreEditar.setText(mascota.nombre)
        bs.etEspecieEditar.setText(mascota.especie)
        bs.etRazaEditar.setText(mascota.raza)
        val fecha = mascota.fechaNacimiento
        if (!fecha.isNullOrBlank()) {
            val p = fecha.split("-")
            if (p.size == 3) bs.etFechaNacEditar.setText("${p[2]}/${p[1]}/${p[0]}")
        }

        // Foto actual
        bs.tvInicialEditar.text = mascota.nombre?.firstOrNull()?.uppercase() ?: "?"
        if (!mascota.fotoUrl.isNullOrBlank() && mascota.fotoUrl.startsWith("http")) {
            Glide.with(this).load(mascota.fotoUrl).centerCrop()
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(e: com.bumptech.glide.load.engine.GlideException?, model: Any?,
                                              target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>, isFirstResource: Boolean): Boolean {
                        bs.civFotoEditar.visibility = View.GONE; bs.tvInicialEditar.visibility = View.VISIBLE; return false
                    }
                    override fun onResourceReady(resource: android.graphics.drawable.Drawable, model: Any,
                                                 target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                                                 dataSource: com.bumptech.glide.load.DataSource, isFirstResource: Boolean): Boolean {
                        bs.civFotoEditar.visibility = View.VISIBLE; bs.tvInicialEditar.visibility = View.GONE; return false
                    }
                }).into(bs.civFotoEditar)
        } else {
            bs.civFotoEditar.visibility = View.GONE
            bs.tvInicialEditar.visibility = View.VISIBLE
        }

        // Date picker
        val abrirDatePicker = View.OnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                val mes = String.format("%02d", m + 1)
                val dia = String.format("%02d", d)
                fechaSeleccionadaEditar = "$y-$mes-$dia"
                bs.etFechaNacEditar.setText("$dia/$mes/$y")
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                .also { it.datePicker.maxDate = System.currentTimeMillis() }
                .show()
        }
        bs.etFechaNacEditar.setOnClickListener(abrirDatePicker)

        // Cambiar foto
        bs.fabCambiarFotoEditar.setOnClickListener {
            seleccionarFotoLauncher.launch("image/*")
        }

        // Guardar
        bs.btnGuardarEditar.setOnClickListener {
            val nombre  = bs.etNombreEditar.text.toString().trim()
            val especie = bs.etEspecieEditar.text.toString().trim()
            val raza    = bs.etRazaEditar.text.toString().trim()

            bs.tilNombreEditar.error = null
            bs.tilEspecieEditar.error = null
            bs.tilFechaNacEditar.error = null

            if (nombre.isEmpty())  { bs.tilNombreEditar.error = "Obligatorio"; return@setOnClickListener }
            if (especie.isEmpty()) { bs.tilEspecieEditar.error = "Obligatorio"; return@setOnClickListener }
            if (fechaSeleccionadaEditar.isEmpty()) { bs.tilFechaNacEditar.error = "Selecciona la fecha"; return@setOnClickListener }

            mostrarCargandoEditar(true)

            if (fotoUriEditar != null) {
                subirFotoYActualizar(fotoUriEditar!!, nombre, especie, raza, dialog)
            } else {
                actualizarMascotaEnBackend(nombre, especie, raza, mascotaActual?.fotoUrl, dialog)
            }
        }

        dialog.show()
    }

    private fun mostrarCargandoEditar(cargando: Boolean) {
        editarBinding?.progressBarEditar?.visibility = if (cargando) View.VISIBLE else View.GONE
        editarBinding?.btnGuardarEditar?.isEnabled   = !cargando
        editarBinding?.fabCambiarFotoEditar?.isEnabled = !cargando
    }

    private fun subirFotoYActualizar(uri: Uri, nombre: String, especie: String, raza: String, dialog: BottomSheetDialog) {
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
        if (bytes == null) {
            mostrarCargandoEditar(false)
            Toast.makeText(this, "No se pudo leer la imagen", Toast.LENGTH_SHORT).show()
            return
        }

        val body = okhttp3.MultipartBody.Builder()
            .setType(okhttp3.MultipartBody.FORM)
            .addFormDataPart("file", "mascota_${idMascota}_${System.currentTimeMillis()}.jpg",
                okhttp3.RequestBody.create("image/jpeg".toMediaTypeOrNull(), bytes))
            .addFormDataPart("upload_preset", Constants.CLOUDINARY_UPLOAD_PRESET)
            .build()

        okhttp3.OkHttpClient().newCall(
            okhttp3.Request.Builder().url(Constants.CLOUDINARY_UPLOAD_URL).post(body).build()
        ).enqueue(object : okhttp3.Callback {
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val bodyStr = response.body?.string()
                if (response.isSuccessful && bodyStr != null) {
                    val url = org.json.JSONObject(bodyStr).getString("secure_url")
                    runOnUiThread { actualizarMascotaEnBackend(nombre, especie, raza, url, dialog) }
                } else {
                    runOnUiThread {
                        mostrarCargandoEditar(false)
                        Toast.makeText(this@DetalleMascotaActivity,
                            "Error al subir la foto (${response.code})", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                runOnUiThread {
                    mostrarCargandoEditar(false)
                    Toast.makeText(this@DetalleMascotaActivity,
                        "Sin conexión: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun actualizarMascotaEnBackend(nombre: String, especie: String, raza: String,
                                           fotoUrl: String?, dialog: BottomSheetDialog) {
        val dto = MascotaActualizarDTO(
            nombre          = nombre,
            especie         = especie,
            raza            = raza.ifEmpty { null },
            fechaNacimiento = fechaSeleccionadaEditar,
            fotoUrl         = fotoUrl
        )

        RetrofitClient.getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)
            .actualizarMascota(idMascota, dto)
            .enqueue(object : Callback<MascotaDTO> {
                override fun onResponse(call: Call<MascotaDTO>, response: Response<MascotaDTO>) {
                    mostrarCargandoEditar(false)
                    if (response.isSuccessful) {
                        response.body()?.let { actualizarUiConMascota(it) }
                        dialog.dismiss()
                        Toast.makeText(this@DetalleMascotaActivity,
                            "Cambios guardados", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@DetalleMascotaActivity,
                            "Error al guardar (${response.code()})", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<MascotaDTO>, t: Throwable) {
                    mostrarCargandoEditar(false)
                    Toast.makeText(this@DetalleMascotaActivity,
                        "Sin conexión: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
    }

    // ── Historial ─────────────────────────────────────────────────────────

    private fun cargarHistorial() {
        binding.progressBarHistorial.visibility = View.VISIBLE
        RetrofitClient.getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)
            .getHistorial(idMascota)
            .enqueue(object : Callback<List<HistorialMedicoDTO>> {
                override fun onResponse(call: Call<List<HistorialMedicoDTO>>, response: Response<List<HistorialMedicoDTO>>) {
                    binding.progressBarHistorial.visibility = View.GONE
                    if (response.isSuccessful) {
                        val historial = response.body() ?: emptyList()
                        historialAdapter.setRegistros(historial)
                        binding.tvSinHistorial.visibility = if (historial.isEmpty()) View.VISIBLE else View.GONE
                        binding.rvHistorial.visibility    = if (historial.isEmpty()) View.GONE   else View.VISIBLE
                    } else {
                        Toast.makeText(this@DetalleMascotaActivity,
                            "Error al cargar historial (${response.code()})", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<List<HistorialMedicoDTO>>, t: Throwable) {
                    binding.progressBarHistorial.visibility = View.GONE
                    Toast.makeText(this@DetalleMascotaActivity,
                        "Sin conexión: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
    }

    // ── PDF ───────────────────────────────────────────────────────────────

    private fun descargarPdf() {
        Toast.makeText(this, "Descargando PDF...", Toast.LENGTH_SHORT).show()
        binding.btnDescargarPdf.isEnabled = false
        RetrofitClient.getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)
            .descargarHistorialPdf(idMascota)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    binding.btnDescargarPdf.isEnabled = true
                    if (response.isSuccessful) response.body()?.let { guardarYAbrirPdf(it) }
                    else Toast.makeText(this@DetalleMascotaActivity, "Error al descargar PDF", Toast.LENGTH_SHORT).show()
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
            FileOutputStream(archivo).use { out -> body.byteStream().use { it.copyTo(out) } }
            val uri = androidx.core.content.FileProvider.getUriForFile(this, "${packageName}.provider", archivo)
            startActivity(Intent.createChooser(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                }, "Abrir PDF con..."
            ))
        } catch (e: Exception) {
            Toast.makeText(this, "Error al abrir el PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun formatearFecha(fechaIso: String?): String {
        if (fechaIso == null) return "—"
        return try {
            val p = fechaIso.split("-")
            "${p[2]}/${p[1]}/${p[0]}"
        } catch (e: Exception) { fechaIso }
    }

    override fun onDestroy() { super.onDestroy() }
}