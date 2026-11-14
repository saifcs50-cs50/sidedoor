package com.sidedoor.guard.model

/** Screen-space rectangle, in pixels, as reported by the accessibility tree. */
data class Bounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {

    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val isEmpty: Boolean get() = width <= 0 || height <= 0
    val centerX: Int get() = left + width / 2
    val centerY: Int get() = top + height / 2

    operator fun contains(other: Bounds): Boolean =
        other.left >= left && other.top >= top && other.right <= right && other.bottom <= bottom

    companion object {
        val EMPTY = Bounds(0, 0, 0, 0)
    }
}
