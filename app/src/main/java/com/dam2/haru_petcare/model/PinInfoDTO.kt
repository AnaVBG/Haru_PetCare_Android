package com.dam2.haru_petcare.model

import com.google.android.gms.maps.model.Marker

data class PinInfo(
    val marker: Marker,
    val idPin: Long,
    val idUsuario: Long,
    val tipo: String,
    val descripcion: String,
    val nombreUsuario: String
)
