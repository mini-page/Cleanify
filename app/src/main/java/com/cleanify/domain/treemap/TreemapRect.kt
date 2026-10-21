package com.cleanify.domain.treemap

/**
 * 2D bounding rectangle for squarified treemap layout.
 * Adapted from Quarry App (Apache 2.0, Tanvir).
 */
data class TreemapRect(
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f
) {
    val right: Float get() = x + width
    val bottom: Float get() = y + height
    val area: Float get() = (width * height).coerceAtLeast(0f)

    fun contains(px: Float, py: Float): Boolean {
        return px in x..right && py in y..bottom
    }
}
