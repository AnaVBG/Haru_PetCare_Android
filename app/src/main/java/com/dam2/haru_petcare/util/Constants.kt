package com.dam2.haru_petcare.util

object Constants {

    // ── RED ───────────────────────────────────────────────────────────────
    const val BASE_URL = "https://harupetcarehub-production.up.railway.app/"

    // ── ROLES ─────────────────────────────────────────────────────────────
    const val ROL_DUENO       = "DUENO"
    const val ROL_VETERINARIO = "VETERINARIO"

    // ── INTENTS ───────────────────────────────────────────────────────────
    // Claves para pasar datos entre Activities con Intent
    const val EXTRA_MASCOTA_ID = "mascota_id"
    const val EXTRA_CITA_ID    = "cita_id"

    const val CLOUDINARY_CLOUD_NAME    = "TU_CLOUD_NAME"
    const val CLOUDINARY_UPLOAD_PRESET = "haru_petcare"
    const val CLOUDINARY_UPLOAD_URL    = "https://api.cloudinary.com/v1_1/$CLOUDINARY_CLOUD_NAME/image/upload"
}