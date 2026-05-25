package com.dam2.haru_petcare.ui.salud

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.dam2.haru_petcare.R
import com.dam2.haru_petcare.databinding.FragmentSaludBinding
import com.dam2.haru_petcare.model.MascotaDTO
import com.dam2.haru_petcare.network.HaruApiService
import com.dam2.haru_petcare.network.RetrofitClient
import com.dam2.haru_petcare.util.SessionManager
import com.google.android.material.tabs.TabLayoutMediator
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SaludFragment : Fragment() {

    private var _binding: FragmentSaludBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var api: HaruApiService

    private var mascotaSeleccionada: MascotaDTO? = null
    private var listaMascotas: List<MascotaDTO> = emptyList()

    companion object {
        private const val ARG_TAB = "tab_inicial"

        fun newInstance(tabIndex: Int): SaludFragment {
            return SaludFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TAB, tabIndex)
                }
            }
        }

        const val TAB_HISTORIAL         = 0
        const val TAB_CITAS             = 1
        const val TAB_VACUNAS           = 2
        const val TAB_DESPARASITACIONES = 3
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSaludBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        api = RetrofitClient
            .getClient(sessionManager.getToken())
            .create(HaruApiService::class.java)

        binding.toolbarSalud.title = "Salud"
        cargarMascotas()
    }

    // ── CARGA DE MASCOTAS DEL DUEÑO ───────────────────────────────────────────

    private fun cargarMascotas() {
        val idDueno = sessionManager.getIdUsuario()

        binding.progressBarSaludMascotas.visibility = View.VISIBLE
        binding.layoutSinMascotasSalud.visibility = View.GONE
        binding.viewPagerSalud.visibility = View.GONE

        api.getMascotasPorDueno(idDueno).enqueue(object : Callback<List<MascotaDTO>> {
            override fun onResponse(
                call: Call<List<MascotaDTO>>,
                response: Response<List<MascotaDTO>>
            ) {
                binding.progressBarSaludMascotas.visibility = View.GONE

                if (!response.isSuccessful) {
                    mostrarError("Error al cargar mascotas (${response.code()})")
                    return
                }

                listaMascotas = response.body() ?: emptyList()

                when {
                    // Sin mascotas → estado vacío
                    listaMascotas.isEmpty() -> {
                        binding.layoutSinMascotasSalud.visibility = View.VISIBLE
                    }

                    // Una sola mascota → la seleccionamos automáticamente
                    listaMascotas.size == 1 -> {
                        binding.scrollSelectorMascota.visibility = View.GONE
                        seleccionarMascota(listaMascotas[0])
                    }

                    // Varias mascotas → mostramos el selector horizontal
                    else -> {
                        binding.scrollSelectorMascota.visibility = View.VISIBLE
                        construirChipsMascotas(listaMascotas)
                        // Seleccionamos la primera por defecto
                        seleccionarMascota(listaMascotas[0])
                    }
                }
            }

            override fun onFailure(call: Call<List<MascotaDTO>>, t: Throwable) {
                binding.progressBarSaludMascotas.visibility = View.GONE
                mostrarError("Sin conexión: ${t.message}")
            }
        })
    }

    // ── SELECTOR HORIZONTAL DE MASCOTAS ──────────────────────────────────────

    private fun construirChipsMascotas(mascotas: List<MascotaDTO>) {
        binding.layoutChipsMascotas.removeAllViews()

        mascotas.forEachIndexed { index, mascota ->
            val chip = TextView(requireContext()).apply {
                text = mascota.nombre
                textSize = 13f
                setPadding(24, 10, 24, 10)
                isClickable = true
                isFocusable = true

                // Estado inicial: primera seleccionada
                if (index == 0) {
                    activarChip(this)
                } else {
                    desactivarChip(this)
                }

                setOnClickListener {
                    // Desactivamos todos los chips
                    for (i in 0 until binding.layoutChipsMascotas.childCount) {
                        val chip = binding.layoutChipsMascotas.getChildAt(i) as TextView
                        desactivarChip(chip)
                    }
                    // Activamos el pulsado
                    activarChip(this)
                    // Cargamos los tabs de esta mascota
                    seleccionarMascota(mascota)
                }
            }

            val params = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 8
            }
            binding.layoutChipsMascotas.addView(chip, params)
        }
    }

    private fun activarChip(chip: TextView) {
        chip.setBackgroundResource(R.drawable.bg_chip_mascota_activo)
        chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.haru_teal))
    }

    private fun desactivarChip(chip: TextView) {
        chip.setBackgroundResource(R.drawable.bg_chip_mascota_inactivo)
        chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.haru_white).also {
            chip.setTextColor(0xCCFFFFFF.toInt())
        })
    }

    // ── SELECCIÓN DE MASCOTA Y CARGA DE TABS ─────────────────────────────────

    private fun seleccionarMascota(mascota: MascotaDTO) {
        mascotaSeleccionada = mascota

        binding.toolbarSalud.subtitle = mascota.nombre ?: ""
        configurarViewPager(mascota.id ?: return)

        binding.viewPagerSalud.visibility = View.VISIBLE
    }

    private fun configurarViewPager(idMascota: Long) {
        val tabInicial = arguments?.getInt(ARG_TAB, 0) ?: 0

        val adapter = SaludPagerAdapter(this, idMascota)
        binding.viewPagerSalud.adapter = adapter

        TabLayoutMediator(binding.tabLayoutSalud, binding.viewPagerSalud) { tab, position ->
            tab.text = when (position) {
                TAB_HISTORIAL         -> "Historial"
                TAB_CITAS             -> "Citas"
                TAB_VACUNAS           -> "Vacunas"
                TAB_DESPARASITACIONES -> "Despar."
                else                  -> ""
            }
        }.attach()

        if (tabInicial != 0) {
            binding.viewPagerSalud.setCurrentItem(tabInicial, false)
        }
    }

    private fun mostrarError(mensaje: String) {
        if (isAdded) Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ── ADAPTER DEL VIEWPAGER2 ────────────────────────────────────────────────────

class SaludPagerAdapter(
    fragment: Fragment,
    private val idMascota: Long
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            SaludFragment.TAB_HISTORIAL         -> HistorialTabFragment.newInstance(idMascota)
            SaludFragment.TAB_CITAS -> CitasTabFragment.newInstance()
            SaludFragment.TAB_VACUNAS           -> VacunasTabFragment.newInstance(idMascota)
            SaludFragment.TAB_DESPARASITACIONES -> DesparasitacionesTabFragment.newInstance(idMascota)
            else                                -> HistorialTabFragment.newInstance(idMascota)
        }
    }
}