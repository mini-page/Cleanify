package com.cleanify.domain.treemap

import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * High-performance squarified treemap calculation engine with guaranteed visible floor weighting.
 *
 * Algorithm adapted from Quarry App (Apache 2.0, Tanvir).
 * Prevents tiny files and folders from collapsing into invisible slivers by applying
 * a minimum visible area floor while proportionally distributing remaining space.
 */
object TreemapEngine {

    /**
     * Constructs a hierarchical TreemapNode for a directory up to [maxDepth].
     */
    fun buildTree(directory: File, currentDepth: Int = 0, maxDepth: Int = 3): TreemapNode {
        val files: List<File> = try {
            directory.listFiles()?.filter { !it.name.startsWith(".") } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        val childNodes = mutableListOf<TreemapNode>()
        var totalDirSize = 0L

        for (file in files) {
            if (file.isDirectory) {
                if (currentDepth < maxDepth) {
                    val subNode = buildTree(file, currentDepth + 1, maxDepth)
                    if (subNode.size > 0L) {
                        childNodes.add(subNode)
                        totalDirSize += subNode.size
                    }
                } else {
                    val size = calculateDirectorySize(file)
                    if (size > 0L) {
                        childNodes.add(
                            TreemapNode(
                                path = file.absolutePath,
                                name = file.name,
                                size = size,
                                isDirectory = true
                            )
                        )
                        totalDirSize += size
                    }
                }
            } else {
                val size = file.length()
                if (size > 0L) {
                    childNodes.add(
                        TreemapNode(
                            path = file.absolutePath,
                            name = file.name,
                            size = size,
                            isDirectory = false,
                            extension = file.extension.lowercase()
                        )
                    )
                    totalDirSize += size
                }
            }
        }

        childNodes.sortByDescending { it.size }

        return TreemapNode(
            path = directory.absolutePath,
            name = if (directory.name.isEmpty()) "Storage" else directory.name,
            size = totalDirSize,
            isDirectory = true,
            children = childNodes
        )
    }

    private fun calculateDirectorySize(directory: File): Long {
        var size = 0L
        val stack = ArrayDeque<File>()
        stack.add(directory)
        var count = 0
        while (stack.isNotEmpty() && count < 5000) {
            val current = stack.removeFirst()
            count++
            val children = current.listFiles() ?: continue
            for (child in children) {
                if (child.isDirectory) {
                    stack.add(child)
                } else {
                    size += child.length()
                }
            }
        }
        return size
    }

    /**
     * Calculates squarified bounding boxes for child nodes within [bounds].
     */
    fun layoutSquarified(
        items: List<TreemapNode>,
        bounds: TreemapRect
    ): List<TreemapNode> {
        val nonZeroItems = items.filter { it.size > 0L }
        if (nonZeroItems.isEmpty() || bounds.width <= 0f || bounds.height <= 0f) return emptyList()

        val sortedItems = nonZeroItems.sortedByDescending { it.size }
        val totalArea = (bounds.width * bounds.height).toDouble()
        val n = sortedItems.size

        // Guaranteed visible floor weighting
        val minFractionPerItem = (0.35 / n).coerceAtMost(0.04)
        val guaranteedBaseArea = totalArea * minFractionPerItem
        val totalGuaranteed = guaranteedBaseArea * n
        val remainingArea = (totalArea - totalGuaranteed).coerceAtLeast(0.0)

        val totalRawSize = sortedItems.sumOf { it.size }.coerceAtLeast(1L).toDouble()

        val normalizedAreas = sortedItems.map { item ->
            val proportionalShare = if (totalRawSize > 0) {
                (item.size.toDouble() / totalRawSize) * remainingArea
            } else {
                0.0
            }
            guaranteedBaseArea + proportionalShare
        }

        val result = mutableListOf<TreemapNode>()
        squarify(sortedItems, normalizedAreas, bounds, result)
        return result
    }

    private fun squarify(
        children: List<TreemapNode>,
        areas: List<Double>,
        rect: TreemapRect,
        result: MutableList<TreemapNode>
    ) {
        if (children.isEmpty() || rect.width <= 0f || rect.height <= 0f) return

        var row = mutableListOf<TreemapNode>()
        var rowAreas = mutableListOf<Double>()
        val remainingChildren = children.toMutableList()
        val remainingAreas = areas.toMutableList()
        var currentRect = rect

        while (remainingChildren.isNotEmpty()) {
            val nextChild = remainingChildren.first()
            val nextArea = remainingAreas.first()
            val currentShortSide = min(currentRect.width, currentRect.height).toDouble()

            if (row.isEmpty()) {
                row.add(nextChild)
                rowAreas.add(nextArea)
                remainingChildren.removeAt(0)
                remainingAreas.removeAt(0)
            } else {
                val currentWorst = worstAspectRatio(rowAreas, currentShortSide)
                val newRowAreas = rowAreas + nextArea
                val newWorst = worstAspectRatio(newRowAreas, currentShortSide)

                if (newWorst <= currentWorst) {
                    row.add(nextChild)
                    rowAreas.add(nextArea)
                    remainingChildren.removeAt(0)
                    remainingAreas.removeAt(0)
                } else {
                    currentRect = layoutRow(row, rowAreas, currentRect, result)
                    row = mutableListOf()
                    rowAreas = mutableListOf()
                }
            }
        }

        if (row.isNotEmpty()) {
            layoutRow(row, rowAreas, currentRect, result)
        }
    }

    private fun worstAspectRatio(rowAreas: List<Double>, length: Double): Double {
        if (rowAreas.isEmpty() || length <= 0.0) return Double.MAX_VALUE
        val totalArea = rowAreas.sum()
        if (totalArea <= 0.0) return Double.MAX_VALUE
        val otherSide = totalArea / length

        var worst = 0.0
        for (area in rowAreas) {
            val itemLength = area / otherSide
            val ratio = max(otherSide / itemLength, itemLength / otherSide)
            if (ratio > worst) worst = ratio
        }
        return worst
    }

    private fun layoutRow(
        row: List<TreemapNode>,
        rowAreas: List<Double>,
        rect: TreemapRect,
        result: MutableList<TreemapNode>
    ): TreemapRect {
        val totalArea = rowAreas.sum()
        val isHorizontal = rect.width < rect.height

        if (isHorizontal) {
            val rowHeight = (totalArea / rect.width).toFloat().coerceAtMost(rect.height)
            var currentX = rect.x
            for (i in row.indices) {
                val isLast = (i == row.size - 1)
                val itemWidth = (rowAreas[i] / rowHeight).toFloat()
                val nextX = if (isLast) rect.right else (currentX + itemWidth).coerceAtMost(rect.right)
                val itemRect = TreemapRect(
                    x = currentX,
                    y = rect.y,
                    width = (nextX - currentX).coerceAtLeast(0f),
                    height = rowHeight
                )
                result.add(row[i].copy(bounds = itemRect))
                currentX = nextX
            }
            return TreemapRect(
                x = rect.x,
                y = (rect.y + rowHeight).coerceAtMost(rect.bottom),
                width = rect.width,
                height = (rect.height - rowHeight).coerceAtLeast(0f)
            )
        } else {
            val rowWidth = (totalArea / rect.height).toFloat().coerceAtMost(rect.width)
            var currentY = rect.y
            for (i in row.indices) {
                val isLast = (i == row.size - 1)
                val itemHeight = (rowAreas[i] / rowWidth).toFloat()
                val nextY = if (isLast) rect.bottom else (currentY + itemHeight).coerceAtMost(rect.bottom)
                val itemRect = TreemapRect(
                    x = rect.x,
                    y = currentY,
                    width = rowWidth,
                    height = (nextY - currentY).coerceAtLeast(0f)
                )
                result.add(row[i].copy(bounds = itemRect))
                currentY = nextY
            }
            return TreemapRect(
                x = (rect.x + rowWidth).coerceAtMost(rect.right),
                y = rect.y,
                width = (rect.width - rowWidth).coerceAtLeast(0f),
                height = rect.height
            )
        }
    }
}
