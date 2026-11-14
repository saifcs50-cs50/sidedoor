package com.sidedoor.app.ui

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sidedoor.app.data.WeekStats
import com.sidedoor.guard.notify.ReelQueue

private val CardShape = RoundedCornerShape(24.dp)

@Composable
fun DoorTile(title: String, caption: String, emphasised: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Card(
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (emphasised) colors.primary else colors.secondaryContainer,
            contentColor = if (emphasised) colors.onPrimary else colors.onSecondaryContainer,
        ),
        modifier = modifier.height(168.dp).clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(caption, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun ReelInboxCard(queue: ReelQueue, actions: FrontDoorActions) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Reels from friends", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (queue.unwatched.isNotEmpty()) TextButton(onClick = actions::markAllWatched) { Text("Mark all watched") }
        }
        val waiting = queue.unwatched
        if (waiting.isEmpty()) {
            Muted("You're caught up. Reels friends send line up here each time you open Chats.")
            return@SectionCard
        }
        Muted(queue.summary())
        waiting.take(MAX_ROWS).forEach { entry ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { actions.openReel(entry) }
                    .padding(vertical = 10.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(entry.sender, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Muted(entry.text, maxLines = 1)
                }
                Muted(DateUtils.getRelativeTimeSpanString(entry.sentAt).toString())
            }
        }
    }
}

@Composable
fun WeekCard(week: WeekStats) {
    SectionCard {
        Text("This week", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth()) {
            Stat("${week.instagramMinutes} min", "in Instagram", Modifier.weight(1f))
            Stat("${week.friendReels}", "friend reels", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth()) {
            Stat("${week.doorsClosed}", "doors closed", Modifier.weight(1f))
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
fun SetupCard(setup: Setup, actions: FrontDoorActions) {
    SectionCard(warning = true) {
        Text("Finish setting up", style = MaterialTheme.typography.titleMedium)
        SetupRow("Guard", "Closes the feed, Reels and Explore", setup.guardOn, actions::turnOnGuard)
        SetupRow("Reels from friends", "Reads DM notifications to list reels", setup.reelsOn, actions::turnOnReels)
        SetupRow("Alerts", "Tells you if Instagram changes under the guard", setup.alertsOn, actions::allowAlerts)
    }
}

@Composable
fun Notice(text: String) {
    SectionCard(warning = true) { Text(text, style = MaterialTheme.typography.bodyMedium) }
}

@Composable
private fun SetupRow(title: String, caption: String, done: Boolean, onFix: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Muted(caption)
        }
        if (done) Muted("On") else Button(onClick = onFix) { Text("Turn on") }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(modifier.padding(vertical = 8.dp)) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Muted(label)
    }
}

@Composable
private fun Muted(text: String, maxLines: Int = Int.MAX_VALUE) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun SectionCard(warning: Boolean = false, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (warning) colors.errorContainer else colors.surface,
            contentColor = if (warning) colors.onErrorContainer else colors.onSurface,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
    }
}

private const val MAX_ROWS = 8
