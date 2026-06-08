package com.dam2.haru_petcare.ui.mapa

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.dam2.haru_petcare.R
import com.dam2.haru_petcare.databinding.BottomSheetCrearPinBinding
import com.dam2.haru_petcare.databinding.BottomSheetDetallePinBinding
import com.dam2.haru_petcare.databinding.FragmentMapaBinding
import com.dam2.haru_petcare.model.AlertaPerdidaDTO
import com.dam2.haru_petcare.model.PinInsertarDTO
import com.dam2.haru_petcare.model.PinMapaDTO
import com.dam2.haru_petcare.network.HaruApiService
import com.dam2.haru_petcare.network.RetrofitClient
import com.dam2.haru_petcare.util.SessionManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MapaFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapaBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var posicionElegida: LatLng? = null
    private var marcadorTemporal: Marker? = null
    private var googleMap: GoogleMap? = null
    private var miUbicacion: Location? = null
    private val markerPinMap = mutableMapOf<Marker, PinMapaDTO>()
    private val markerAlertaMap = mutableMapOf<Marker, AlertaPerdidaDTO>()

    private val permisosUbicacionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        val concedido = permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true
                || permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (concedido) activarCapaUbicacion()
        else Toast.makeText(requireContext(), "Permiso denegado", Toast.LENGTH_SHORT).show()
    }

    private data class PinVisual(val iconoRes: Int, val colorFondo: Int, val etiqueta: String)

    private fun visualParaTipo(tipo: String?): PinVisual = when (tipo?.uppercase()) {
        "FUENTE"   -> PinVisual(R.drawable.ic_pin_fuente,   R.color.pin_fuente_bg,   "Fuente de agua")
        "PARQUE"   -> PinVisual(R.drawable.ic_pin_parque,   R.color.pin_parque_bg,   "Parque")
        "PAPELERA" -> PinVisual(R.drawable.ic_pin_papelera, R.color.pin_papelera_bg, "Papelera")
        "PELIGRO"  -> PinVisual(R.drawable.ic_pin_peligro,  R.color.pin_peligro_bg,  "Zona de peligro")
        else       -> PinVisual(R.drawable.ic_pin_fuente,   R.color.pin_fuente_bg,   "Punto de interés")
    }

    private fun colorMarkerParaTipo(tipo: String?): Int = when (tipo?.uppercase()) {
        "FUENTE"   -> R.color.pin_fuente
        "PARQUE"   -> R.color.pin_parque
        "PAPELERA" -> R.color.pin_papelera
        "PELIGRO"  -> R.color.pin_peligro
        else       -> R.color.haru_teal
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        configurarFabs()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.apply {
            isZoomControlsEnabled = false
            isMyLocationButtonEnabled = false
            isMapToolbarEnabled = false
        }

        map.setOnMarkerClickListener { marker ->
            val pin = markerPinMap[marker]
            val alerta = markerAlertaMap[marker]
            when {
                pin    != null -> mostrarBottomSheetDetalle(pin)
                alerta != null -> mostrarDialogoAlerta(alerta)
            }
            true
        }

        map.setOnMapLongClickListener { latLng ->
            posicionElegida = latLng
            marcadorTemporal?.remove()
            marcadorTemporal = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
            )
            mostrarBottomSheetCrearPin()
        }

        pedirPermisosUbicacion()
        cargarPines()
    }

    private fun pedirPermisosUbicacion() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            activarCapaUbicacion()
        } else {
            permisosUbicacionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    @SuppressLint("MissingPermission")
    private fun activarCapaUbicacion() {
        googleMap?.isMyLocationEnabled = true
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                miUbicacion = it
                val posicion = LatLng(it.latitude, it.longitude)
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(posicion, 15f))
            }
        }
    }

    private fun cargarPines() {
        binding.progressBarMapa.visibility = View.VISIBLE
        val api = RetrofitClient.getClient(sessionManager.getToken()).create(HaruApiService::class.java)
        api.getPines().enqueue(object : Callback<List<PinMapaDTO>> {
            override fun onResponse(call: Call<List<PinMapaDTO>>, response: Response<List<PinMapaDTO>>) {
                if (!isAdded) return
                binding.progressBarMapa.visibility = View.GONE
                if (response.isSuccessful) {
                    googleMap?.clear()
                    markerPinMap.clear()
                    markerAlertaMap.clear()
                    response.body()?.forEach { anadirPinAlMapa(it) }
                    cargarAlertasEnMapa()
                }
            }
            override fun onFailure(call: Call<List<PinMapaDTO>>, t: Throwable) {
                if (!isAdded) return
                binding.progressBarMapa.visibility = View.GONE
            }
        })
    }

    private fun cargarAlertasEnMapa() {
        val api = RetrofitClient.getClient(sessionManager.getToken()).create(HaruApiService::class.java)
        api.getAlertasActivas().enqueue(object : Callback<List<AlertaPerdidaDTO>> {
            override fun onResponse(call: Call<List<AlertaPerdidaDTO>>, response: Response<List<AlertaPerdidaDTO>>) {
                if (!isAdded) return
                response.body()?.forEach { alerta ->
                    val lat = alerta.ultimaUbicacionLat ?: return@forEach
                    val lng = alerta.ultimaUbicacionLng ?: return@forEach
                    val marker = googleMap?.addMarker(
                        MarkerOptions()
                            .position(LatLng(lat, lng))
                            .title(alerta.nombreMascota ?: "Mascota perdida")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    ) ?: return@forEach
                    markerAlertaMap[marker] = alerta
                }
            }
            override fun onFailure(call: Call<List<AlertaPerdidaDTO>>, t: Throwable) { }
        })
    }

    private fun mostrarDialogoAlerta(alerta: AlertaPerdidaDTO) {
        val mensaje = buildString {
            appendLine("Mascota: ${alerta.nombreMascota ?: "Desconocida"}")
            appendLine("Dueño: ${alerta.nombreDueno ?: "Desconocido"}")
            appendLine("Contacto: ${alerta.telefonoDueno ?: "No disponible"}")
            alerta.mensajeAdicional?.takeIf { it.isNotBlank() }?.let {
                append("\n$it")
            }
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Mascota perdida")
            .setMessage(mensaje.trim())
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun anadirPinAlMapa(pin: PinMapaDTO) {
        val posicion = LatLng(pin.latitud ?: return, pin.longitud ?: return)
        val visual = visualParaTipo(pin.tipo)

        val icono: BitmapDescriptor = bitmapDescriptorFromVector(
            iconoRes = visual.iconoRes,
            colorFondoRes = colorMarkerParaTipo(pin.tipo),
            tamanoPx = 96
        )

        val marker = googleMap?.addMarker(
            MarkerOptions()
                .position(posicion)
                .title(visual.etiqueta)
                .icon(icono)
                .anchor(0.5f, 1.0f)
        )
        marker?.let { markerPinMap[it] = pin }
    }

    private fun bitmapDescriptorFromVector(iconoRes: Int, colorFondoRes: Int, tamanoPx: Int): BitmapDescriptor {
        val context = requireContext()

        val bitmap = Bitmap.createBitmap(tamanoPx, tamanoPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val circulo = ContextCompat.getDrawable(context, R.drawable.bg_pin_fuente)!!.mutate()
        circulo.setTint(ContextCompat.getColor(context, colorFondoRes))
        circulo.setBounds(0, 0, tamanoPx, tamanoPx)
        circulo.draw(canvas)

        val padding = (tamanoPx * 0.22).toInt()
        val icono = ContextCompat.getDrawable(context, iconoRes)!!.mutate()
        icono.setTint(ContextCompat.getColor(context, R.color.haru_brown))
        icono.setBounds(padding, padding, tamanoPx - padding, tamanoPx - padding)
        icono.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun configurarFabs() {
        binding.fabAnadirPin.setOnClickListener { mostrarBottomSheetCrearPin() }
        binding.fabMiUbicacion.setOnClickListener {
            miUbicacion?.let {
                googleMap?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 16f)
                )
            }
        }
    }

    private fun mostrarBottomSheetCrearPin() {
        val dialog = BottomSheetDialog(requireContext())
        val bsBinding = BottomSheetCrearPinBinding.inflate(layoutInflater)
        dialog.setContentView(bsBinding.root)

        if (posicionElegida != null) {
            bsBinding.tvSubtituloCrearPin.text = "Pin en la posición que pulsaste"
        }

        bsBinding.btnCancelarPin.setOnClickListener {
            marcadorTemporal?.remove()
            marcadorTemporal = null
            posicionElegida = null
            dialog.dismiss()
        }

        bsBinding.btnCrearPin.setOnClickListener {
            val tipo = when (bsBinding.chipGroupTipoPin.checkedChipId) {
                bsBinding.chipFuente.id   -> "FUENTE"
                bsBinding.chipParque.id   -> "PARQUE"
                bsBinding.chipPapelera.id -> "PAPELERA"
                bsBinding.chipPeligro.id  -> "PELIGRO"
                else -> "FUENTE"
            }
            crearPin(tipo, bsBinding.etDescripcionPin.text.toString().trim())
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun mostrarBottomSheetDetalle(pin: PinMapaDTO) {
        val dialog = BottomSheetDialog(requireContext())
        val bsBinding = BottomSheetDetallePinBinding.inflate(layoutInflater)
        dialog.setContentView(bsBinding.root)

        val visual = visualParaTipo(pin.tipo)

        bsBinding.ivIconoPin.setImageResource(visual.iconoRes)
        bsBinding.ivIconoPin.setColorFilter(
            ContextCompat.getColor(requireContext(), colorMarkerParaTipo(pin.tipo))
        )

        bsBinding.flIconoPin.backgroundTintList =
            ContextCompat.getColorStateList(requireContext(), colorMarkerParaTipo(pin.tipo).let {
                when (pin.tipo?.uppercase()) {
                    "FUENTE"   -> R.color.pin_fuente_bg
                    "PARQUE"   -> R.color.pin_parque_bg
                    "PAPELERA" -> R.color.pin_papelera_bg
                    "PELIGRO"  -> R.color.pin_peligro_bg
                    else       -> R.color.pin_fuente_bg
                }
            })

        bsBinding.tvTipoPin.text = visual.etiqueta
        bsBinding.tvUsuarioPin.text = "Añadido por un vecino"
        bsBinding.tvDescripcionPin.text = pin.descripcion?.takeIf { it.isNotBlank() }
            ?: "Sin descripción adicional"

        if (pin.idUsuario == sessionManager.getIdUsuario()) {
            bsBinding.btnBorrarPin.visibility = View.VISIBLE
            bsBinding.btnBorrarPin.setOnClickListener {
                borrarPin(pin)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun crearPin(tipo: String, descripcion: String) {
        val lat = posicionElegida?.latitude ?: miUbicacion?.latitude ?: return
        val lng = posicionElegida?.longitude ?: miUbicacion?.longitude ?: return

        val dto = PinInsertarDTO(tipo, lat, lng, descripcion, sessionManager.getIdUsuario())
        val api = RetrofitClient.getClient(sessionManager.getToken()).create(HaruApiService::class.java)

        api.crearPin(dto).enqueue(object : Callback<PinMapaDTO> {
            override fun onResponse(call: Call<PinMapaDTO>, response: Response<PinMapaDTO>) {
                if (response.isSuccessful) {
                    marcadorTemporal?.remove()
                    marcadorTemporal = null
                    posicionElegida = null
                    response.body()?.let { anadirPinAlMapa(it) }
                    Toast.makeText(requireContext(), "¡Pin añadido!", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<PinMapaDTO>, t: Throwable) {
                Toast.makeText(requireContext(), "Error al crear el pin", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun borrarPin(pin: PinMapaDTO) {
        val api = RetrofitClient.getClient(sessionManager.getToken()).create(HaruApiService::class.java)
        pin.id?.let { id ->
            api.borrarPin(id).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        cargarPines()
                        Toast.makeText(requireContext(), "Pin eliminado", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Toast.makeText(requireContext(), "Error al eliminar", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}