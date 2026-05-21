package com.dam2.haru_petcare.model

import com.google.gson.annotations.SerializedName

data class MascotaActualizarDTO(
    val nombre: String?,
    val especie: String?,
    val raza: String?,
    @SerializedName("fechaNacimiento")
    val fechaNacimiento: String?,
    val fotoUrl: String?
)