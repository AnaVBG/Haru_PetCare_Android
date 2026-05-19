package com.dam2.haru_petcare.model

import com.google.gson.annotations.SerializedName

data class MascotaDTO(
    val id: Long?,
    val nombre: String?,
    val especie: String?,
    val raza: String?,
    @SerializedName("fechaNacimiento")
    val fechaNacimiento: String?,
    val fotoUrl: String?,
    val duenoId: Long?,
    val nombreDueno: String?   // nombre del dueño, usado en la vista del veterinario
)