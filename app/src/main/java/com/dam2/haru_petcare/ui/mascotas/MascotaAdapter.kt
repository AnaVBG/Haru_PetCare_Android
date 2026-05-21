package com.dam2.haru_petcare.ui.mascotas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.dam2.haru_petcare.databinding.ItemMascotaBinding
import com.dam2.haru_petcare.model.MascotaDTO

class MascotaAdapter(
    private val onClick: (MascotaDTO) -> Unit
) : RecyclerView.Adapter<MascotaAdapter.MascotaViewHolder>() {

    private var lista: List<MascotaDTO> = emptyList()

    fun setMascotas(nuevaLista: List<MascotaDTO>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MascotaViewHolder {
        val binding = ItemMascotaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MascotaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MascotaViewHolder, position: Int) {
        holder.bind(lista[position])
    }

    override fun getItemCount() = lista.size

    inner class MascotaViewHolder(
        private val binding: ItemMascotaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(mascota: MascotaDTO) {
            binding.tvNombreMascota.text  = mascota.nombre  ?: "Sin nombre"
            binding.tvEspecieMascota.text = "🐾 ${mascota.especie ?: "—"}"
            binding.tvRazaMascota.text    = mascota.raza    ?: "Desconocida"

            cargarAvatar(mascota)

            binding.root.setOnClickListener { onClick(mascota) }
        }

        /**
         * Carga la foto si hay URL; si no, muestra el círculo teal con la inicial.
         *
         * Glide usa listener para saber si la carga tuvo éxito:
         * - Éxito  → mostramos civFotoMascota, ocultamos la inicial
         * - Fallo  → ocultamos civFotoMascota, mostramos la inicial
         */
        private fun cargarAvatar(mascota: MascotaDTO) {
            val inicial = mascota.nombre?.firstOrNull()?.uppercase() ?: "?"
            binding.tvInicialMascota.text = inicial

            if (!mascota.fotoUrl.isNullOrBlank() && mascota.fotoUrl.startsWith("http")) {
                // Visible ANTES de Glide para que tenga dimensiones reales
                binding.civFotoMascota.visibility   = View.VISIBLE
                binding.tvInicialMascota.visibility = View.GONE
                Glide.with(binding.root.context)
                    .load(mascota.fotoUrl)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                        override fun onLoadFailed(
                            e: com.bumptech.glide.load.engine.GlideException?,
                            model: Any?,
                            target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            binding.civFotoMascota.visibility   = View.GONE
                            binding.tvInicialMascota.visibility = View.VISIBLE
                            return false
                        }
                        override fun onResourceReady(
                            resource: android.graphics.drawable.Drawable,
                            model: Any,
                            target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                            dataSource: com.bumptech.glide.load.DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }
                    })
                    .into(binding.civFotoMascota)
            } else {
                binding.civFotoMascota.visibility   = View.GONE
                binding.tvInicialMascota.visibility = View.VISIBLE
            }
        }
    }
}