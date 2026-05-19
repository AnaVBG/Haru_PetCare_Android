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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.dam2.haru_petcare.R
import com.dam2.haru_petcare.databinding.BottomSheetCrearPinBinding
import com.dam2.haru_petcare.databinding.BottomSheetDetallePinBinding
import com.dam2.haru_petcare.databinding.FragmentMapaBinding
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

    private val permisosUbicacionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        val concedido = permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true
                || permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (concedido) activarCapaUbicacion()
        else Toast.makeText(requireContext(), "Permiso denegado", Toast.LENGTH_SHORT).show()
    }

    // ── Datos visuales por tipo de pin ────────────────────────────────────
    // Cada tipo tiene: iconoRes (drawable SVG), colorFondo (color del círculo), etiqueta
    private data class PinVisual(val iconoRes: Int, val colorFondo: Int, val etiqueta: String)

    private fun visualParaTipo(tipo: String?): PinVisual = when (tipo?.uppercase()) {
        "FUENTE"   -> PinVisual(R.drawable.ic_pin_fuente,   R.color.pin_fuente_bg,   "Fuente de agua")
        "PARQUE"   -> PinVisual(R.drawable.ic_pin_parque,   R.color.pin_parque_bg,   "Parque")
        "PAPELERA" -> PinVisual(R.drawable.ic_pin_papelera, R.color.pin_papelera_bg, "Papelera")
        "PELIGRO"  -> PinVisual(R.drawable.ic_pin_peligro,  R.color.pin_peligro_bg,  "Zona de peligro")
        else       -> PinVisual(R.drawable.ic_pin_fuente,   R.color.pin_fuente_bg,   "Punto de interés")
    }

    // ── Colores del marcador en el mapa (círculo de fondo del pin) ────────
    private fun colorMarkerParaTipo(tipo: String?): Int = when (tipo?.uppercase()) {
        "FUENTE"   -> R.color.pin_fuente
        "PARQUE"   -> R.color.pin_parque
        "PAPELERA" -> R.color.pin_papelera
        "PELIGRO"  -> R.color.pin_peligro
        else       -> R.color.haru_teal
    }

    // ─────────────────────────────────────────────────────────────────────

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
            if (pin != null) mostrarBottomSheetDetalle(pin)
            true
        }

        map.setOnMapLongClickListener { latLng ->
            posicionElegida = latLng
            marcadorTemporal?.remove()
            // Marcador temporal con ícono neutro mientras el usuario elige el tipo
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
                    response.body()?.forEach { anadirPinAlMapa(it) }
                }
            }
            override fun onFailure(call: Call<List<PinMapaDTO>>, t: Throwable) {
                if (!isAdded) return
                binding.progressBarMapa.visibility = View.GONE
            }
        })
    }

    /**
     * Coloca un marcador en el mapa usando el ícono SVG vectorial
     * correcto para cada tipo de pin, en lugar de los marcadores
     * de color genérico de defaultMarker(hue).
     *
     * bitmapDescriptorFromVector convierte el VectorDrawable en un
     * Bitmap que Google Maps puede renderizar como ícono de marcador.
     */
    private fun anadirPinAlMapa(pin: PinMapaDTO) {
        val posicion = LatLng(pin.latitud ?: return, pin.longitud ?: return)
        val visual = visualParaTipo(pin.tipo)

        val icono: BitmapDescriptor = bitmapDescriptorFromVector(
            iconoRes = visual.iconoRes,
            colorFondoRes = colorMarkerParaTipo(pin.tipo),
            tamanoPx = 96  // 96px = tamaño cómodo en el mapa a densidades normales
        )

        val marker = googleMap?.addMarker(
            MarkerOptions()
                .position(posicion)
                .title(visual.etiqueta)
                .icon(icono)
                .anchor(0.5f, 1.0f) // El punto del pin apunta exactamente a la coordenada
        )
        marker?.let { markerPinMap[it] = pin }
    }

    /**
     * Convierte un VectorDrawable (XML en res/drawable) en un BitmapDescriptor
     * que Google Maps acepta como ícono de marcador.
     *
     * El marcador tiene forma de círculo de color con el ícono blanco dentro,
     * igual que el diseño del sistema Haru.
     *
     * @param iconoRes      ID del drawable SVG (ej. R.drawable.ic_pin_fuente)
     * @param colorFondoRes ID del color de fondo del círculo (ej. R.color.pin_fuente)
     * @param tamanoPx      Tamaño del marcador en píxeles físicos del dispositivo
     */
    private fun bitmapDescriptorFromVector(iconoRes: Int, colorFondoRes: Int, tamanoPx: Int): BitmapDescriptor {
        val context = requireContext()

        // 1. Creamos el bitmap del tamaño deseado
        val bitmap = Bitmap.createBitmap(tamanoPx, tamanoPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 2. Dibujamos el círculo de fondo con el color del tipo
        val circulo = ContextCompat.getDrawable(context, R.drawable.bg_pin_fuente)!!.mutate()
        circulo.setTint(ContextCompat.getColor(context, colorFondoRes))
        circulo.setBounds(0, 0, tamanoPx, tamanoPx)
        circulo.draw(canvas)

        // 3. Dibujamos el ícono SVG centrado (con padding del 25%)
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

        // Setear el ícono SVG en el ImageView
        bsBinding.ivIconoPin.setImageResource(visual.iconoRes)
        bsBinding.ivIconoPin.setColorFilter(
            ContextCompat.getColor(requireContext(), colorMarkerParaTipo(pin.tipo))
        )

        // Setear el color del fondo circular del ícono
        bsBinding.flIconoPin.backgroundTintList =
            ContextCompat.getColorStateList(requireContext(), colorMarkerParaTipo(pin.tipo).let {
                // Usamos la versión clara (bg) para el fondo del círculo en el bottom sheet
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

        // Botón borrar solo si es nuestro pin
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