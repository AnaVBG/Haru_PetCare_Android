package com.dam2.haru_petcare.ui.mascotas

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dam2.haru_petcare.R
import com.dam2.haru_petcare.databinding.FragmentMascotasBinding
import com.dam2.haru_petcare.model.CitaDTO
import com.dam2.haru_petcare.model.MascotaDTO
import com.dam2.haru_petcare.network.HaruApiService
import com.dam2.haru_petcare.network.RetrofitClient
import com.dam2.haru_petcare.ui.main.MainActivity
import com.dam2.haru_petcare.util.Constants
import com.dam2.haru_petcare.util.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MascotaFragment : Fragment() {

    private var _binding: FragmentMascotasBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: MascotaAdapter
    private lateinit var api: HaruApiService

    private val addMascotaLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            cargarMascotas()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMascotasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        api = RetrofitClient
            .getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)

        configurarToolbar()
        configurarSaludo()
        configurarAdapter()
        configurarAccesosRapidos()
        cargarMascotas()
        cargarProximasCitas()

        binding.fabAnadirMascota.setOnClickListener {
            addMascotaLauncher.launch(
                Intent(requireContext(), AddMascotaActivity::class.java)
            )
        }
    }

    // ── TOOLBAR ──────────────────────────────────────────────────────────────

    private fun configurarToolbar() {
        binding.toolbarMascotas.title = "Inicio"
    }

    // ── SALUDO DINÁMICO ───────────────────────────────────────────────────────

    private fun configurarSaludo() {
        val nombre = sessionManager.getNombre() ?: "Usuario"
        val hora = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)

        val saludo = when {
            hora < 14 -> "Buenos días"
            hora < 21 -> "Buenas tardes"
            else      -> "Buenas noches"
        }

        binding.tvSaludo.text = saludo
        binding.tvNombreUsuario.text = nombre
    }

    // ── ADAPTER MASCOTAS ──────────────────────────────────────────────────────

    private fun configurarAdapter() {
        adapter = MascotaAdapter { mascota ->
            val intent = Intent(requireContext(), DetalleMascotaActivity::class.java).apply {
                putExtra(Constants.EXTRA_MASCOTA_ID, mascota.id)
                putExtra("mascota_nombre", mascota.nombre)
            }
            startActivity(intent)
        }

        binding.rvMascotas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MascotaFragment.adapter
        }
    }

    // ── CARGA DE MASCOTAS ─────────────────────────────────────────────────────

    private fun cargarMascotas() {
        val idDueno = sessionManager.getIdUsuario()

        binding.progressBarMascotas.visibility = View.VISIBLE
        binding.layoutSinMascotas.visibility = View.GONE
        binding.rvMascotas.visibility = View.GONE

        api.getMascotasPorDueno(idDueno).enqueue(object : Callback<List<MascotaDTO>> {
            override fun onResponse(
                call: Call<List<MascotaDTO>>,
                response: Response<List<MascotaDTO>>
            ) {
                binding.progressBarMascotas.visibility = View.GONE

                if (response.isSuccessful) {
                    val mascotas = response.body() ?: emptyList()
                    adapter.setMascotas(mascotas)

                    if (mascotas.isEmpty()) {
                        binding.layoutSinMascotas.visibility = View.VISIBLE
                        binding.rvMascotas.visibility = View.GONE
                    } else {
                        binding.layoutSinMascotas.visibility = View.GONE
                        binding.rvMascotas.visibility = View.VISIBLE
                    }
                } else {
                    mostrarError("Error al cargar mascotas (${response.code()})")
                }
            }

            override fun onFailure(call: Call<List<MascotaDTO>>, t: Throwable) {
                binding.progressBarMascotas.visibility = View.GONE
                mostrarError("Sin conexión: ${t.message}")
            }
        })
    }

    // ── PRÓXIMAS CITAS (solo lectura) ─────────────────────────────────────────

    private fun cargarProximasCitas() {
        val idDueno = sessionManager.getIdUsuario()

        binding.progressBarCitasHome.visibility = View.VISIBLE
        binding.layoutCita1.visibility = View.GONE
        binding.layoutCita2.visibility = View.GONE
        binding.dividerCitas.visibility = View.GONE
        binding.tvSinCitas.visibility = View.GONE
        binding.tvVerTodasCitas.visibility = View.GONE

        api.getCitasDueno(idDueno).enqueue(object : Callback<List<CitaDTO>> {
            override fun onResponse(
                call: Call<List<CitaDTO>>,
                response: Response<List<CitaDTO>>
            ) {
                binding.progressBarCitasHome.visibility = View.GONE

                if (!response.isSuccessful) {
                    binding.tvSinCitas.visibility = View.VISIBLE
                    return
                }

                // Filtramos solo las citas futuras o de hoy, ordenadas por fecha
                val hoy = LocalDate.now()
                val proximasCitas = (response.body() ?: emptyList())
                    .filter { cita ->
                        val fechaCita = LocalDate.parse(
                            cita.fechaCita?.substring(0, 10)
                        )
                        !fechaCita.isBefore(hoy)
                    }
                    .sortedBy { it.fechaCita }
                    .take(2)

                if (proximasCitas.isEmpty()) {
                    binding.tvSinCitas.visibility = View.VISIBLE
                    return
                }

                // Mostramos la primera cita
                mostrarCita(
                    cita       = proximasCitas[0],
                    layoutCita = binding.layoutCita1,
                    tvDia      = binding.tvCita1Dia,
                    tvMes      = binding.tvCita1Mes,
                    tvDesc     = binding.tvCita1Desc,
                    tvSub      = binding.tvCita1Sub,
                    tvEstado   = binding.tvCita1Estado
                )
                binding.layoutCita1.visibility = View.VISIBLE
                binding.tvVerTodasCitas.visibility = View.VISIBLE

                // Si hay segunda cita la mostramos también
                if (proximasCitas.size >= 2) {
                    binding.dividerCitas.visibility = View.VISIBLE
                    mostrarCita(
                        cita       = proximasCitas[1],
                        layoutCita = binding.layoutCita2,
                        tvDia      = binding.tvCita2Dia,
                        tvMes      = binding.tvCita2Mes,
                        tvDesc     = binding.tvCita2Desc,
                        tvSub      = binding.tvCita2Sub,
                        tvEstado   = binding.tvCita2Estado
                    )
                    binding.layoutCita2.visibility = View.VISIBLE
                }
            }

            override fun onFailure(call: Call<List<CitaDTO>>, t: Throwable) {
                binding.progressBarCitasHome.visibility = View.GONE
                binding.tvSinCitas.visibility = View.VISIBLE
            }
        })
    }

    private fun mostrarCita(
        cita: CitaDTO,
        layoutCita: View,
        tvDia: android.widget.TextView,
        tvMes: android.widget.TextView,
        tvDesc: android.widget.TextView,
        tvSub: android.widget.TextView,
        tvEstado: android.widget.TextView
    ) {
        // El backend devuelve fechaCita como "2025-05-28T10:00:00"
        val fecha = LocalDate.parse(cita.fechaCita.substring(0, 10))
        val hora  = cita.fechaCita.substring(11, 16)

        val formateadorMes = DateTimeFormatter.ofPattern("MMM", Locale("es", "ES"))

        tvDia.text    = fecha.dayOfMonth.toString()
        tvMes.text    = fecha.format(formateadorMes).uppercase()
        tvDesc.text   = cita.motivo ?: "Cita veterinaria"
        tvSub.text    = "$hora · ${cita.nombreVeterinario ?: "Veterinario"}"
        tvEstado.text = cita.estado ?: "Pendiente"

        // Color del badge según el estado
        val colorFondo = when (cita.estado?.uppercase()) {
            "COMPLETADA" -> R.drawable.bg_badge_completada
            "CANCELADA"  -> R.drawable.bg_badge_cancelada
            else         -> R.drawable.bg_badge_pendiente
        }
        tvEstado.setBackgroundResource(colorFondo)
    }

    // ── ACCESOS RÁPIDOS ───────────────────────────────────────────────────────

    private fun configurarAccesosRapidos() {
        val mainActivity = requireActivity() as MainActivity

        // Salud → navega al nav_salud del BottomNav
        binding.cardAccesoSalud.setOnClickListener {
            mainActivity.binding.bottomNavView.selectedItemId = R.id.nav_salud
        }

        // Mapa → navega al nav_mapa
        binding.cardAccesoMapa.setOnClickListener {
            mainActivity.binding.bottomNavView.selectedItemId = R.id.nav_mapa
        }

        // Alertas → navega al nav_alertas
        binding.cardAccesoAlertas.setOnClickListener {
            mainActivity.binding.bottomNavView.selectedItemId = R.id.nav_alertas
        }

        // Perfil → navega al nav_perfil
        binding.cardAccesoPerfil.setOnClickListener {
            mainActivity.binding.bottomNavView.selectedItemId = R.id.nav_perfil
        }

        // "Ver todas en Salud →" también navega a Salud
        binding.tvVerTodasCitas.setOnClickListener {
            mainActivity.binding.bottomNavView.selectedItemId = R.id.nav_salud
        }
    }

    // ── UTILIDADES ────────────────────────────────────────────────────────────

    private fun mostrarError(mensaje: String) {
        if (isAdded) Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}