package com.sidedoor.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FrontDoorScreen(state: FrontDoorState, actions: FrontDoorActions) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header()
            if (!state.setup.instagramInstalled) {
                Notice("Instagram isn't installed on this phone.")
                return@Column
            }
            state.instagramChangedOn?.let { Notice("Instagram $it changed its screens. The guard may miss things until Sidedoor's rules are updated.") }
            if (!state.setup.complete) SetupCard(state.setup, actions)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                DoorTile(
                    title = "Chats",
                    caption = "Straight to your DMs",
                    emphasised = true,
                    onClick = actions::openChats,
                    modifier = Modifier.weight(1f),
                )
                DoorTile(
                    title = "Saved",
                    caption = if (state.guardRunning) "One tap, no detours" else "Needs the guard on",
                    emphasised = false,
                    onClick = actions::openSaved,
                    modifier = Modifier.weight(1f),
                )
            }
            ReelInboxCard(state.reels, actions)
            WeekCard(state.week)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Header() {
    Column(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
        Text(
            "Sidedoor",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            "Chats, reels friends send, your Saved. Nothing else.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
