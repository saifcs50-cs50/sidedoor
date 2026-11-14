package com.sidedoor.app.notify

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.sidedoor.app.graph
import com.sidedoor.guard.classify.IgIds
import com.sidedoor.guard.notify.NotificationRules
import com.sidedoor.guard.notify.ReelEntry

/**
 * Reads Instagram's DM notifications to line up the reels friends send in
 * Sidedoor's "Reels from friends" list. Read-only: every notification is left
 * exactly as Instagram posted it.
 */
class ReelInboxListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != IgIds.PACKAGE || !NotificationRules.isMessage(sbn.notification.channelId)) return
        DmNotificationReader.read(sbn)
            .filter { NotificationRules.isReelShare(listOf(it.text)) }
            .forEach { m -> graph.reelInbox.add(ReelEntry(m.sender, m.text, m.sentAt), sbn.notification.contentIntent) }
    }
}
