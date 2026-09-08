package com.screeniq.ocr.clustering

import android.graphics.Rect
import com.screeniq.core.model.TextBlock

/**
 * Geometric clustering of visual text blocks to identify cohesive logical
 * sections (e.g., event headers, price blocks, address clusters).
 */
class SpatialTextClusterer {

    /**
     * Clusters lines/blocks that are vertically aligned or horizontally adjacent.
     * Default vertical tolerance is 35 pixels; horizontal overlap tolerance is 20 pixels.
     */
    fun clusterBlocks(
        blocks: List<TextBlock>,
        verticalTolerancePx: Int = 40,
        horizontalTolerancePx: Int = 100
    ): List<TextBlock> {
        if (blocks.size <= 1) return blocks

        // Filter out blocks without bounding boxes or empty text
        val validBlocks = blocks.filter { it.text.isNotBlank() }
        if (validBlocks.isEmpty()) return emptyList()

        val sorted = validBlocks.sortedWith(
            compareBy<TextBlock> { it.boundingBox?.top ?: 0 }
                .thenBy { it.boundingBox?.left ?: 0 }
        )

        val clusters = mutableListOf<MutableList<TextBlock>>()
        var currentCluster = mutableListOf<TextBlock>()

        for (block in sorted) {
            if (currentCluster.isEmpty()) {
                currentCluster.add(block)
                continue
            }

            val lastBlock = currentCluster.last()
            val lastBox = lastBlock.boundingBox
            val currentBox = block.boundingBox

            val shouldMerge = if (lastBox != null && currentBox != null) {
                val verticalGap = currentBox.top - lastBox.bottom
                val horizontalOverlap = maxOf(0, minOf(lastBox.right, currentBox.right) - maxOf(lastBox.left, currentBox.left))
                val horizontalGap = currentBox.left - lastBox.right

                // Vertically stacked with reasonable gap and horizontal alignment
                (verticalGap in -10..verticalTolerancePx && (horizontalOverlap > 0 || Math.abs(currentBox.left - lastBox.left) < horizontalTolerancePx)) ||
                // Or horizontally adjacent on virtually the same line
                (Math.abs(currentBox.top - lastBox.top) <= 15 && horizontalGap in 0..horizontalTolerancePx)
            } else {
                false
            }

            if (shouldMerge) {
                currentCluster.add(block)
            } else {
                clusters.add(currentCluster)
                currentCluster = mutableListOf(block)
            }
        }

        if (currentCluster.isNotEmpty()) {
            clusters.add(currentCluster)
        }

        return clusters.map { mergeCluster(it) }
    }

    private fun mergeCluster(cluster: List<TextBlock>): TextBlock {
        if (cluster.size == 1) return cluster.first()

        val combinedText = cluster.joinToString("\n") { it.text }
        val avgConfidence = cluster.map { it.confidence }.average().toFloat()

        val boxes = cluster.mapNotNull { it.boundingBox }
        val mergedBox = if (boxes.isNotEmpty()) {
            Rect(
                boxes.minOf { it.left },
                boxes.minOf { it.top },
                boxes.maxOf { it.right },
                boxes.maxOf { it.bottom }
            )
        } else null

        return TextBlock(
            text = combinedText,
            boundingBox = mergedBox,
            confidence = avgConfidence
        )
    }
}
