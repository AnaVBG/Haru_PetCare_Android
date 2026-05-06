package com.dam2.haru_petcare.ui.mascotas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
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

            // Inicial del nombre para el avatar circular
            // 'firstOrNull()' devuelve el primer carácter o null si el string está vacío
            // 'uppercase()' lo pone en mayúscula
            val inicial = mascota.nombre?.firstOrNull()?.uppercase() ?: "?"
            binding.tvInicialMascota.text = inicial

            binding.root.setOnClickListener { onMascotaClick(mascota) }
        }
    }
}