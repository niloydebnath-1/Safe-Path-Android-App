package com.example.nirapod.service

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.nirapod.MainActivity
import com.example.nirapod.NirapodApplication
import com.example.nirapod.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NirapodMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        val app = application as? NirapodApplication ?: return
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { app.container.authRepository.saveFcmToken(token) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = message.notification?.title ?: message.data["title"] ?: "Nirapod alert"
        val body = message.notification?.body ?: message.data["body"] ?: "A report has been updated."
        val notification = NotificationCompat.Builder(this, NirapodApplication.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        runCatching { NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification) }
    }
}
