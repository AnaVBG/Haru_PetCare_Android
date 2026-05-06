package com.dam2.haru_petcare.ui.mapa

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
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
            marcadorTemporal = map.addMarker(
                MarkerOptions().position(latLng)
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

    private fun anadirPinAlMapa(pin: PinMapaDTO) {
        val posicion = LatLng(pin.latitud ?: return, pin.longitud ?: return)
        val (hue, emoji, etiqueta) = when (pin.tipo?.uppercase()) {
            "FUENTE"   -> Triple(BitmapDescriptorFactory.HUE_AZURE,  "💧", "Fuente")
            "PAPELERA" -> Triple(BitmapDescriptorFactory.HUE_GREEN,  "🗑️", "Papelera")
            "PELIGRO"  -> Triple(BitmapDescriptorFactory.HUE_RED,    "⚠️", "Peligro")
            "PARQUE"   -> Triple(BitmapDescriptorFactory.HUE_ORANGE, "🌳", "Parque")
            else       -> Triple(BitmapDescriptorFactory.HUE_VIOLET, "📍", "Punto")
        }

        val marker = googleMap?.addMarker(MarkerOptions()
            .position(posicion)
            .title("$emoji $etiqueta")
            .icon(BitmapDescriptorFactory.defaultMarker(hue)))

        marker?.let { markerPinMap[it] = pin }
    }

    private fun configurarFabs() {
        binding.fabAnadirPin.setOnClickListener { mostrarBottomSheetCrearPin() }
        binding.fabMiUbicacion.setOnClickListener {
            miUbicacion?.let {
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 16f))
            }
        }
    }

    private fun mostrarBottomSheetCrearPin() {
        val dialog = BottomSheetDialog(requireContext())
        val bsBinding = BottomSheetCrearPinBinding.inflate(layoutInflater)
        dialog.setContentView(bsBinding.root)

        if (posicionElegida != null) {
            bsBinding.tvSubtituloCrearPin.text = "Pin en posición seleccionada"
        }

        bsBinding.btnCancelarPin.setOnClickListener {
            marcadorTemporal?.remove()
            marcadorTemporal = null
            posicionElegida = null
            dialog.dismiss()
        }

        bsBinding.btnCrearPin.setOnClickListener {
            val tipo = when (bsBinding.chipGroupTipoPin.checkedChipId) {
                bsBinding.chipFuente.id -> "FUENTE"
                bsBinding.chipPapelera.id -> "PAPELERA"
                bsBinding.chipPeligro.id -> "PELIGRO"
                bsBinding.chipParque.id -> "PARQUE"
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

        val (emoji, etiqueta) = when (pin.tipo?.uppercase()) {
            "FUENTE"   -> Pair("💧", "Fuente")
            "PAPELERA" -> Pair("🗑️", "Papelera")
            "PELIGRO"  -> Pair("⚠️", "Peligro")
            "PARQUE"   -> Pair("🌳", "Parque")
            else       -> Pair("📍", "Punto")
        }

        bsBinding.tvEmojiPin.text = emoji
        bsBinding.tvTipoPin.text = etiqueta
        bsBinding.tvDescripcionPin.text = pin.descripcion ?: "Sin descripción"
        bsBinding.tvUsuarioPin.text = "Añadido por un vecino"

        // Mostrar botón borrar solo si el pin es nuestro
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
                    Toast.makeText(requireContext(), "Pin añadido", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<PinMapaDTO>, t: Throwable) {}
        })
    }

    private fun borrarPin(pin: PinMapaDTO) {
        val api = RetrofitClient.getClient(sessionManager.getToken()).create(HaruApiService::class.java)
        pin.id?.let { id ->
            api.borrarPin(id).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        cargarPines()
                        Toast.makeText(requireContext(), "Pin borrado", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<Void>, t: Throwable) {}
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}