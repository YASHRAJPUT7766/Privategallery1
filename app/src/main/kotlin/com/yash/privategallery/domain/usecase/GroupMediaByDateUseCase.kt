package com.yash.privategallery.domain.usecase

import com.yash.privategallery.domain.model.MediaDateGroup
import com.yash.privategallery.domain.model.MediaItem
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

/**
 * Buckets a flat, already-sorted (newest first) list of [MediaItem] into labeled
 * date groups for the home timeline (Section 6, 7).
 *
 * Uses each item's best-available timestamp — see [MediaItem.dateTaken] — which
 * the repository layer is responsible for resolving from MediaStore's
 * DATE_TAKEN, falling back to DATE_ADDED, then DATE_MODIFIED (Section 7:
 * "Use the best available timestamp"). This use case only does the grouping,
 * not the fallback resolution.
 */
class GroupMediaByDateUseCase @Inject constructor() {

    operator fun invoke(
        items: List<MediaItem>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        now: Instant = Instant.now()
    ): List<MediaDateGroup> {
        if (items.isEmpty()) return emptyList()

        val today = now.atZone(zoneId).toLocalDate()
        val yesterday = today.minusDays(1)
        val startOfWeek = today.minusDays(6) // rolling 7-day "This Week" window
        val startOfMonth = today.withDayOfMonth(1)

        val monthDayFormatter = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

        return items
            .groupBy { item ->
                val itemDate = Instant.ofEpochMilli(item.dateTaken).atZone(zoneId).toLocalDate()
                when {
                    itemDate.isEqual(today) -> "Today"
                    itemDate.isEqual(yesterday) -> "Yesterday"
                    !itemDate.isBefore(startOfWeek) -> "This Week"
                    !itemDate.isBefore(startOfMonth) -> "This Month"
                    else -> monthDayFormatter.format(java.util.Date(item.dateTaken))
                }
            }
            .map { (label, groupItems) -> MediaDateGroup(label, groupItems) }
            .sortedByDescending { group ->
                // Preserve overall newest-first ordering of the groups themselves,
                // using the newest item's timestamp within each group as the sort key.
                group.items.maxOf { it.dateTaken }
            }
    }
}

/** Days between two epoch-millis timestamps in the given zone — small helper used by callers
 *  that need e.g. "days remaining" style calculations (Section 27) without duplicating zone logic. */
fun daysBetween(fromEpochMillis: Long, toEpochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long {
    val from = Instant.ofEpochMilli(fromEpochMillis).atZone(zoneId).toLocalDate()
    val to = Instant.ofEpochMilli(toEpochMillis).atZone(zoneId).toLocalDate()
    return ChronoUnit.DAYS.between(from, to)
}
