package com.sidedoor.guard.model

/**
 * Everything visible in the watched app's window at one moment.
 *
 * Only nodes that are visible to the user with non-empty bounds belong here -
 * the capture side filters, so rules can treat "present" as "on screen".
 * Instagram keeps back-stack fragments and neighbouring pager pages laid out
 * off screen; counting those would make every screen look like every other.
 */
class ScreenSnapshot(val nodes: List<UiNode>) {

    private val byId: Map<String, List<UiNode>> =
        nodes.filter { it.viewId != null }.groupBy { it.viewId!! }

    fun has(viewId: String): Boolean = byId.containsKey(viewId)

    fun hasAny(viewIds: Collection<String>): Boolean = viewIds.any(::has)

    fun first(viewId: String): UiNode? = byId[viewId]?.firstOrNull()

    fun all(viewId: String): List<UiNode> = byId[viewId].orEmpty()

    /** First node whose text or description equals [label] exactly. */
    fun withLabel(label: String): UiNode? = nodes.firstOrNull { it.text == label || it.description == label }

    fun anyLabelStartsWith(prefix: String): Boolean =
        nodes.any { it.text?.startsWith(prefix) == true || it.description?.startsWith(prefix) == true }

    /** A node counts as selected if it or any node drawn inside it is selected. */
    fun isSelectedWithin(container: UiNode): Boolean =
        container.isSelected || nodes.any { it.isSelected && it.bounds in container.bounds }

    val isEmpty: Boolean get() = nodes.isEmpty()

    companion object {
        val EMPTY = ScreenSnapshot(emptyList())
    }
}
