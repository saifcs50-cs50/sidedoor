package com.sidedoor.guard.support

import com.sidedoor.guard.model.Bounds
import com.sidedoor.guard.model.ScreenSnapshot
import com.sidedoor.guard.model.UiNode
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Loads a `uiautomator dump` XML captured from a real device (personal text
 * scrubbed) into the same [ScreenSnapshot] the service builds at runtime.
 * uiautomator only dumps nodes that are visible to the user, which is exactly
 * the filter the service applies.
 */
object Fixtures {

    private const val ID_PREFIX = "com.instagram.android:id/"
    private val BOUNDS = Regex("""\[(-?\d+),(-?\d+)]\[(-?\d+),(-?\d+)]""")

    fun load(name: String): ScreenSnapshot {
        val stream = checkNotNull(javaClass.classLoader?.getResourceAsStream("fixtures/$name")) {
            "Missing fixture $name"
        }
        val doc = stream.use { DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it) }
        val elements = doc.getElementsByTagName("node")
        val nodes = (0 until elements.length)
            .map { elements.item(it) as Element }
            .map(::toNode)
            .filterNot { it.bounds.isEmpty }
        return ScreenSnapshot(nodes)
    }

    private fun toNode(e: Element): UiNode = UiNode(
        viewId = e.getAttribute("resource-id").removePrefix(ID_PREFIX).ifBlank { null },
        text = e.getAttribute("text").ifBlank { null },
        description = e.getAttribute("content-desc").ifBlank { null },
        bounds = parseBounds(e.getAttribute("bounds")),
        isSelected = e.getAttribute("selected") == "true",
        isClickable = e.getAttribute("clickable") == "true",
    )

    private fun parseBounds(raw: String): Bounds {
        val m = BOUNDS.matchEntire(raw) ?: return Bounds.EMPTY
        val (l, t, r, b) = m.destructured
        return Bounds(l.toInt(), t.toInt(), r.toInt(), b.toInt())
    }
}

/** Builder for screens a uiautomator dump cannot capture (it never idles while video plays). */
class SnapshotBuilder {
    private val nodes = mutableListOf<UiNode>()

    fun id(viewId: String, text: String? = null, desc: String? = null, bounds: Bounds = FULL, selected: Boolean = false) {
        nodes += UiNode(viewId = viewId, text = text, description = desc, bounds = bounds, isSelected = selected)
    }

    fun label(text: String, bounds: Bounds = FULL) {
        nodes += UiNode(text = text, bounds = bounds)
    }

    /** A node known only by its content description, as Instagram's Bloks rows are. */
    fun desc(description: String, bounds: Bounds = FULL) {
        nodes += UiNode(description = description, bounds = bounds)
    }

    /** Instagram's bottom bar with [selected] highlighted the way the test phone reports it. */
    fun tabBar(selected: String) {
        id("tab_bar", bounds = Bounds(0, 1490, 720, 1572))
        TABS.forEachIndexed { i, tab ->
            val frame = Bounds(i * 144, 1490, (i + 1) * 144, 1572)
            id(tab, bounds = frame)
            id("tab_icon", bounds = Bounds(frame.left + 49, 1508, frame.left + 94, 1553), selected = tab == selected)
        }
    }

    fun build() = ScreenSnapshot(nodes.toList())

    companion object {
        val FULL = Bounds(0, 64, 720, 1572)
        val TABS = listOf("feed_tab", "clips_tab", "direct_tab", "search_tab", "profile_tab")
    }
}

fun snapshot(block: SnapshotBuilder.() -> Unit): ScreenSnapshot = SnapshotBuilder().apply(block).build()
