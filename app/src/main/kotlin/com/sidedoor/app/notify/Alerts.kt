package com.sidedoor.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sidedoor.app.R
import com.sidedoor.app.ui.MainActivity

/** Sidedoor's own notification. Only one thing is worth interrupting you for. */
object Alerts {

    private const val CHANNEL = "sidedoor_alerts"
    private const val ID_IG_CHANGED = 2

    /** The inbox canary failed: Instagram's screens no longer match the rules. */
    fun instagramChanged(context: Context, version: String?) {
        post(
            context, ID_IG_CHANGED, R.string.alert_ig_changed_title,
            context.getString(R.string.alert_ig_changed_body, version ?: "?"),
        )
    }

    private fun post(context: Context, id: Int, title: Int, body: String) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context, id, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.alert_channel), NotificationManager.IMPORTANCE_DEFAULT)
        )
    }
}
