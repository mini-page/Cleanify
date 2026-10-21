package com.cleanify.domain.treemap

/**
 * Node representation for treemap hierarchy and layout.
 * Adapted from Quarry App (Apache 2.0, Tanvir).
 */
data class TreemapNode(
    val path: String,
    val name: String,
    val size: Long,
    val isDirectory: Boolean,
    val extension: String = "",
    val bounds: TreemapRect = TreemapRect(),
    val children: List<TreemapNode> = emptyList()
) {
    val isLeaf: Boolean get() = !isDirectory || children.isEmpty()
}
