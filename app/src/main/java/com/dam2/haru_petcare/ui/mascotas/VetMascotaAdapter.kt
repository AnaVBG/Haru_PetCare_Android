package com.dam2.haru_petcare.ui.mascotas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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

            if (!mascota.fotoUrl.isNullOrBlank()) {
                Glide.with(binding.root.context)
                    .load(mascota.fotoUrl)
                    .centerCrop()
                    .into(binding.ivFotoMascota)
            }

            binding.root.setOnClickListener { onClick(mascota) }
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