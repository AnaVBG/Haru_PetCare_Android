package com.dam2.haru_petcare.ui.mascotas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.dam2.haru_petcare.R
import com.dam2.haru_petcare.databinding.ItemMascotaBinding
import com.dam2.haru_petcare.model.MascotaDTO

class MascotaAdapter(
    private val onMascotaClick: (MascotaDTO) -> Unit
) : RecyclerView.Adapter<MascotaAdapter.MascotaViewHolder>() {

    private val mascotas = mutableListOf<MascotaDTO>()

    fun setMascotas(nuevaLista: List<MascotaDTO>) {
        mascotas.clear()
        mascotas.addAll(nuevaLista)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MascotaViewHolder {
        val binding = ItemMascotaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MascotaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MascotaViewHolder, position: Int) {
        holder.bind(mascotas[position])
    }

    override fun getItemCount() = mascotas.size

    inner class MascotaViewHolder(
        private val binding: ItemMascotaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(mascota: MascotaDTO) {
            binding.tvNombreMascota.text  = mascota.nombre  ?: "Sin nombre"
            binding.tvEspecieMascota.text = mascota.especie ?: ""
            binding.tvRazaMascota.text    = mascota.raza    ?: "Raza desconocida"

            val inicial = mascota.nombre?.firstOrNull()?.uppercase() ?: "?"
            binding.tvInicialMascota.text = inicial

            if (!mascota.fotoUrl.isNullOrBlank()) {
                // Hay foto — la cargamos con Glide en círculo
                // y ocultamos el fallback de la inicial
                binding.ivFotoMascota.visibility = View.VISIBLE
                binding.viewFondoAvatar.visibility = View.GONE
                binding.tvInicialMascota.visibility = View.GONE

                Glide.with(binding.root.context)
                    .load(mascota.fotoUrl)
                    .transform(CircleCrop())
                    .placeholder(R.drawable.bg_avatar_circle)
                    .error(R.drawable.bg_avatar_circle)
                    .into(binding.ivFotoMascota)
            } else {
                // Sin foto — mostramos el círculo teal con la inicial
                binding.ivFotoMascota.visibility = View.GONE
                binding.viewFondoAvatar.visibility = View.VISIBLE
                binding.tvInicialMascota.visibility = View.VISIBLE
            }

            binding.root.setOnClickListener { onMascotaClick(mascota) }
        }
    }
}