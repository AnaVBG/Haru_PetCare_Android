package com.dam2.haru_petcare.model

import com.google.gson.annotations.SerializedName

data class MascotaDTO(
    val id: Long?,
    val nombre: String?,
    val especie: String?,
    val raza: String?,
    // @SerializedName: cuando el nombre del campo JSON no coincide
    // con el nombre de la propiedad Kotlin (camelCase vs snake_case)
    @SerializedName("fechaNacimiento")
    val fechaNacimiento: String?,
    val fotoUrl: String?,
    val duenoId: Long?
)