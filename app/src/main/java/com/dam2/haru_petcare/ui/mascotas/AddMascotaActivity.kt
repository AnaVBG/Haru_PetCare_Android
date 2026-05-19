package com.dam2.haru_petcare.ui.mascotas

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.dam2.haru_petcare.databinding.ActivityAddMascotaBinding
import com.dam2.haru_petcare.model.MascotaDTO
import com.dam2.haru_petcare.model.MascotaInsertarDTO
import com.dam2.haru_petcare.network.HaruApiService
import com.dam2.haru_petcare.network.RetrofitClient
import com.dam2.haru_petcare.util.SessionManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDate
import java.util.Calendar

class AddMascotaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddMascotaBinding
    private lateinit var sessionManager: SessionManager

    private var fechaSeleccionada: String = ""
    private var uriFotoSeleccionada: Uri? = null

    private val galeriaLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            uriFotoSeleccionada = it
            binding.ivFotoMascota.scaleType = ImageView.ScaleType.CENTER_CROP
            Glide.with(this).load(it).centerCrop().into(binding.ivFotoMascota)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddMascotaBinding.inflate(layoutInflater)
        sessionManager = SessionManager(this)
        setContentView(binding.root)

        configurarToolbar()
        configurarSelectorFoto()
        configurarDatePicker()
        configurarBotonGuardar()
    }

    private fun configurarToolbar() {
        setSupportActionBar(binding.toolbarAddMascota)
        binding.toolbarAddMascota.setNavigationOnClickListener { finish() }
    }

    private fun configurarSelectorFoto() {
        binding.ivFotoMascota.setOnClickListener {
            galeriaLauncher.launch("image/*")
        }
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

        val dto = MascotaInsertarDTO(
            nombre = nombre,
            especie = especie,
            raza = raza.ifEmpty { "Desconocida" },
            fechaNacimiento = LocalDate.parse(fechaSeleccionada),
            duenoId = sessionManager.getIdUsuario()
        )

        val api = RetrofitClient
            .getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)

        api.insertarMascota(dto).enqueue(object : Callback<MascotaDTO> {

            override fun onResponse(call: Call<MascotaDTO>, response: Response<MascotaDTO>) {
                if (response.isSuccessful) {
                    val mascotaCreada = response.body()
                    val uri = uriFotoSeleccionada
                    if (uri != null && mascotaCreada != null) {
                        val id = mascotaCreada.id ?: run {
                            mostrarCargando(false)
                            finalizarConExito(mascotaCreada.nombre ?: "Mascota")
                            return
                        }
                        subirFoto(id, uri, mascotaCreada.nombre ?: "Mascota")
                    } else {
                        mostrarCargando(false)
                        Toast.makeText(
                            this@AddMascotaActivity,
                            "¡${mascotaCreada?.nombre} añadida correctamente! 🐾",
                            Toast.LENGTH_LONG
                        ).show()
                        setResult(RESULT_OK)
                        finish()
                    }
                } else {
                    mostrarCargando(false)
                    mostrarError("Error al guardar (${response.code()})")
                }
            }

            override fun onFailure(call: Call<MascotaDTO>, t: Throwable) {
                mostrarCargando(false)
                mostrarError("Sin conexión: ${t.message}")
            }
        })
    }

    private fun subirFoto(idMascota: Long, uri: Uri, nombreMascota: String) {
        val inputStream = contentResolver.openInputStream(uri) ?: run {
            mostrarCargando(false)
            finalizarConExito(nombreMascota)
            return
        }
        val bytes = inputStream.readBytes()
        inputStream.close()

        val mediaType = (contentResolver.getType(uri) ?: "image/jpeg").toMediaTypeOrNull()
        val requestBody = bytes.toRequestBody(mediaType)
        val part = MultipartBody.Part.createFormData("foto", "foto_mascota.jpg", requestBody)

        val api = RetrofitClient
            .getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)

        api.subirFotoMascota(idMascota, part).enqueue(object : Callback<MascotaDTO> {
            override fun onResponse(call: Call<MascotaDTO>, response: Response<MascotaDTO>) {
                mostrarCargando(false)
                finalizarConExito(nombreMascota)
            }

            override fun onFailure(call: Call<MascotaDTO>, t: Throwable) {
                mostrarCargando(false)
                // La mascota se creó, solo falló la subida de foto
                Toast.makeText(
                    this@AddMascotaActivity,
                    "¡$nombreMascota añadida! (la foto no se pudo subir)",
                    Toast.LENGTH_LONG
                ).show()
                setResult(RESULT_OK)
                finish()
            }
        })
    }

    private fun finalizarConExito(nombreMascota: String) {
        Toast.makeText(this, "¡$nombreMascota añadida correctamente! 🐾", Toast.LENGTH_LONG).show()
        setResult(RESULT_OK)
        finish()
    }

    private fun limpiarErrores() {
        binding.tilNombreMascota.error = null
        binding.tilEspecie.error = null
        binding.tilFechaNacimiento.error = null
    }

    private fun mostrarCargando(cargando: Boolean) {
        binding.progressBarAniadir.visibility = if (cargando) View.VISIBLE else View.GONE
        binding.btnGuardarMascota.isEnabled = !cargando
    }

    private fun mostrarError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}