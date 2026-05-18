package com.dam2.haru_petcare.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.dam2.haru_petcare.R
import com.dam2.haru_petcare.ui.main.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * HaruFirebaseMessagingService: escucha las notificaciones push de Firebase.
 *
 * Android arranca este servicio automáticamente cuando llega una
 * notificación FCM, incluso si la app está en segundo plano o cerrada.
 *
 * Dos métodos clave:
 * - onMessageReceived: se llama cuando llega una notificación y la app
 *   está en primer plano. Nosotros construimos y mostramos la notificación.
 * - onNewToken: se llama cuando Firebase genera o renueva el token del
 *   dispositivo. Lo enviamos al backend para mantenerlo actualizado.
 */
class HaruFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID   = "haru_alertas_channel"
        private const val CHANNEL_NAME = "Alertas de mascotas perdidas"
        private const val NOTIFICATION_ID = 1001
    }

    /**
     * Se llama cuando llega una notificación push CON la app en primer plano.
     * Si la app está en segundo plano, Firebase muestra la notificación
     * automáticamente sin llamar a este método.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val titulo = message.notification?.title ?: "🐾 Alerta Haru"
        val cuerpo = message.notification?.body  ?: "Una mascota se ha perdido cerca de ti"

        mostrarNotificacion(titulo, cuerpo)
    }

    /**
     * Se llama cuando Firebase genera un nuevo token para este dispositivo.
     * Ocurre la primera vez que la app se instala, y ocasionalmente cuando
     * Firebase rota los tokens por seguridad.
     *
     * IMPORTANTE: si no enviamos el nuevo token al backend, las notificaciones
     * dejarán de llegar a este dispositivo.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Guardamos el token localmente para enviarlo al backend
        // cuando el usuario esté logueado
        val prefs = getSharedPreferences("haru_session", Context.MODE_PRIVATE)
        prefs.edit().putString("token_fcm_pendiente", token).apply()

        // Si ya hay sesión activa, lo enviamos al backend ahora mismo
        val idUsuario = prefs.getLong("usuario_id", -1L)
        val jwtToken  = prefs.getString("jwt_token", null)

        if (idUsuario != -1L && jwtToken != null) {
            enviarTokenAlBackend(idUsuario, token, jwtToken)
        }
    }

    /**
     * Construye y muestra la notificación en la barra de estado del dispositivo.
     *
     * NotificationChannel: obligatorio desde Android 8 (API 26).
     * Sin canal, las notificaciones no se muestran en versiones modernas.
     */
    private fun mostrarNotificacion(titulo: String, cuerpo: String) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear el canal si no existe (en Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                // IMPORTANCE_HIGH: muestra la notificación como heads-up
                // (aparece flotando en la parte superior de la pantalla)
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de mascotas perdidas cerca de tu ubicación"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(canal)
        }

        // Al pulsar la notificación, abre MainActivity
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        // PendingIntent: envuelve el Intent para que el sistema pueda
        // lanzarlo más tarde cuando el usuario pulse la notificación
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificacion = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notificacion_pata)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // se borra al pulsar
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notificacion)
    }

    /**
     * Envía el token FCM al backend usando HttpURLConnection.
     * Usamos HttpURLConnection en vez de Retrofit porque este método
     * se llama desde un Service, fuera del contexto de una Activity,
     * y no tenemos acceso al ciclo de vida de Retrofit fácilmente.
     */
    private fun enviarTokenAlBackend(idUsuario: Long, tokenFcm: String, jwtToken: String) {
        // Ejecutamos en un hilo separado — nunca en el hilo principal
        Thread {
            try {
                val prefs   = getSharedPreferences("haru_session", Context.MODE_PRIVATE)
                val baseUrl = "http://10.0.2.2:8080" // ajustar según entorno

                val url = java.net.URL("$baseUrl/api/auth/fcm/$idUsuario")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.apply {
                    requestMethod = "PUT"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $jwtToken")
                    doOutput = true
                    outputStream.write("\"$tokenFcm\"".toByteArray())
                }
                conn.responseCode // ejecuta la petición
                conn.disconnect()
            } catch (e: Exception) {
                // Si falla, el token se enviará la próxima vez que el usuario abra la app
                e.printStackTrace()
            }
        }.start()
    }
}