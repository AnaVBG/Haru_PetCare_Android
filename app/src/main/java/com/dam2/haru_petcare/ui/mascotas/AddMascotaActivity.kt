package com.dam2.haru_petcare.ui.mascotas

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.dam2.haru_petcare.databinding.ActivityAddMascotaBinding
import com.dam2.haru_petcare.model.MascotaDTO
import com.dam2.haru_petcare.model.MascotaInsertarDTO
import com.dam2.haru_petcare.network.HaruApiService
import com.dam2.haru_petcare.network.RetrofitClient
import com.dam2.haru_petcare.util.Constants
import com.dam2.haru_petcare.util.SessionManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.time.LocalDate
import java.util.Calendar

class AddMascotaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddMascotaBinding
    private lateinit var sessionManager: SessionManager

    private var fechaSeleccionada: String = ""
    private var imagenUri: Uri? = null

    private val seleccionarImagenLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imagenUri = it
            Glide.with(this)
                .load(it)
                .centerCrop()
                .into(binding.ivFotoPreview)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddMascotaBinding.inflate(layoutInflater)
        sessionManager = SessionManager(this)
        setContentView(binding.root)

        configurarToolbar()
        configurarDatePicker()
        configurarSeleccionFoto()
        configurarBotonGuardar()
    }

    private fun configurarToolbar() {
        setSupportActionBar(binding.toolbarAddMascota)
        binding.toolbarAddMascota.setNavigationOnClickListener { finish() }
    }

    private fun configurarDatePicker() {
        val abrirDatePicker = View.OnClickListener {
            val calendario = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, anio, mes, dia ->
                    val mesFormateado = String.format("%02d", mes + 1)
                    val diaFormateado = String.format("%02d", dia)
                    fechaSeleccionada = "$anio-$mesFormateado-$diaFormateado"
                    binding.etFechaNacimiento.setText("$diaFormateado/$mesFormateado/$anio")
                },
                calendario.get(Calendar.YEAR),
                calendario.get(Calendar.MONTH),
                calendario.get(Calendar.DAY_OF_MONTH)
            ).also { picker ->
                picker.datePicker.maxDate = System.currentTimeMillis()
            }.show()
        }
        binding.etFechaNacimiento.setOnClickListener(abrirDatePicker)
        binding.tilFechaNacimiento.setEndIconOnClickListener(abrirDatePicker)
    }

    private fun configurarSeleccionFoto() {
        val abrirGaleria = View.OnClickListener {
            seleccionarImagenLauncher.launch("image/*")
        }
        binding.ivFotoPreview.setOnClickListener(abrirGaleria)
        binding.fabSeleccionarFoto.setOnClickListener(abrirGaleria)
    }

    private fun configurarBotonGuardar() {
        binding.btnGuardarMascota.setOnClickListener { intentarGuardar() }
    }

    private fun intentarGuardar() {
        val nombre  = binding.etNombreMascota.text.toString().trim()
        val especie = binding.etEspecie.text.toString().trim()
        val raza    = binding.etRaza.text.toString().trim()

        limpiarErrores()

        if (nombre.isEmpty()) {
            binding.tilNombreMascota.error = "El nombre es obligatorio"
            return
        }
        if (especie.isEmpty()) {
            binding.tilEspecie.error = "La especie es obligatoria"
            return
        }
        if (fechaSeleccionada.isEmpty()) {
            binding.tilFechaNacimiento.error = "Selecciona la fecha de nacimiento"
            return
        }

        mostrarCargando(true)

        if (imagenUri != null) {
            subirImagenYGuardar(nombre, especie, raza)
        } else {
            guardarMascota(nombre, especie, raza, fotoUrl = null)
        }
    }

    /**
     * Sube la imagen directamente a Cloudinary usando el preset unsigned.
     * No se necesita API Key ni Secret — el preset lo permite sin autenticación.
     * Usa las constantes centralizadas de Constants.kt.
     */
    private fun subirImagenYGuardar(nombre: String, especie: String, raza: String) {
        val uri = imagenUri ?: return

        val inputStream = contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()

        if (bytes == null) {
            mostrarCargando(false)
            mostrarError("No se pudo leer la imagen seleccionada")
            return
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                "mascota_${System.currentTimeMillis()}.jpg",
                RequestBody.create("image/jpeg".toMediaTypeOrNull(), bytes)
            )
            // ✅ Usamos la constante en vez del literal hardcodeado
            .addFormDataPart("upload_preset", Constants.CLOUDINARY_UPLOAD_PRESET)
            .build()

        val request = Request.Builder()
            // ✅ Usamos la URL construida en Constants
            .url(Constants.CLOUDINARY_UPLOAD_URL)
            .post(requestBody)
            .build()

        OkHttpClient().newCall(request).enqueue(object : okhttp3.Callback {

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val bodyStr = response.body?.string()

                if (response.isSuccessful && bodyStr != null) {
                    val secureUrl = JSONObject(bodyStr).getString("secure_url")
                    runOnUiThread {
                        guardarMascota(nombre, especie, raza, fotoUrl = secureUrl)
                    }
                } else {
                    runOnUiThread {
                        mostrarCargando(false)
                        mostrarError("Error al subir la imagen (${response.code})")
                    }
                }
            }

            override fun onFailure(call: okhttp3.Call, e: IOException) {
                runOnUiThread {
                    mostrarCargando(false)
                    mostrarError("Sin conexión al subir imagen: ${e.message}")
                }
            }
        })
    }

    private fun guardarMascota(
        nombre: String,
        especie: String,
        raza: String,
        fotoUrl: String?
    ) {
        val dto = MascotaInsertarDTO(
            nombre          = nombre,
            especie         = especie,
            raza            = raza.ifEmpty { "Desconocida" },
            fechaNacimiento = LocalDate.parse(fechaSeleccionada),
            duenoId         = sessionManager.getIdUsuario(),
            fotoUrl         = fotoUrl
        )

        val api = RetrofitClient
            .getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)

        api.insertarMascota(dto).enqueue(object : Callback<MascotaDTO> {

            override fun onResponse(call: Call<MascotaDTO>, response: Response<MascotaDTO>) {
                mostrarCargando(false)
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@AddMascotaActivity,
                        "¡${response.body()?.nombre} añadida correctamente! 🐾",
                        Toast.LENGTH_LONG
                    ).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    mostrarError("Error al guardar (${response.code()})")
                }
            }

            override fun onFailure(call: Call<MascotaDTO>, t: Throwable) {
                mostrarCargando(false)
                mostrarError("Sin conexión: ${t.message}")
            }
        })
    }

    private fun limpiarErrores() {
        binding.tilNombreMascota.error   = null
        binding.tilEspecie.error         = null
        binding.tilFechaNacimiento.error = null
    }

    private fun mostrarCargando(cargando: Boolean) {
        binding.progressBarAniadir.visibility = if (cargando) View.VISIBLE else View.GONE
        binding.btnGuardarMascota.isEnabled   = !cargando
    }

    private fun mostrarError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}