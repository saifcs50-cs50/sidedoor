package com.sidedoor.app.notify

import android.app.Notification
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat

/** One message as it appears in an Instagram DM notification. */
data class IncomingMessage(val sender: String, val text: String, val sentAt: Long)

/**
 * Pulls individual messages out of an Instagram DM notification. Chats post
 * in MessagingStyle, which carries every recent message with its own sender
 * and timestamp; anything else is read as a single title + text message.
 */
object DmNotificationReader {

    fun read(sbn: StatusBarNotification): List<IncomingMessage> {
        val notification = sbn.notification
        val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        val messages = style?.messages.orEmpty()
        if (messages.isNotEmpty()) {
            return messages.mapNotNull { m ->
                val text = m.text?.toString() ?: return@mapNotNull null
                val sender = m.person?.name?.toString() ?: style?.conversationTitle?.toString() ?: title
                IncomingMessage(sender, text, m.timestamp)
            }
        }
        val text = (notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: notification.extras.getCharSequence(Notification.EXTRA_TEXT))?.toString()
            ?: return emptyList()
        return listOf(IncomingMessage(title, text, sbn.postTime))
    }
}
