package com.yash.privategallery.domain.usecase

import com.yash.privategallery.domain.model.MediaItem
import javax.inject.Inject

/** A group of likely-duplicate items (Section 28: "3 similar photos"). */
data class DuplicateGroup(
    val items: List<MediaItem>,
    val reason: DuplicateReason
)

enum class DuplicateReason {
    EXACT_HASH_MATCH,
    SAME_SIZE_AND_DIMENSIONS,
    PERCEPTUAL_SIMILARITY
}

/**
 * Groups likely-duplicate images. Runs in three passes, cheapest/most-certain
 * first, so expensive perceptual hashing only runs on items that didn't already
 * match exactly (Section 28: "File hash, Size, Dimensions, Perceptual similarity
 * where practical"). Never deletes anything itself — purely produces groups for
 * the UI to present with an explicit confirm step (Section 28: "Never
 * automatically delete duplicates without confirmation").
 *
 * [fileHashProvider] and [perceptualHashProvider] are injected rather than
 * computed here because hashing requires reading file bytes — an I/O concern
 * that belongs to the data layer (data/media), not domain logic. This use case
 * only orchestrates the grouping strategy.
 */
class DetectDuplicatesUseCase @Inject constructor() {

    suspend operator fun invoke(
        items: List<MediaItem>,
        fileHashProvider: suspend (MediaItem) -> String?,
        perceptualHashProvider: suspend (MediaItem) -> Long?,
        hammingDistanceThreshold: Int = 5
    ): List<DuplicateGroup> {
        val results = mutableListOf<DuplicateGroup>()
        val consumed = mutableSetOf<Long>()

        // Pass 1: exact file hash match (byte-identical files, e.g. re-saved copies).
        val hashGroups = mutableMapOf<String, MutableList<MediaItem>>()
        for (item in items) {
            val hash = fileHashProvider(item) ?: continue
            hashGroups.getOrPut(hash) { mutableListOf() }.add(item)
        }
        hashGroups.values.filter { it.size > 1 }.forEach { group ->
            results.add(DuplicateGroup(group, DuplicateReason.EXACT_HASH_MATCH))
            consumed.addAll(group.map { it.id })
        }

        // Pass 2: same size + same dimensions among items not already grouped.
        val remaining = items.filter { it.id !in consumed }
        val dimensionGroups = remaining
            .groupBy { Triple(it.sizeBytes, it.width, it.height) }
            .values
            .filter { it.size > 1 }
        dimensionGroups.forEach { group ->
            results.add(DuplicateGroup(group, DuplicateReason.SAME_SIZE_AND_DIMENSIONS))
            consumed.addAll(group.map { it.id })
        }

        // Pass 3: perceptual similarity (e.g. average-hash / dHash) for near-duplicates
        // that differ in compression or minor edits — only run on what's left, since
        // this is the most expensive pass.
        val stillRemaining = items.filter { it.id !in consumed }
        val perceptualHashes = stillRemaining.mapNotNull { item ->
            perceptualHashProvider(item)?.let { item to it }
        }
        val usedInPerceptualPass = mutableSetOf<Long>()
        for (i in perceptualHashes.indices) {
            val (itemA, hashA) = perceptualHashes[i]
            if (itemA.id in usedInPerceptualPass) continue
            val group = mutableListOf(itemA)
            for (j in i + 1 until perceptualHashes.size) {
                val (itemB, hashB) = perceptualHashes[j]
                if (itemB.id in usedInPerceptualPass) continue
                if (hammingDistance(hashA, hashB) <= hammingDistanceThreshold) {
                    group.add(itemB)
                }
            }
            if (group.size > 1) {
                results.add(DuplicateGroup(group, DuplicateReason.PERCEPTUAL_SIMILARITY))
                usedInPerceptualPass.addAll(group.map { it.id })
            }
        }

        return results
    }

    private fun hammingDistance(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)
}
