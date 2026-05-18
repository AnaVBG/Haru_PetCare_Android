package com.dam2.haru_petcare.ui.alertas

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dam2.haru_petcare.databinding.ItemAlertaBinding
import com.dam2.haru_petcare.model.AlertaPerdidaDTO

class AlertaAdapter(
    private val idUsuarioLogueado: Long,
    private val onResolverAlerta: (AlertaPerdidaDTO) -> Unit
) : RecyclerView.Adapter<AlertaAdapter.AlertaViewHolder>() {

    private val alertas = mutableListOf<AlertaPerdidaDTO>()

    fun setAlertas(nuevaLista: List<AlertaPerdidaDTO>) {
        alertas.clear()
        alertas.addAll(nuevaLista)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertaViewHolder {
        val binding = ItemAlertaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AlertaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlertaViewHolder, position: Int) {
        holder.bind(alertas[position])
    }

    override fun getItemCount() = alertas.size

    inner class AlertaViewHolder(
        private val binding: ItemAlertaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(alerta: AlertaPerdidaDTO) {
            binding.tvNombreMascotaAlerta.text = alerta.nombreMascota ?: "Mascota"
            binding.tvDuenoAlerta.text         = "Dueño: ${alerta.nombreDueno ?: "Desconocido"}"
            binding.tvMensajeAlerta.text       = alerta.mensajeAdicional
                ?.takeIf { it.isNotBlank() }
                ?: "Sin descripción adicional"
            binding.tvFechaAlerta.text         = formatearFecha(alerta.fechaAlerta)

            configurarBotonLlamar(alerta)
            configurarBotonResolver(alerta)
        }

        /**
         * Botón llamar: abre el marcador de teléfono con el número del dueño.
         * Solo visible si el dueño tiene teléfono registrado.
         *
         * Usamos ACTION_DIAL en vez de ACTION_CALL porque:
         * - ACTION_DIAL abre el marcador y deja que el usuario confirme
         * - ACTION_CALL haría la llamada directamente pero requiere
         *   el permiso CALL_PHONE, que Google Play exige justificar
         */
        private fun configurarBotonLlamar(alerta: AlertaPerdidaDTO) {
            val telefono = alerta.telefonoDueno
            if (!telefono.isNullOrBlank()) {
                binding.btnLlamarDueno.visibility = View.VISIBLE
                binding.btnLlamarDueno.setOnClickListener {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:$telefono")
                    }
                    binding.root.context.startActivity(intent)
                }
            } else {
                binding.btnLlamarDueno.visibility = View.GONE
            }
        }

        /**
         * Botón "Mascota encontrada": solo visible si la alerta
         * pertenece al usuario logueado.
         *
         * Comparamos alerta.idUsuario con idUsuarioLogueado que viene
         * de SessionManager en AlertasFragment. Si no coinciden, el botón
         * se oculta — un vecino no puede resolver la alerta de otro.
         */
        private fun configurarBotonResolver(alerta: AlertaPerdidaDTO) {
            val esSuAlerta = alerta.idUsuario == idUsuarioLogueado

            binding.btnMascotaEncontrada.visibility =
                if (esSuAlerta) View.VISIBLE else View.GONE

            if (esSuAlerta) {
                binding.btnMascotaEncontrada.setOnClickListener {
                    onResolverAlerta(alerta)
                }
            }
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