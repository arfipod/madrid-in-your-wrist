package com.arfipod.madridinyourwrist.transit

import java.text.Normalizer
import java.util.Locale

object MadridTransitSearch {
    private val normalizedFieldsCache = mutableMapOf<String, NormalizedSearchFields>()

    fun search(
        options: List<MadridTransitOption>,
        query: String,
        limit: Int = 8,
    ): List<MadridTransitOption> {
        val terms = query.normalizedSearchText()
            .split(" ")
            .filter { term -> term.isNotBlank() }
        if (terms.isEmpty()) return options

        return options
            .mapNotNull { option ->
                val score = option.searchScore(terms)
                if (score > 0) option to score else null
            }
            .sortedWith(
                compareByDescending<Pair<MadridTransitOption, Int>> { (_, score) -> score }
                    .thenBy { (option, _) -> option.label }
            )
            .map { (option, _) -> option }
            .take(limit.coerceAtLeast(1))
    }

    internal fun String.normalizedSearchText(): String {
        val withoutDiacritics = Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
        return withoutDiacritics
            .lowercase(Locale.ROOT)
            .replace("[^a-z0-9]+".toRegex(), " ")
            .trim()
    }

    private fun MadridTransitOption.searchScore(terms: List<String>): Int {
        val normalizedFields = normalizedSearchFields()
        if (terms.any { term -> term !in normalizedFields.haystack }) return 0

        val termScore = terms.fold(0) { score, term ->
            score + when {
                normalizedFields.fields.any { field -> field == term } -> 40
                normalizedFields.fields.any { field -> field.startsWith(term) } -> 25
                normalizedFields.fields.any { field -> term in field } -> 15
                else -> 5
            }
        }
        val labelScore = normalizedFields.fields.firstOrNull()?.let { label ->
            if (terms.joinToString(" ") in label) 30 else 0
        } ?: 0
        val primaryScore = terms.fold(0) { score, term ->
            score + when {
                normalizedFields.primaryFields.any { field -> field == term } -> 35
                normalizedFields.primaryFields.any { field -> field.startsWith(term) } -> 25
                normalizedFields.primaryFields.any { field -> term in field } -> 15
                else -> 0
            }
        }
        return termScore + labelScore + primaryScore
    }

    private fun MadridTransitOption.normalizedSearchFields(): NormalizedSearchFields = synchronized(normalizedFieldsCache) {
        normalizedFieldsCache.getOrPut(id) {
            val fields = searchFields().map { field -> field.normalizedSearchText() }
            val primaryFields = primarySearchFields().map { field -> field.normalizedSearchText() }
            NormalizedSearchFields(
                fields = fields,
                primaryFields = primaryFields,
                haystack = fields.joinToString(" "),
            )
        }
    }

    private fun MadridTransitOption.primarySearchFields(): List<String> {
        return listOfNotNull(
            label,
            detail,
            metroTarget?.stopNameQuery,
            metroTarget?.routeNameQuery,
            metroTarget?.destinationQuery,
            busTarget?.stopId,
            busTarget?.lineId,
            busTarget?.destination,
        )
    }

    private fun MadridTransitOption.searchFields(): List<String> {
        val metroFields = metroTarget?.let { target ->
            listOfNotNull(
                target.label,
                target.stopNameQuery,
                target.routeNameQuery,
                target.destinationQuery,
                target.routeNameQuery?.let { routeName -> "L$routeName" },
                target.routeNameQuery?.let { routeName -> "Linea $routeName" },
                target.routeNameQuery?.let { routeName -> "Línea $routeName" },
                "Estacion ${target.stopNameQuery}",
                "Estación ${target.stopNameQuery}",
            )
        }.orEmpty()
        val busFields = busTarget?.let { target ->
            listOfNotNull(
                target.label,
                target.stopId,
                target.lineId,
                target.destination,
                "Parada ${target.stopId}",
                "Stop ${target.stopId}",
                target.lineId?.let { lineId -> "Linea $lineId" },
                target.lineId?.let { lineId -> "Línea $lineId" },
            )
        }.orEmpty()

        return listOf(label, detail, kind.label, source.label) + searchAliases + metroFields + busFields
    }

    private data class NormalizedSearchFields(
        val fields: List<String>,
        val primaryFields: List<String>,
        val haystack: String,
    )
}
