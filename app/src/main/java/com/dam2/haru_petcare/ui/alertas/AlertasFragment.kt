package com.dam2.haru_petcare.ui.alertas

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dam2.haru_petcare.databinding.BottomSheetCrearAlertaBinding
import com.dam2.haru_petcare.databinding.FragmentAlertasBinding
import com.dam2.haru_petcare.model.AlertaInsertarDTO
import com.dam2.haru_petcare.model.AlertaPerdidaDTO
import com.dam2.haru_petcare.model.MascotaDTO
import com.dam2.haru_petcare.network.HaruApiService
import com.dam2.haru_petcare.network.RetrofitClient
import com.dam2.haru_petcare.util.SessionManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.messaging.FirebaseMessaging
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AlertasFragment : Fragment() {

    private var _binding: FragmentAlertasBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var adapter: AlertaAdapter

    // Lista de mascotas del usuario para el Spinner del BottomSheet
    private var misMascotas = listOf<MascotaDTO>()

    private val permisosLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        val concedido = permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true
                || permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (concedido) {
            mostrarBottomSheetCrearAlerta()
        } else {
            Toast.makeText(
                requireContext(),
                "Necesitamos tu ubicación para enviar la alerta",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlertasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager      = SessionManager(requireContext())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        configurarAdapter()
        cargarAlertas()
        cargarMisMascotas()
        registrarTokenFcm()

        binding.btnEmergencia.setOnClickListener {
            pedirPermisosYMostrarBottomSheet()
        }
    }

    private fun configurarAdapter() {
        adapter = AlertaAdapter(
            idUsuarioLogueado = sessionManager.getIdUsuario(),
            onResolverAlerta  = { alerta -> mostrarDialogoResolver(alerta) }
        )
        binding.rvAlertas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter       = this@AlertasFragment.adapter
            itemAnimator?.changeDuration = 0
        }
    }

    /**
     * Carga todas las alertas activas.
     * GET /api/alertas/activas
     */
    private fun cargarAlertas() {
        binding.progressBarAlertas.visibility = View.VISIBLE

        val api = RetrofitClient
            .getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)

        api.getAlertasActivas().enqueue(object : Callback<List<AlertaPerdidaDTO>> {

            override fun onResponse(
                call: Call<List<AlertaPerdidaDTO>>,
                response: Response<List<AlertaPerdidaDTO>>
            ) {
                if (!isAdded) return
                binding.progressBarAlertas.visibility = View.GONE

                if (response.isSuccessful) {
                    val alertas = response.body() ?: emptyList()
                    adapter.setAlertas(alertas)

                    binding.layoutSinAlertas.visibility =
                        if (alertas.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvAlertas.visibility =
                        if (alertas.isEmpty()) View.GONE else View.VISIBLE
                } else {
                    mostrarError("Error al cargar alertas (${response.code()})")
                }
            }

            override fun onFailure(call: Call<List<AlertaPerdidaDTO>>, t: Throwable) {
                if (!isAdded) return
                binding.progressBarAlertas.visibility = View.GONE
                mostrarError("Sin conexión: ${t.message}")
            }
        })
    }

    /**
     * Carga las mascotas del usuario para el Spinner del BottomSheet.
     * Necesitamos saber qué mascota se ha perdido antes de crear la alerta.
     */
    private fun cargarMisMascotas() {
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
                    if (response.isSuccessful) {
                        misMascotas = response.body() ?: emptyList()
                    }
                }

                override fun onFailure(call: Call<List<MascotaDTO>>, t: Throwable) {
                    // Si falla, el Spinner estará vacío — lo gestionamos en el BottomSheet
                }
            })
    }

    /**
     * Registra el token FCM del dispositivo en el backend.
     * Se llama cada vez que el Fragment arranca para mantenerlo actualizado.
     *
     * FirebaseMessaging.getInstance().token obtiene el token actual del
     * dispositivo de forma asíncrona con un listener de éxito/fallo.
     */
    private fun registrarTokenFcm() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful || !isAdded) return@addOnCompleteListener

            val tokenFcm = task.result

            val api = RetrofitClient
                .getClient(sessionManager.getToken())
                .create(HaruApiService::class.java)

            api.actualizarTokenFcm(sessionManager.getIdUsuario(), tokenFcm)
                .enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        // Token registrado — ahora el backend puede enviar push a este dispositivo
                    }
                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        // Si falla, lo intentará la próxima vez que abra el Fragment
                    }
                })
        }
    }

    // ── PERMISOS ─────────────────────────────────────────────────────────

    private fun pedirPermisosYMostrarBottomSheet() {
        val tienePermiso = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (tienePermiso) {
            mostrarBottomSheetCrearAlerta()
        } else {
            permisosLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    // ── BOTTOM SHEET CREAR ALERTA ─────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private fun mostrarBottomSheetCrearAlerta() {
        if (misMascotas.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Primero registra una mascota antes de crear una alerta",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val dialog    = BottomSheetDialog(requireContext())
        val bsBinding = BottomSheetCrearAlertaBinding.inflate(layoutInflater)
        dialog.setContentView(bsBinding.root)

        // Rellenamos el Spinner con los nombres de las mascotas del usuario
        val nombresMascotas = misMascotas.map { it.nombre ?: "Sin nombre" }
        val spinnerAdapter  = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            nombresMascotas
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        bsBinding.spinnerMascota.adapter = spinnerAdapter

        bsBinding.btnCancelarAlerta.setOnClickListener { dialog.dismiss() }

        bsBinding.btnConfirmarAlerta.setOnClickListener {
            val indiceMascota = bsBinding.spinnerMascota.selectedItemPosition
            val mascotaElegida = misMascotas.getOrNull(indiceMascota)

            if (mascotaElegida == null) {
                Toast.makeText(requireContext(), "Selecciona una mascota", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val mensaje = bsBinding.etMensajeAlerta.text.toString().trim()
            dialog.dismiss()

            // Obtenemos la ubicación actual y creamos la alerta
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (!isAdded) return@addOnSuccessListener

                if (location == null) {
                    Toast.makeText(
                        requireContext(),
                        "No se pudo obtener tu ubicación. Activa el GPS e inténtalo de nuevo.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                crearAlerta(
                    lat      = location.latitude,
                    lng      = location.longitude,
                    mensaje  = mensaje,
                    idMascota = mascotaElegida.id ?: return@addOnSuccessListener
                )
            }
        }

        dialog.show()
    }

    // ── LLAMADAS RETROFIT ─────────────────────────────────────────────────

    /**
     * Crea la alerta de pérdida.
     * POST /api/alertas
     * El backend la guarda y dispara las notificaciones FCM a los vecinos.
     */
    private fun crearAlerta(lat: Double, lng: Double, mensaje: String, idMascota: Long) {
        binding.btnEmergencia.isEnabled = false

        val dto = AlertaInsertarDTO(
            ultimaUbicacionLat = lat,
            ultimaUbicacionLng = lng,
            mensajeAdicional   = mensaje,
            idMascota          = idMascota,
            idUsuario          = sessionManager.getIdUsuario()
        )

        val api = RetrofitClient
            .getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)

        api.crearAlerta(dto).enqueue(object : Callback<AlertaPerdidaDTO> {

            override fun onResponse(
                call: Call<AlertaPerdidaDTO>,
                response: Response<AlertaPerdidaDTO>
            ) {
                if (!isAdded) return
                binding.btnEmergencia.isEnabled = true

                if (response.isSuccessful) {
                    Toast.makeText(
                        requireContext(),
                        "🚨 Alerta enviada. Los vecinos han sido notificados.",
                        Toast.LENGTH_LONG
                    ).show()
                    // Recargamos la lista para mostrar la alerta recién creada
                    cargarAlertas()
                } else {
                    mostrarError("Error al enviar la alerta (${response.code()})")
                }
            }

            override fun onFailure(call: Call<AlertaPerdidaDTO>, t: Throwable) {
                if (!isAdded) return
                binding.btnEmergencia.isEnabled = true
                mostrarError("Sin conexión: ${t.message}")
            }
        })
    }

    /**
     * Marca una alerta como resuelta.
     * PUT /api/alertas/{id}/resolver
     */
    private fun resolverAlerta(alerta: AlertaPerdidaDTO) {
        val idAlerta = alerta.id ?: return

        val api = RetrofitClient
            .getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)

        api.resolverAlerta(idAlerta).enqueue(object : Callback<Void> {

            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (!isAdded) return

                if (response.isSuccessful) {
                    Toast.makeText(
                        requireContext(),
                        "✅ ¡Qué alegría! Mascota marcada como encontrada.",
                        Toast.LENGTH_LONG
                    ).show()
                    cargarAlertas() // Recargamos — la alerta desaparece de la lista
                } else {
                    mostrarError("Error al resolver la alerta (${response.code()})")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                if (!isAdded) return
                mostrarError("Sin conexión: ${t.message}")
            }
        })
    }

    private fun mostrarDialogoResolver(alerta: AlertaPerdidaDTO) {
        AlertDialog.Builder(requireContext())
            .setTitle("¿Mascota encontrada?")
            .setMessage("¿Confirmas que ${alerta.nombreMascota ?: "tu mascota"} ya está en casa?")
            .setPositiveButton("Sí, la encontré") { _, _ -> resolverAlerta(alerta) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarError(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}