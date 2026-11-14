package com.sidedoor.app.guard

import android.content.Context
import android.content.Intent

/** Apps whose arrival in front ends a guarded session: home screens, and Sidedoor itself. */
object HomeApps {

    fun sessionEnders(context: Context): Set<String> {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val launchers = context.packageManager.queryIntentActivities(home, 0).map { it.activityInfo.packageName }
        return (launchers + context.packageName).toSet()
    }
}
