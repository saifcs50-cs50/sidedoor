package com.sidedoor.app.notify

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.sidedoor.app.guard.GuardService

/** Whether each of Sidedoor's two system grants is actually switched on right now. */
object GuardStatus {

    fun isGuardEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        val me = ComponentName(context, GuardService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    fun isReelReaderEnabled(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun canPostAlerts(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()
}
