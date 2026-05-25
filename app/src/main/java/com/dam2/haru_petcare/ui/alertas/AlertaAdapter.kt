package com.dam2.haru_petcare.ui.alertas

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.dam2.haru_petcare.R
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

            cargarFotoMascota(alerta)
            configurarBotonLlamar(alerta)
            configurarBotonResolver(alerta)
        }

        private fun cargarFotoMascota(alerta: AlertaPerdidaDTO) {
            val inicial = alerta.nombreMascota?.firstOrNull()?.uppercase() ?: "?"
            binding.tvInicialMascotaAlerta.text = inicial

            if (!alerta.fotoUrlMascota.isNullOrBlank()) {
                // Hay foto — la cargamos con Glide en círculo
                binding.ivFotoMascotaAlerta.visibility = View.VISIBLE
                binding.viewFondoAvatarAlerta.visibility = View.GONE
                binding.tvInicialMascotaAlerta.visibility = View.GONE

                Glide.with(binding.root.context)
                    .load(alerta.fotoUrlMascota)
                    .transform(CircleCrop())
                    .placeholder(R.drawable.bg_avatar_alerta)
                    .error(R.drawable.bg_avatar_alerta)
                    .into(binding.ivFotoMascotaAlerta)
            } else {
                // Sin foto — fondo marrón con inicial
                binding.ivFotoMascotaAlerta.visibility = View.GONE
                binding.viewFondoAvatarAlerta.visibility = View.VISIBLE
                binding.tvInicialMascotaAlerta.visibility = View.VISIBLE
            }
        }

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

        private fun configurarBotonResolver(alerta: AlertaPerdidaDTO) {
            val esPropia = alerta.idUsuario == idUsuarioLogueado
            binding.btnMascotaEncontrada.visibility =
                if (esPropia) View.VISIBLE else View.GONE

            binding.btnMascotaEncontrada.setOnClickListener {
                onResolverAlerta(alerta)
            }
        }

        private fun formatearFecha(fecha: String?): String {
            if (fecha == null) return "—"
            return try {
                val partes = fecha.substring(0, 10).split("-")
                "${partes[2]}/${partes[1]}/${partes[0]}"
            } catch (e: Exception) { fecha }
        }
    }
}