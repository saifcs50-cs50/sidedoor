package com.sidedoor.app.ui

import android.Manifest
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.sidedoor.app.graph
import com.sidedoor.app.launch.Instagram
import com.sidedoor.app.notify.GuardStatus
import com.sidedoor.guard.notify.ReelEntry
import com.sidedoor.guard.trip.TripGoal

/** The Front Door: the only way into Instagram you need. */
class MainActivity : ComponentActivity(), FrontDoorActions {

    private val setup = mutableStateOf(Setup())
    private val alertsPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshSetup()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null && handleShortcut(intent)) return
        enableEdgeToEdge()
        setContent {
            val running by graph.bus.running.collectAsState()
            val week by graph.stats.week.collectAsState()
            val reels by graph.reelInbox.queue.collectAsState()
            val changedOn by graph.bus.canaryFailure.collectAsState()
            SidedoorTheme {
                FrontDoorScreen(
                    state = FrontDoorState(setup.value, running, week, reels, changedOn),
                    actions = this,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshSetup()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShortcut(intent)
    }

    /** Home-screen shortcuts go straight through the door and leave the Front Door out of the back stack. */
    private fun handleShortcut(intent: Intent?): Boolean {
        when (intent?.action) {
            ACTION_OPEN_CHATS -> openChats()
            ACTION_OPEN_SAVED -> openSaved()
            else -> return false
        }
        finish()
        return true
    }

    private fun refreshSetup() {
        setup.value = Setup(
            instagramInstalled = Instagram.isInstalled(this),
            guardOn = GuardStatus.isGuardEnabled(this),
            reelsOn = GuardStatus.isReelReaderEnabled(this),
            alertsOn = GuardStatus.canPostAlerts(this),
        )
    }

    // ---------------------------------------------------------------- doors

    override fun openChats() {
        graph.bus.requestCanary(SystemClock.elapsedRealtime())
        startSession()
        launchInbox()
    }

    override fun openSaved() {
        graph.bus.requestTrip(TripGoal.Saved, SystemClock.elapsedRealtime())
        startSession()
        launchInbox()
    }

    /**
     * Instagram's own "open this chat" intent when a notification left one;
     * otherwise the guard walks from the inbox to that chat's row.
     */
    override fun openReel(entry: ReelEntry) {
        startSession()
        val opener = graph.reelInbox.openerFor(entry.sender)
        if (opener != null && send(opener)) return
        graph.bus.requestTrip(TripGoal.Chat(entry.sender), SystemClock.elapsedRealtime())
        launchInbox()
    }

    /** Only visits started here are guarded; Instagram opened any other way is left alone. */
    private fun startSession() {
        if (!graph.bus.running.value) {
            Toast.makeText(this, "The guard is off, so this visit isn't guarded", Toast.LENGTH_LONG).show()
        }
        graph.bus.requestSession(SystemClock.elapsedRealtime())
    }

    override fun markAllWatched() = graph.reelInbox.markAllWatched()

    private fun launchInbox() {
        runCatching { startActivity(Instagram.inboxIntent()) }
            .onFailure { Toast.makeText(this, "Couldn't open Instagram", Toast.LENGTH_SHORT).show() }
    }

    /** Fires Instagram's own "open this chat" intent from its notification. */
    private fun send(opener: PendingIntent): Boolean = try {
        val options = ActivityOptions.makeBasic().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
            }
        }
        opener.send(this, 0, null, null, null, null, options.toBundle())
        true
    } catch (e: PendingIntent.CanceledException) {
        Log.i("Sidedoor", "Chat link expired; opening the inbox instead", e)
        false
    }

    // ---------------------------------------------------------------- setup

    private companion object {
        const val ACTION_OPEN_CHATS = "com.sidedoor.app.action.OPEN_CHATS"
        const val ACTION_OPEN_SAVED = "com.sidedoor.app.action.OPEN_SAVED"
    }

    override fun turnOnGuard() = startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    override fun turnOnReels() = startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))

    override fun allowAlerts() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            alertsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            )
        }
    }
}
