package com.dam2.haru_petcare.ui.mascotas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dam2.haru_petcare.databinding.ItemHistorialBinding
import com.dam2.haru_petcare.model.HistorialMedicoDTO

class HistorialAdapter : RecyclerView.Adapter<HistorialAdapter.HistorialViewHolder>() {

    private val registros = mutableListOf<HistorialMedicoDTO>()

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
            // Chip con el tipo de registro (vacuna, consulta, cirugía...)
            binding.chipTipoRegistro.text = registro.tipoRegistro ?: "Registro"

            // Color del chip según el tipo de registro
            val chipColor = when (registro.tipoRegistro?.uppercase()) {
                "VACUNA"   -> android.graphics.Color.parseColor("#E0F5F5") // teal claro
                "CIRUGIA"  -> android.graphics.Color.parseColor("#FDECEA") // rojo claro
                "CONSULTA" -> android.graphics.Color.parseColor("#FFF8E1") // amarillo claro
                else       -> android.graphics.Color.parseColor("#F5F5F5") // gris
            }
            binding.chipTipoRegistro.chipBackgroundColor =
                android.content.res.ColorStateList.valueOf(chipColor)

            binding.tvDescripcionHistorial.text = registro.descripcion ?: "Sin descripción"

            // Formateamos la fecha: "2024-03-15T10:30:00" → "15/03/2024"
            binding.tvFechaHistorial.text = formatearFecha(registro.fechaRegistro)
        }

        /**
         * Convierte "2024-03-15T10:30:00" en "15/03/2024".
         * Usamos split y substring para evitar dependencias de parsing de fechas.
         */
        private fun formatearFecha(fechaIso: String?): String {
            if (fechaIso == null) return "Fecha desconocida"
            return try {
                // "2024-03-15T10:30:00" → tomamos solo la parte de la fecha
                val partes = fechaIso.split("T")[0].split("-")
                // Reordenamos de YYYY-MM-DD a DD/MM/YYYY
                "${partes[2]}/${partes[1]}/${partes[0]}"
            } catch (e: Exception) {
                fechaIso // Si falla, mostramos la fecha tal como viene
            }
        }
    }
}