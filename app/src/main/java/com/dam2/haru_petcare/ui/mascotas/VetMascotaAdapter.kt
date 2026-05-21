package com.dam2.haru_petcare.ui.mascotas

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.dam2.haru_petcare.databinding.ItemVetMascotaBinding
import com.dam2.haru_petcare.model.MascotaDTO

class VetMascotaAdapter(
    private val onClick: (MascotaDTO) -> Unit
) : RecyclerView.Adapter<VetMascotaAdapter.ViewHolder>() {

    private var lista: List<MascotaDTO> = emptyList()

    fun setLista(nuevaLista: List<MascotaDTO>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemVetMascotaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(mascota: MascotaDTO) {
            binding.tvNombreMascota.text = mascota.nombre ?: "—"
            binding.tvEspecieRaza.text   = "${mascota.especie ?: "—"} · ${mascota.raza ?: "—"}"
            binding.tvNombreDueno.text   = "Dueño/a: ${mascota.nombreDueno ?: "—"}"

            cargarAvatar(mascota)

            binding.root.setOnClickListener { onClick(mascota) }
        }

        private fun cargarAvatar(mascota: MascotaDTO) {
            val inicial = mascota.nombre?.firstOrNull()?.uppercase() ?: "?"
            binding.tvInicialMascota.text = inicial

            if (!mascota.fotoUrl.isNullOrBlank() && mascota.fotoUrl.startsWith("http")) {
                Glide.with(binding.root.context)
                    .load(mascota.fotoUrl)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            binding.civFotoMascota.visibility   = View.GONE
                            binding.tvInicialMascota.visibility = View.VISIBLE
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            binding.civFotoMascota.visibility   = View.VISIBLE
                            binding.tvInicialMascota.visibility = View.GONE
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVetMascotaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(lista[position])

    override fun getItemCount() = lista.size
}