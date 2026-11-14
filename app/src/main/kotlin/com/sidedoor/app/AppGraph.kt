package com.sidedoor.app

import android.content.Context
import com.sidedoor.app.data.ReelInboxStore
import com.sidedoor.app.data.StatsStore
import com.sidedoor.app.guard.GuardBus

/**
 * Process-wide singletons. The guard service, the notification listener and
 * the Front Door screen all run in one process, so plain objects are enough;
 * there is nothing here a DI framework would earn its keep on.
 */
class AppGraph(context: Context) {
    val bus = GuardBus()
    val stats = StatsStore(context)
    val reelInbox = ReelInboxStore(context)
}

val Context.graph: AppGraph get() = (applicationContext as SidedoorApp).graph
