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
import com.dam2.haru_petcare.util.Constants
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class HaruFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID   = "haru_alertas_channel"
        private const val CHANNEL_NAME = "Alertas de mascotas perdidas"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val titulo = message.notification?.title ?: "Alerta Haru"
        val cuerpo = message.notification?.body  ?: "Una mascota se ha perdido cerca de ti"

        mostrarNotificacion(titulo, cuerpo)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val prefs = getSharedPreferences("haru_session", Context.MODE_PRIVATE)
        prefs.edit().putString("token_fcm_pendiente", token).apply()

        val idUsuario = prefs.getLong("usuario_id", -1L)
        val jwtToken  = prefs.getString("jwt_token", null)

        if (idUsuario != -1L && jwtToken != null) {
            enviarTokenAlBackend(idUsuario, token, jwtToken)
        }
    }

    private fun mostrarNotificacion(titulo: String, cuerpo: String) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de mascotas perdidas cerca de tu ubicación"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(canal)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

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
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notificacion)
    }

    private fun enviarTokenAlBackend(idUsuario: Long, tokenFcm: String, jwtToken: String) {
        Thread {
            try {
                val baseUrl = Constants.BASE_URL.trimEnd('/')
                val url = java.net.URL("$baseUrl/api/auth/fcm/$idUsuario")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.apply {
                    requestMethod = "PUT"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $jwtToken")
                    doOutput = true
                    outputStream.write("\"$tokenFcm\"".toByteArray())
                }
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}