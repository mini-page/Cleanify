package com.cleanify.domain.usecase

import com.cleanify.data.model.MediaItem

/**
 * Intelligent arbiter for resolving duplicate media groups into a keeper and redundant copies.
 *
 * Algorithm adapted from SD Maid SE's DuplicatesArbiter (GPLv3, Darken).
 * Evaluates duplicate litigants using customizable, prioritized criteria:
 * 1. Preferred location/bucket (keep primary Camera/DCIM over WhatsApp/Download).
 * 2. Original timestamp (keep oldest original vs newest).
 * 3. File name purity (penalize duplicate naming patterns like "(1)", "_copy", etc.).
 */
class DuplicatesArbiter(
    val strategy: Strategy = Strategy.DEFAULT
) {
    enum class DatePreference {
        KEEP_OLDEST,
        KEEP_NEWEST
    }

    data class Strategy(
        val preferredBuckets: List<String> = listOf("Camera", "DCIM", "Pictures", "Photos"),
        val deprioritizedBuckets: List<String> = listOf("Download", "Downloads", "WhatsApp", "Telegram", "Messenger"),
        val datePreference: DatePreference = DatePreference.KEEP_OLDEST,
        val preferCleanNames: Boolean = true
    ) {
        companion object {
            val DEFAULT = Strategy()
        }
    }

    data class ArbitrationResult(
        val keeper: MediaItem,
        val redundantCopies: List<MediaItem>
    )

    fun arbitrate(group: DuplicateGroup): ArbitrationResult {
        return arbitrateItems(group.items)
    }

    fun arbitrateItems(items: List<MediaItem>): ArbitrationResult {
        if (items.isEmpty()) {
            throw IllegalArgumentException("Cannot arbitrate empty item list")
        }
        if (items.size == 1) {
            return ArbitrationResult(keeper = items.first(), redundantCopies = emptyList())
        }

        // Score each candidate: higher score = better keeper
        val scored = items.map { item ->
            item to calculateKeeperScore(item)
        }

        // Sort by score descending (highest score first)
        val sorted = scored.sortedWith(
            compareByDescending<Pair<MediaItem, Int>> { it.second }
                .thenBy {
                    val timestamp = if (it.first.dateModified > 0) it.first.dateModified else it.first.dateAdded
                    if (strategy.datePreference == DatePreference.KEEP_OLDEST) timestamp else -timestamp
                }
        ).map { it.first }

        val keeper = sorted.first()
        val redundant = sorted.drop(1)
        return ArbitrationResult(keeper = keeper, redundantCopies = redundant)
    }

    private fun calculateKeeperScore(item: MediaItem): Int {
        var score = 100
        val bucket = item.bucketName.lowercase()
        val name = item.displayName.lowercase()

        // 1. Preferred locations
        for (pref in strategy.preferredBuckets) {
            if (bucket.contains(pref.lowercase())) {
                score += 50
                break
            }
        }

        // 2. Deprioritized locations
        for (deprec in strategy.deprioritizedBuckets) {
            if (bucket.contains(deprec.lowercase())) {
                score -= 40
                break
            }
        }

        // 3. Name purity (penalize obvious duplicate suffixes)
        if (strategy.preferCleanNames) {
            if (name.contains("(1)") || name.contains("(2)") || name.contains("copy") || name.contains("_1")) {
                score -= 30
            }
        }

        return score
    }
}
