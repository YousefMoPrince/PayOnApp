package com.jo.payonapp.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.jo.payonapp.MainActivity
import com.jo.payonapp.R

class PayOnNotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PayOn Real-time Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time payment notifications and security events"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun makeFCMNotification(
        notificationId: String,
        transactionId: String?,
        title: String,
        eventType: String,
        message: String
    ) {
        val finalTitle = title.ifBlank { "إشعار جديد من PayOn" }
        val finalMessage = message.ifBlank { "لديك تحديث جديد في حسابك" }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EVENT_TYPE", eventType)
            putExtra("NOTIFICATION_ID", notificationId)
            putExtra("TRANSACTION_ID", transactionId)
        }

        val reqCode = notificationId.hashCode()

        val pendingIntent = PendingIntent.getActivity(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.logobluee)
            .setContentTitle(finalTitle)
            .setContentText(finalMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(finalMessage))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(defaultSoundUri)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(reqCode, builder.build())
    }
    fun makeNotification(
        notificationId: String,
        title: String,
        eventType: String,
        message: String
    ) {
        val finalTitle = title.ifBlank { "إشعار جديد من PayOn" }
        val finalMessage = message.ifBlank { "لديك تحديث جديد في حسابك" }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EVENT_TYPE", eventType)
            putExtra("NOTIFICATION_ID", notificationId)
        }

        val reqCode = notificationId.hashCode()

        val pendingIntent = PendingIntent.getActivity(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.logobluee)
            .setContentTitle(finalTitle)
            .setContentText(finalMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(finalMessage))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(defaultSoundUri)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent).build()

        notificationManager.notify(reqCode, builder)
    }

    companion object {
        const val CHANNEL_ID = "payon_high_priority_v2"
    }
}