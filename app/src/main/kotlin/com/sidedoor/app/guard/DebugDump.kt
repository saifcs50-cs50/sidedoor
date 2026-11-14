package com.sidedoor.app.guard

import android.content.Context
import android.util.Log
import android.util.Xml
import com.sidedoor.guard.classify.IgIds
import com.sidedoor.guard.classify.Screen
import com.sidedoor.guard.model.ScreenSnapshot
import java.io.File

/**
 * Debug builds only: writes the current Instagram snapshot in the same XML
 * shape as `uiautomator dump`, so a screen the phone can see but uiautomator
 * cannot (anything playing video) becomes a test fixture.
 *
 *   adb shell am broadcast -a com.sidedoor.app.DUMP
 *   adb shell run-as com.sidedoor.app ls files/dumps
 */
object DebugDump {

    const val ACTION = "com.sidedoor.app.DUMP"
    private const val TAG = "SidedoorDump"

    fun write(context: Context, snapshot: ScreenSnapshot, screen: Screen): File? = runCatching {
        val dir = File(context.filesDir, "dumps").apply { mkdirs() }
        val file = File(dir, "${System.currentTimeMillis()}-${screen.name.lowercase()}.xml")
        file.outputStream().use { out ->
            val xml = Xml.newSerializer()
            xml.setOutput(out, "UTF-8")
            xml.startDocument("UTF-8", true)
            xml.startTag(null, "hierarchy")
            snapshot.nodes.forEach { n ->
                xml.startTag(null, "node")
                xml.attribute(null, "resource-id", n.viewId?.let { "${IgIds.PACKAGE}:id/$it" }.orEmpty())
                xml.attribute(null, "text", n.text.orEmpty())
                xml.attribute(null, "content-desc", n.description.orEmpty())
                xml.attribute(null, "selected", n.isSelected.toString())
                xml.attribute(null, "clickable", n.isClickable.toString())
                with(n.bounds) { xml.attribute(null, "bounds", "[$left,$top][$right,$bottom]") }
                xml.endTag(null, "node")
            }
            xml.endTag(null, "hierarchy")
            xml.endDocument()
        }
        Log.i(TAG, "Wrote ${file.name} (${snapshot.nodes.size} nodes)")
        file
    }.onFailure { Log.e(TAG, "Dump failed", it) }.getOrNull()
}
