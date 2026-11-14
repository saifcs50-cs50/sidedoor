package com.sidedoor.guard.model

/**
 * One on-screen node of the watched app, reduced to what the guard reads.
 *
 * [viewId] is the short resource name ("clips_tab"), with the package prefix
 * stripped, or null for nodes Instagram renders without an id (Bloks/Compose).
 */
data class UiNode(
    val viewId: String? = null,
    val text: String? = null,
    val description: String? = null,
    val bounds: Bounds = Bounds.EMPTY,
    val isSelected: Boolean = false,
    val isClickable: Boolean = false,
) {
    /** Text or content description, whichever the node carries. */
    val label: String? get() = text?.takeIf { it.isNotBlank() } ?: description?.takeIf { it.isNotBlank() }
}
