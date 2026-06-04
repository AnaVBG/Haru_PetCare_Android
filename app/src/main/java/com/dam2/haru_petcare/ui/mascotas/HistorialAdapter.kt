package com.dam2.haru_petcare.ui.mascotas

import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dam2.haru_petcare.databinding.ItemHistorialBinding
import com.dam2.haru_petcare.model.HistorialMedicoDTO
import java.text.Normalizer

class HistorialAdapter : RecyclerView.Adapter<HistorialAdapter.HistorialViewHolder>() {

    private val registros = mutableListOf<HistorialMedicoDTO>()
    private data class ChipEstilo(val fondo: String, val texto: String)

    fun setRegistros(nuevaLista: List<HistorialMedicoDTO>) {
        registros.clear()
        registros.addAll(nuevaLista)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistorialViewHolder {
        val binding = ItemHistorialBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HistorialViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistorialViewHolder, position: Int) {
        holder.bind(registros[position])
    }

    override fun getItemCount() = registros.size

    inner class HistorialViewHolder(
        private val binding: ItemHistorialBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(registro: HistorialMedicoDTO) {
            binding.chipTipoRegistro.text = registro.tipoRegistro ?: "Registro"

            val isDark = (itemView.context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

            // Eliminamos acentos antes de comparar para que "Cirugía" == "CIRUGIA", etc.
            val tipo = registro.tipoRegistro
                ?.let { Normalizer.normalize(it, Normalizer.Form.NFD) }
                ?.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
                ?.uppercase()

            val estilo = when (tipo) {
                "VACUNA" -> if (isDark)
                    ChipEstilo("#1A4444", "#5ECFCF") else ChipEstilo("#9DE2E2", "#1A6A6A")
                "CIRUGIA" -> if (isDark)
                    ChipEstilo("#3D1A18", "#FF9B96") else ChipEstilo("#FFBDBA", "#7A1A10")
                "CONSULTA" -> if (isDark)
                    ChipEstilo("#3D3000", "#FFD060") else ChipEstilo("#FFE08A", "#6B4A00")
                "DESPARASITACION" -> if (isDark)
                    ChipEstilo("#1A3320", "#81C784") else ChipEstilo("#C8E6C9", "#2E6B30")
                "ANALISIS" -> if (isDark)
                    ChipEstilo("#2A1F40", "#CE93D8") else ChipEstilo("#D1C4E9", "#4A2E80")
                else -> if (isDark)
                    ChipEstilo("#2A2A2A", "#A0AAAA") else ChipEstilo("#D0D0D0", "#3A3A3A")
            }

            binding.chipTipoRegistro.chipBackgroundColor =
                android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(estilo.fondo))
            binding.chipTipoRegistro.setTextColor(android.graphics.Color.parseColor(estilo.texto))

            binding.tvDescripcionHistorial.text = registro.descripcion ?: "Sin descripción"
            binding.tvFechaHistorial.text = formatearFecha(registro.fechaRegistro)
        }

        private fun formatearFecha(fechaIso: String?): String {
            if (fechaIso == null) return "Fecha desconocida"
            return try {
                val partes = fechaIso.split("T")[0].split("-")
                "${partes[2]}/${partes[1]}/${partes[0]}"
            } catch (e: Exception) {
                fechaIso
            }
        }
    }
}