package com.arfipod.madridinyourwrist.transit

enum class MadridTransitRefreshSource {
    EMPTY_SELECTION,
    AUTO_CACHE,
    DISTANCE_SKIPPED_CACHE,
    DISTANCE_SKIPPED_EMPTY,
    ONLINE_LIVE,
    ONLINE_CACHE_FALLBACK,
    ONLINE_EMPTY,
    OFFLINE_CACHE,
    OFFLINE_EMPTY,
}

data class MadridTransitRefreshDecision(
    val source: MadridTransitRefreshSource,
    val snapshot: MadridTransitSnapshot?,
    val snapshotToSave: MadridTransitSnapshot? = null,
)

object MadridTransitRefreshPolicy {
    const val AUTO_REFRESH_CACHE_MAX_AGE_MILLIS = 3 * 60 * 1000L

    fun clearSelection(
        previous: MadridTransitSnapshot?,
        place: MadridTransitPlace,
        updatedAt: String,
        storedAtEpochMillis: Long = System.currentTimeMillis(),
    ): MadridTransitRefreshDecision {
        val mergedSnapshot = MadridTransitSnapshots.mergePlace(
            previous = previous,
            place = place,
            updatedAt = updatedAt,
            replacementItems = emptyList(),
            storedAtEpochMillis = storedAtEpochMillis,
        )
        return MadridTransitRefreshDecision(
            source = MadridTransitRefreshSource.EMPTY_SELECTION,
            snapshot = mergedSnapshot.takeUnless { snapshot -> snapshot.items.isEmpty() },
            snapshotToSave = mergedSnapshot,
        )
    }

    fun automaticCache(
        previous: MadridTransitSnapshot?,
        place: MadridTransitPlace,
        optionIds: Set<String>,
        nowEpochMillis: Long,
        maxAgeMillis: Long = AUTO_REFRESH_CACHE_MAX_AGE_MILLIS,
    ): MadridTransitRefreshDecision? {
        val snapshot = previous ?: return null
        if (!snapshot.isFreshAt(nowEpochMillis = nowEpochMillis, maxAgeMillis = maxAgeMillis)) return null
        if (!snapshot.hasCacheForAll(place = place, optionIds = optionIds)) return null

        return MadridTransitRefreshDecision(
            source = MadridTransitRefreshSource.AUTO_CACHE,
            snapshot = snapshot,
        )
    }

    fun distanceSkipped(
        previous: MadridTransitSnapshot?,
        place: MadridTransitPlace,
        optionIds: Set<String>,
    ): MadridTransitRefreshDecision {
        return MadridTransitRefreshDecision(
            source = if (previous.hasCacheFor(place, optionIds)) {
                MadridTransitRefreshSource.DISTANCE_SKIPPED_CACHE
            } else {
                MadridTransitRefreshSource.DISTANCE_SKIPPED_EMPTY
            },
            snapshot = previous,
        )
    }

    fun offline(
        previous: MadridTransitSnapshot?,
        place: MadridTransitPlace,
        optionIds: Set<String>,
    ): MadridTransitRefreshDecision {
        return MadridTransitRefreshDecision(
            source = if (previous.hasCacheFor(place, optionIds)) {
                MadridTransitRefreshSource.OFFLINE_CACHE
            } else {
                MadridTransitRefreshSource.OFFLINE_EMPTY
            },
            snapshot = previous,
        )
    }

    fun onlineSuccess(
        previous: MadridTransitSnapshot?,
        place: MadridTransitPlace,
        optionIds: Set<String>,
        updatedAt: String,
        storedAtEpochMillis: Long = System.currentTimeMillis(),
        results: List<MadridTransitLoadResult>,
    ): MadridTransitRefreshDecision {
        val refreshedOptionIds = results.mapNotNull { result -> result.refreshedOptionId() }.toSet()
        if (refreshedOptionIds.isEmpty()) {
            return MadridTransitRefreshDecision(
                source = if (previous.hasCacheFor(place, optionIds)) {
                    MadridTransitRefreshSource.ONLINE_CACHE_FALLBACK
                } else {
                    MadridTransitRefreshSource.ONLINE_EMPTY
                },
                snapshot = previous,
            )
        }

        val selectedSnapshot = MadridTransitSnapshots.fromResults(
            results = results,
            updatedAt = updatedAt,
            storedAtEpochMillis = storedAtEpochMillis,
        )
        val mergedSnapshot = MadridTransitSnapshots.mergeRefresh(
            previous = previous,
            place = place,
            updatedAt = updatedAt,
            refreshedOptionIds = refreshedOptionIds,
            replacementItems = selectedSnapshot.items,
            storedAtEpochMillis = storedAtEpochMillis,
        )
        return MadridTransitRefreshDecision(
            source = MadridTransitRefreshSource.ONLINE_LIVE,
            snapshot = mergedSnapshot.takeUnless { snapshot -> snapshot.items.isEmpty() },
            snapshotToSave = mergedSnapshot,
        )
    }

    fun onlineFailure(
        previous: MadridTransitSnapshot?,
        place: MadridTransitPlace,
        optionIds: Set<String>,
    ): MadridTransitRefreshDecision {
        return MadridTransitRefreshDecision(
            source = if (previous.hasCacheFor(place, optionIds)) {
                MadridTransitRefreshSource.ONLINE_CACHE_FALLBACK
            } else {
                MadridTransitRefreshSource.ONLINE_EMPTY
            },
            snapshot = previous,
        )
    }

    private fun MadridTransitSnapshot?.hasCacheFor(
        place: MadridTransitPlace,
        optionIds: Set<String>,
    ): Boolean {
        return this?.items.orEmpty().any { item ->
            item.place == place && item.optionId in optionIds
        }
    }

    private fun MadridTransitSnapshot.hasCacheForAll(
        place: MadridTransitPlace,
        optionIds: Set<String>,
    ): Boolean {
        if (optionIds.isEmpty()) return false
        val cachedOptionIds = items
            .asSequence()
            .filter { item -> item.place == place }
            .map { item -> item.optionId }
            .toSet()
        return optionIds.all { optionId -> optionId in cachedOptionIds }
    }

    private fun MadridTransitSnapshot.isFreshAt(
        nowEpochMillis: Long,
        maxAgeMillis: Long,
    ): Boolean {
        val ageMillis = nowEpochMillis - storedAtEpochMillis
        return storedAtEpochMillis > 0L && ageMillis in 0..maxAgeMillis
    }
}
