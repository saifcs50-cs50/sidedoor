package com.sidedoor.app.launch

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.sidedoor.guard.classify.IgIds

/** Everything Sidedoor knows about starting Instagram. */
object Instagram {

    /** Verified on Instagram 448: opens straight into the DM inbox, even from a cold start. */
    private const val INBOX_LINK = "instagram://direct-inbox"

    fun inboxIntent(): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(INBOX_LINK))
            .setPackage(IgIds.PACKAGE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun isInstalled(context: Context): Boolean = version(context) != null

    fun version(context: Context): String? = try {
        context.packageManager.getPackageInfo(IgIds.PACKAGE, 0).versionName
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
}
