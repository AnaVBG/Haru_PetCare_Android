package com.dam2.haru_petcare.ui.citas

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dam2.haru_petcare.databinding.ItemCitaBinding
import com.dam2.haru_petcare.model.CitaDTO
import com.dam2.haru_petcare.util.Constants

class CitaAdapter(
    private val esVeterinario: Boolean,
    private val onCambiarEstado: (CitaDTO, String) -> Unit
) : RecyclerView.Adapter<CitaAdapter.CitaViewHolder>() {

    // Lista completa recibida de la API (no se modifica)
    private var todasLasCitas = listOf<CitaDTO>()

    // Lista filtrada que realmente muestra el RecyclerView
    private var citasFiltradas = listOf<CitaDTO>()

    fun setCitas(nuevaLista: List<CitaDTO>) {
        todasLasCitas  = nuevaLista
        citasFiltradas = nuevaLista
        notifyDataSetChanged()
    }

    /**
     * Filtra la lista por estado sin volver a llamar a la API.
     * @param filtro null = mostrar todas
     */
    fun filtrarPorEstado(filtro: String?) {
        citasFiltradas = if (filtro == null) {
            todasLasCitas
        } else {
            todasLasCitas.filter { it.estado == filtro }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CitaViewHolder {
        val binding = ItemCitaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CitaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CitaViewHolder, position: Int) {
        holder.bind(citasFiltradas[position])
    }

    override fun getItemCount() = citasFiltradas.size

    inner class CitaViewHolder(
        private val binding: ItemCitaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cita: CitaDTO) {
            binding.tvNombreMascotaCita.text = cita.nombreMascota    ?: "Mascota"
            binding.tvMotivoCita.text        = cita.motivo           ?: "Sin motivo"
            binding.tvVeterinarioCita.text   = cita.nombreVeterinario ?: "—"
            binding.tvFechaCita.text         = formatearFechaHora(cita.fechaCita)

            aplicarEstiloEstado(cita.estado)
            configurarBotonesVeterinario(cita)
        }

        /**
         * Cambia el color de la franja lateral y el chip según el estado.
         * Esto da feedback visual inmediato sin leer el texto del chip.
         */
        private fun aplicarEstiloEstado(estado: String?) {
            val (colorFranja, textoChip, colorChipFondo, colorChipTexto) = when (estado) {
                "PENDIENTE"  -> EstadoEstilo(
                    franja     = "#4BBFBF", // haru_teal
                    chip       = "Pendiente",
                    chipFondo  = "#E0F5F5",
                    chipTexto  = "#2E9494"
                )
                "COMPLETADA" -> EstadoEstilo(
                    franja    = "#4CAF50", // verde
                    chip      = "Completada",
                    chipFondo = "#E8F5E9",
                    chipTexto = "#2E7D32"
                )
                "CANCELADA"  -> EstadoEstilo(
                    franja    = "#5C2018", // haru_brown
                    chip      = "Cancelada",
                    chipFondo = "#FBE9E7",
                    chipTexto = "#5C2018"
                )
                else -> EstadoEstilo(
                    franja    = "#B0BFBF",
                    chip      = "Desconocido",
                    chipFondo = "#F5F5F5",
                    chipTexto = "#757575"
                )
            }

            binding.viewEstadoColor.setBackgroundColor(Color.parseColor(colorFranja))
            binding.chipEstadoCita.text = textoChip
            binding.chipEstadoCita.chipBackgroundColor =
                android.content.res.ColorStateList.valueOf(Color.parseColor(colorChipFondo))
            binding.chipEstadoCita.setTextColor(Color.parseColor(colorChipTexto))
        }

        /**
         * Muestra los botones de acción solo si:
         * 1. El usuario es veterinario
         * 2. La cita está en estado PENDIENTE (no tiene sentido actuar sobre ya cerradas)
         */
        private fun configurarBotonesVeterinario(cita: CitaDTO) {
            val mostrarBotones = esVeterinario && cita.estado == "PENDIENTE"
            binding.layoutBotonesVeterinario.visibility =
                if (mostrarBotones) View.VISIBLE else View.GONE

            if (mostrarBotones) {
                binding.btnCompletar.setOnClickListener {
                    // Propagamos la acción al Fragment para que haga la llamada Retrofit
                    onCambiarEstado(cita, "COMPLETADA")
                }
                binding.btnCancelar.setOnClickListener {
                    onCambiarEstado(cita, "CANCELADA")
                }
            }
        }

        /**
         * Convierte "2024-06-15T11:00:00" en "15/06/2024 a las 11:00"
         */
        private fun formatearFechaHora(fechaIso: String?): String {
            if (fechaIso == null) return "Fecha desconocida"
            return try {
                val partes    = fechaIso.split("T")
                val fechaPart = partes[0].split("-")
                val horaPart  = partes[1].substring(0, 5) // "11:00"
                "${fechaPart[2]}/${fechaPart[1]}/${fechaPart[0]} · $horaPart"
            } catch (e: Exception) {
                fechaIso
            }
        }
    }

    /**
     * Data class auxiliar para agrupar los valores de estilo de cada estado.
     * Usamos desestructuración (component1..4) en el 'when' de arriba.
     */
    private data class EstadoEstilo(
        val franja: String,
        val chip: String,
        val chipFondo: String,
        val chipTexto: String
    )
}