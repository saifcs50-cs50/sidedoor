package com.sidedoor.app.guard

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.ContextCompat
import com.sidedoor.app.BuildConfig
import com.sidedoor.app.R
import com.sidedoor.app.graph
import com.sidedoor.app.launch.Instagram
import com.sidedoor.app.notify.Alerts
import com.sidedoor.app.overlay.DeadZones
import com.sidedoor.app.overlay.Shield
import com.sidedoor.guard.canary.CanaryResult
import com.sidedoor.guard.canary.InboxCanary
import com.sidedoor.guard.classify.IgIds
import com.sidedoor.guard.classify.Screen
import com.sidedoor.guard.classify.ScreenClassifier
import com.sidedoor.guard.model.ScreenSnapshot
import com.sidedoor.guard.policy.Action
import com.sidedoor.guard.policy.BlockReason
import com.sidedoor.guard.policy.CorrectionLadder
import com.sidedoor.guard.policy.DeadZonePlanner
import com.sidedoor.guard.policy.Episode
import com.sidedoor.guard.policy.GuardPolicy
import com.sidedoor.guard.policy.GuardState
import com.sidedoor.guard.policy.Verdict
import com.sidedoor.guard.session.GuardSession
import com.sidedoor.guard.inbox.InboxRows
import com.sidedoor.guard.trip.Target
import com.sidedoor.guard.trip.TripGoal
import com.sidedoor.guard.trip.TripStep
import com.sidedoor.guard.trip.nextStep
import com.sidedoor.guard.window.Presence

/**
 * The guard. During a Sidedoor session - Instagram opened through Sidedoor -
 * it classifies every Instagram screen and closes the ones that are not chats,
 * reels friends sent, or Saved. Instagram opened any other way is not guarded,
 * or even read.
 *
 * All decisions live in :guard-core and are unit-tested; this class only
 * reads the screen, asks, and carries out the answer.
 */
class GuardService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val ladder = CorrectionLadder()

    private lateinit var shield: Shield
    private lateinit var deadZones: DeadZones

    private var session: GuardSession? = null
    private var sessionEnders: Set<String> = emptySet()
    private var policy = GuardState()
    private var lastScreen: Screen? = null
    private var episode: Episode? = null
    private var trip: Trip? = null
    private var canary: InboxCanary? = null
    private var instagramSeenAt: Long? = null
    private var pagerMovedAt = Long.MIN_VALUE / 2
    private var coveredSince: Long? = null
    private var notInFrontSince: Long? = null
    private var lastSnapshot: Pair<ScreenSnapshot, Screen>? = null

    private var evaluateDueAt: Long? = null
    private val evaluateRunnable = Runnable {
        evaluateDueAt = null
        evaluate()
    }
    private val watchdogRunnable = Runnable { watchdog() }

    private val dumpReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            lastSnapshot?.let { (snapshot, screen) -> DebugDump.write(context, snapshot, screen) }
        }
    }

    // lifecycle

    override fun onServiceConnected() {
        super.onServiceConnected()
        shield = Shield(this)
        deadZones = DeadZones(this)
        sessionEnders = HomeApps.sessionEnders(this)
        watchPackages(duringSession = false)
        graph.bus.setRunning(true)
        if (BuildConfig.DEBUG) {
            ContextCompat.registerReceiver(
                this, dumpReceiver, IntentFilter(DebugDump.ACTION), ContextCompat.RECEIVER_EXPORTED,
            )
        }
        Log.i(TAG, "Guard connected")
    }

    override fun onInterrupt() = hideOverlays()

    override fun onUnbind(intent: Intent?): Boolean {
        shutdown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        shutdown()
        super.onDestroy()
    }

    private fun shutdown() {
        if (!::shield.isInitialized) return
        handler.removeCallbacksAndMessages(null)
        hideOverlays()
        session = null
        graph.stats.flush()
        graph.bus.setRunning(false)
        if (BuildConfig.DEBUG) runCatching { unregisterReceiver(dumpReceiver) }
        Log.i(TAG, "Guard stopped")
    }

    // events

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // During a session the home screens are watched too, only so their
        // windows can be read; their events are not needed.
        if (event?.packageName?.toString() != IgIds.PACKAGE) return
        // Outside a Sidedoor session Instagram is none of the guard's business.
        if (session == null && !graph.bus.hasPendingSession(SystemClock.elapsedRealtime())) return
        if (lastScreen == Screen.FRIEND_REEL && isReelPagerMoving(event)) coverNextReel()
        scheduleEvaluate(
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) CONTENT_DELAY_MS else FAST_DELAY_MS
        )
    }

    /**
     * The reply bar that marks a friend's reel sits outside the reel pager, so
     * it stays on screen until a swipe settles - long enough for the next,
     * suggested reel to be seen. So the moment the pager itself starts moving
     * away from a friend's reel, the shield goes up first and questions come
     * after. Scrolling the comments is a different list and does not count.
     */
    private fun isReelPagerMoving(event: AccessibilityEvent): Boolean {
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && event.scrollDeltaY == 0) return false
        val source = event.source ?: return false
        val id = source.viewIdResourceName ?: source.parent?.viewIdResourceName
        return id?.endsWith(":id/${IgIds.CLIPS_VIEWER}") == true
    }

    private fun coverNextReel() {
        pagerMovedAt = SystemClock.elapsedRealtime()
        shield.show(BlockReason.END_OF_FRIEND_REELS.message)
    }

    /** Coalesces bursts of events into one evaluation, pulling it earlier if a faster one arrives. */
    private fun scheduleEvaluate(delayMs: Long) {
        val due = SystemClock.uptimeMillis() + delayMs
        val pending = evaluateDueAt
        if (pending != null && pending <= due) return
        handler.removeCallbacks(evaluateRunnable)
        evaluateDueAt = due
        handler.postAtTime(evaluateRunnable, due)
    }

    // evaluate

    private fun evaluate() {
        val now = SystemClock.elapsedRealtime()
        val scan = WindowReader.scan(this)
        val root = scan.instagramRoot
        if (root == null) {
            if (session == null) return
            accountTime(scan.presence, now)
            if (isCovered(scan, now)) {
                hideOverlays()
                if (coveredSince == null) Log.i(TAG, "covered")
                coveredSince = coveredSince ?: now
                startWatchdog()
            } else {
                onNotInFront(scan, now)
            }
            return
        }
        notInFrontSince = null
        if (!inSession(now)) return
        accountTime(scan.presence, now)
        coveredSince = null
        val snapshot = SnapshotReader.read(root)
        if (snapshot.isEmpty) return
        val screen = ScreenClassifier.classify(snapshot)
        lastSnapshot = snapshot to screen
        onScreen(screen, snapshot, now)

        val decision = GuardPolicy.decide(policy, screen)
        policy = decision.next
        when (val verdict = decision.verdict) {
            Verdict.Allow -> allow(screen, snapshot, root, scan.presence, now)
            is Verdict.Block -> block(verdict, root, now)
        }
        startWatchdog()
    }

    private fun onScreen(screen: Screen, snapshot: ScreenSnapshot, now: Long) {
        if (screen != lastScreen) {
            Log.i(TAG, "screen=$screen")
            if (screen == Screen.FRIEND_REEL) {
                graph.stats.countFriendReel()
                snapshot.first(SENDER_ID)?.text?.let(graph.reelInbox::markOneWatchedFrom)
            }
        }
        lastScreen = screen
        graph.bus.takeTrip(now)?.let { goal ->
            Log.i(TAG, "${goal.logName} trip started on $screen")
            trip = Trip(goal, startedAt = now)
        }
        // The inbox rows say which reels are still waiting; this is what fills
        // the reels list when Instagram's own notifications are off.
        if (screen == Screen.INBOX) graph.reelInbox.syncFromInbox(InboxRows.parse(snapshot), System.currentTimeMillis())
        graph.bus.takeCanary(now)?.let { canary = InboxCanary.arm(it) }
        canary?.let { settleCanary(it.onScreen(screen, now)) }
    }

    private fun allow(
        screen: Screen,
        snapshot: ScreenSnapshot,
        root: AccessibilityNodeInfo,
        presence: Presence,
        now: Long,
    ) {
        episode = null
        if (trip != null && advanceTrip(screen, snapshot, root, now)) return
        val pagerSettling = screen == Screen.FRIEND_REEL && now - pagerMovedAt < PAGER_SETTLE_MS
        when {
            // Keep the cover up until the swipe lands, then look again.
            pagerSettling -> scheduleEvaluate(PAGER_SETTLE_MS)
            screen == Screen.UNKNOWN -> shield.hideAfter(UNKNOWN_GRACE_MS)
            else -> shield.hide()
        }
        deadZones.show(if (presence.canShowDeadZones) DeadZonePlanner.plan(snapshot) else emptyList())
    }

    private fun block(verdict: Verdict.Block, root: AccessibilityNodeInfo, now: Long) {
        deadZones.hide()
        shield.show(verdict.reason.message)
        val current = episode?.takeIf { it.correction == verdict.correction }
        if (current == null) {
            graph.stats.countDoorClosed()
            Log.i(TAG, "blocked ${verdict.screen}: ${verdict.reason}")
            // Every block leaves evidence in debug builds, so a false positive can become a fixture.
            if (BuildConfig.DEBUG) lastSnapshot?.let { (snapshot, screen) -> DebugDump.write(this, snapshot, screen) }
        }
        val (action, next) = ladder.next(current ?: Episode.start(verdict.correction, now), now)
        episode = next
        action?.let { perform(it, root) }
        // Keep the ladder climbing even if Instagram stops sending events.
        scheduleEvaluate(CorrectionLadder.RETRY_MS + 50)
    }

    private fun perform(action: Action, root: AccessibilityNodeInfo) {
        Log.i(TAG, "action=$action")
        when (action) {
            Action.Back -> performGlobalAction(GLOBAL_ACTION_BACK)
            Action.TapInboxTab -> if (!NodeActions.tap(this, root, Target.ById(IgIds.TAB_DIRECT))) {
                openInbox()
            }
            Action.OpenInboxLink -> openInbox()
            Action.GoHome -> performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    private fun openInbox() {
        // An enabled accessibility service is exempt from background-activity-launch limits.
        runCatching { startActivity(Instagram.inboxIntent()) }
            .onFailure { Log.e(TAG, "Could not open the inbox link", it) }
    }

    // session

    /**
     * Whether this visit is guarded. A live session carries on unless the
     * detour away from Instagram ran too long; otherwise a fresh request from
     * Sidedoor starts one. No session, no guard.
     */
    private fun inSession(now: Long): Boolean {
        session?.let { current ->
            val resumed = current.returned(now)
            if (resumed == null) endSession("detour ran long") else session = resumed
        }
        if (session == null && graph.bus.takeSession(now)) {
            session = GuardSession.start(now)
            watchPackages(duringSession = true)
            Log.i(TAG, "session started")
        }
        return session != null
    }

    /**
     * The service may only read windows of the packages it watches. To tell
     * "went to the home screen" (session over) from "popped into another app"
     * (a detour), the home screens and Sidedoor itself are added for the length
     * of a session, and dropped again when it ends.
     */
    private fun watchPackages(duringSession: Boolean) {
        val info = serviceInfo ?: return
        val watched = if (duringSession) sessionEnders + IgIds.PACKAGE else setOf(IgIds.PACKAGE)
        info.packageNames = watched.toTypedArray()
        serviceInfo = info
    }

    private fun endSession(reason: String) {
        Log.i(TAG, "session ended: $reason")
        session = null
        notInFrontSince = null
        watchPackages(duringSession = false)
        policy = GuardState()
        trip = null
        canary = null
        episode = null
        lastScreen = null
        hideOverlays()
        handler.removeCallbacks(watchdogRunnable)
        graph.stats.flush()
    }

    // trips

    /** A walk to Saved or to one chat. [progressAt] moves every time it reaches a new screen. */
    private data class Trip(
        val goal: TripGoal,
        val startedAt: Long,
        val lastStepAt: Long = 0,
        val progressAt: Long = startedAt,
        val lastScreen: Screen? = null,
    )

    /** Returns true while the trip is still under way (the shield stays up). */
    private fun advanceTrip(screen: Screen, snapshot: ScreenSnapshot, root: AccessibilityNodeInfo, now: Long): Boolean {
        val current = (trip ?: return false).let {
            if (it.lastScreen != screen) it.copy(progressAt = now, lastScreen = screen) else it
        }
        trip = current
        // A cold Instagram can take seconds to draw Settings, so the limit is on
        // standing still, with an overall cap as a backstop.
        if (now - current.progressAt > TRIP_STALL_MS || now - current.startedAt > TRIP_CAP_MS) {
            Log.w(TAG, "${current.goal.logName} trip timed out on $screen")
            if (BuildConfig.DEBUG) lastSnapshot?.let { (shot, seen) -> DebugDump.write(this, shot, seen) }
            trip = null
            return false
        }
        val step = nextStep(current.goal, screen, snapshot)
        if (step == TripStep.Arrived) {
            Log.i(TAG, "${current.goal.logName} trip arrived")
            trip = null
            return false
        }
        // Touchable: a stray tap during the trip must not land on Instagram and
        // fight the guard's own navigation.
        shield.show(
            when (val goal = current.goal) {
                TripGoal.Saved -> getString(R.string.shield_opening_saved)
                is TripGoal.Chat -> getString(R.string.shield_opening_chat, goal.thread)
            }
        )
        if (now - current.lastStepAt >= TRIP_STEP_MS) {
            when (step) {
                is TripStep.Tap -> NodeActions.tap(this, root, step.target)
                TripStep.Back -> {
                    Log.i(TAG, "trip: back from $screen")
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }
                else -> Unit
            }
            if (step != TripStep.Wait) trip = current.copy(lastStepAt = now)
        }
        scheduleEvaluate(TRIP_STEP_MS)
        return true
    }

    private val TripGoal.logName: String
        get() = when (this) {
            TripGoal.Saved -> "Saved"
            is TripGoal.Chat -> "Chat"
        }

    // canary

    private fun settleCanary(result: CanaryResult) {
        if (result == CanaryResult.Pending) return
        canary = null
        val version = Instagram.version(this)
        if (result == CanaryResult.Failed) {
            Log.w(TAG, "Canary failed on Instagram $version")
            Alerts.instagramChanged(this, version)
        }
        graph.bus.reportCanary(if (result == CanaryResult.Failed) version else null)
    }

    // presence

    /**
     * Overlays must never outlive Instagram being in front: a dead zone left
     * behind would eat taps on the home screen's dock. While Instagram might be
     * in front, re-check the window stack on a short timer, independent of
     * whether Instagram happens to be sending events.
     */
    private fun startWatchdog() {
        handler.removeCallbacks(watchdogRunnable)
        // Faster while covered: the shade closing is the moment the tabs would be bare.
        handler.postDelayed(watchdogRunnable, if (coveredSince != null) COVERED_POLL_MS else WATCHDOG_MS)
    }

    private fun watchdog() {
        val now = SystemClock.elapsedRealtime()
        if (!isScreenInUse()) {
            // Phone put down: nothing to guard and nothing to watch. Instagram's
            // own events bring the guard back after unlock.
            hideOverlays()
            coveredSince = null
            return
        }
        val scan = WindowReader.scan(this)
        accountTime(scan.presence, now)
        canary?.let { settleCanary(it.onTick(now)) }
        when {
            scan.presence.appInFront -> {
                notInFrontSince = null
                // Back from under the shade or a dialog: Instagram may not send
                // an event, so look again now rather than leave the tabs bare.
                val wasCovered = coveredSince
                coveredSince = null
                if (wasCovered != null) {
                    Log.i(TAG, "uncovered after ${now - wasCovered}ms")
                    evaluate()
                } else if (!scan.presence.canShowDeadZones) {
                    deadZones.hide()
                }
            }
            isCovered(scan, now) -> {
                hideOverlays()
                if (coveredSince == null) Log.i(TAG, "covered")
                coveredSince = coveredSince ?: now
            }
            else -> {
                coveredSince = null
                onNotInFront(scan, now)
                return
            }
        }
        startWatchdog()
    }

    /**
     * Instagram is still there, just under the shade or a dialog: hide the
     * overlays but keep checking, so they are back the moment it closes. The
     * lock screen also counts as covering, hence the cap.
     */
    private fun isCovered(scan: WindowScan, now: Long): Boolean {
        val covered = scan.instagramOnScreen || (scan.presence.coveredBySystem && lastScreen != null)
        return covered && now - (coveredSince ?: now) < MAX_COVERED_MS
    }

    /**
     * What replaced Instagram, for the session. A dark or locked screen is not
     * leaving: a visit survives the phone being put down.
     */
    /**
     * Instagram swaps activities (inbox to chat) with a moment in which none
     * of its windows is up - about 0.35 s on the test phone. That is not leaving. The
     * overlays go at once, but the visit is judged only once Instagram has
     * stayed gone for [LEAVE_CONFIRM_MS].
     */
    private fun onNotInFront(scan: WindowScan, now: Long) {
        hideOverlays()
        val since = notInFrontSince ?: now.also { notInFrontSince = it }
        if (now - since >= LEAVE_CONFIRM_MS) {
            if (BuildConfig.DEBUG) Log.d(TAG, "not in front; windows: ${scan.describe()}")
            leftInstagram(replacedBy(scan.presence))
        } else {
            handler.removeCallbacks(watchdogRunnable)
            handler.postDelayed(watchdogRunnable, LEAVE_CONFIRM_MS)
        }
    }

    private fun replacedBy(presence: Presence): String? = if (isScreenInUse()) presence.replacedBy else null

    private fun isScreenInUse(): Boolean =
        getSystemService(PowerManager::class.java).isInteractive &&
            !getSystemService(KeyguardManager::class.java).isKeyguardLocked

    /** [frontApp] is what replaced Instagram in front: home ends the session, another app is a detour. */
    private fun leftInstagram(frontApp: String?) {
        val now = SystemClock.elapsedRealtime()
        hideOverlays()
        episode = null
        instagramSeenAt = null
        lastScreen = null
        graph.stats.flush()
        handler.removeCallbacks(watchdogRunnable)
        // The inbox check only judges the visit it was armed for: leaving before
        // Instagram finished loading must not turn into a false alarm later.
        canary = null
        val current = session ?: return
        val next = current.leftFor(frontApp, sessionEnders, now)
        when {
            next == null -> endSession("left for $frontApp")
            next.isOverdue(now) -> endSession("detour ran long")
            else -> {
                if (current.awaySince == null && next.awaySince != null) Log.i(TAG, "away: $frontApp")
                session = next
                // Keep looking while away: the home screen can be unreadable for a
                // moment mid-animation, and a detour can end at the home screen.
                if (next.awaySince != null) handler.postDelayed(watchdogRunnable, DETOUR_POLL_MS)
            }
        }
    }

    private fun hideOverlays() {
        if (!::shield.isInitialized) return
        shield.hide()
        deadZones.hide()
    }

    private fun accountTime(presence: Presence, now: Long) {
        val since = instagramSeenAt
        if (presence.appInFront && since != null) {
            graph.stats.addInstagramTime((now - since).coerceAtMost(MAX_TIME_STEP_MS))
        }
        instagramSeenAt = if (presence.appInFront) now else null
    }

    private companion object {
        const val TAG = "Sidedoor"
        const val SENDER_ID = "sender_username_or_fullname"

        /** Window changes and scrolls (a swipe to the next reel) are handled almost at once. */
        const val FAST_DELAY_MS = 30L

        /** Content changes fire constantly while video plays; this caps the tree reads. */
        const val CONTENT_DELAY_MS = 200L

        const val UNKNOWN_GRACE_MS = 400L
        const val PAGER_SETTLE_MS = 450L
        /** How soon overlays go when Instagram loses the front: the app that replaced it sends Sidedoor no events. */
        const val WATCHDOG_MS = 300L
        const val MAX_COVERED_MS = 60_000L
        const val COVERED_POLL_MS = 200L
        const val DETOUR_POLL_MS = 1_000L
        const val LEAVE_CONFIRM_MS = 500L
        const val MAX_TIME_STEP_MS = 2_000L
        const val TRIP_STEP_MS = 650L
        const val TRIP_STALL_MS = 12_000L
        const val TRIP_CAP_MS = 25_000L
    }
}
