package com.arfipod.madridinyourwrist.transit

import java.util.Locale
import java.util.PriorityQueue

object MadridTransitSearch {
    private const val MAX_QUERY_CHARS = 48

    fun search(
        options: List<MadridTransitOption>,
        query: String,
        limit: Int = 8,
    ): List<MadridTransitOption> {
        val terms = query.normalizedSearchTerms()
        if (terms.isEmpty()) return options
        val phrase = terms.joinToString(" ")

        val resultLimit = limit.coerceAtLeast(1)
        val bestMatches = PriorityQueue<SearchMatch>(
            compareBy<SearchMatch> { match -> match.score }
                .thenByDescending { match -> match.option.label }
                .thenByDescending { match -> match.option.id }
        )

        options.forEach { option ->
            val score = option.searchScore(terms, phrase)
            if (score <= 0) return@forEach

            val match = SearchMatch(option = option, score = score)
            if (bestMatches.size < resultLimit) {
                bestMatches += match
            } else if (BETTER_MATCH_FIRST.compare(match, bestMatches.peek()) < 0) {
                bestMatches.poll()
                bestMatches += match
            }
        }

        return bestMatches.toList()
            .sortedWith(BETTER_MATCH_FIRST)
            .map { match -> match.option }
    }

    fun isQueryReady(query: String): Boolean {
        val normalized = query.normalizedSearchText()
        return normalized.length >= 2 || normalized.any { char -> char.isDigit() }
    }

    internal fun String.normalizedSearchText(): String {
        if (isBlank()) return ""

        val clipped = if (length > MAX_QUERY_CHARS) take(MAX_QUERY_CHARS) else this
        val builder = StringBuilder(clipped.length)
        var lastWasSpace = true
        clipped.forEach { rawChar ->
            val char = rawChar.normalizedSearchChar()
            if (char != null) {
                builder.append(char)
                lastWasSpace = false
            } else if (!lastWasSpace) {
                builder.append(' ')
                lastWasSpace = true
            }
        }
        if (builder.isNotEmpty() && builder.last() == ' ') {
            builder.setLength(builder.length - 1)
        }
        return builder.toString()
    }

    private fun String.normalizedSearchTerms(): List<String> {
        return normalizedSearchText()
            .split(' ')
            .filter { term -> term.isNotBlank() }
    }

    private fun MadridTransitOption.searchScore(terms: List<String>, phrase: String): Int {
        val fields = searchFields()
            .asSequence()
            .map { field -> field.normalizedSearchText() }
            .filter { field -> field.isNotBlank() }
            .distinct()
            .toList()
        val primaryFields = primarySearchFields()
            .asSequence()
            .map { field -> field.normalizedSearchText() }
            .filter { field -> field.isNotBlank() }
            .distinct()
            .toList()

        val termScore = terms.fold(0) { score, term ->
            val fieldScore = fields.bestFieldScore(term)
            if (fieldScore == 0) return 0
            score + fieldScore
        }
        val primaryScore = terms.fold(0) { score, term ->
            score + primaryFields.bestFieldScore(term)
        }
        val phraseScore = if (terms.size > 1 && phrase in fields.firstOrNull().orEmpty()) {
            30
        } else {
            0
        }
        return termScore + primaryScore + phraseScore
    }

    private fun List<String>.bestFieldScore(term: String): Int = when {
        any { field -> field == term } -> 40
        any { field -> field.startsWith(term) } -> 25
        any { field -> term in field } -> 15
        else -> 0
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

    private fun Char.normalizedSearchChar(): Char? = when (this) {
        in 'a'..'z', in '0'..'9' -> this
        in 'A'..'Z' -> lowercaseChar()
        'á', 'à', 'ä', 'â', 'Á', 'À', 'Ä', 'Â' -> 'a'
        'é', 'è', 'ë', 'ê', 'É', 'È', 'Ë', 'Ê' -> 'e'
        'í', 'ì', 'ï', 'î', 'Í', 'Ì', 'Ï', 'Î' -> 'i'
        'ó', 'ò', 'ö', 'ô', 'Ó', 'Ò', 'Ö', 'Ô' -> 'o'
        'ú', 'ù', 'ü', 'û', 'Ú', 'Ù', 'Ü', 'Û' -> 'u'
        'ñ', 'Ñ' -> 'n'
        'ç', 'Ç' -> 'c'
        else -> lowercaseChar()
            .takeIf { char -> char in 'a'..'z' || char in '0'..'9' }
    }

    private data class SearchMatch(
        val option: MadridTransitOption,
        val score: Int,
    )

    private val BETTER_MATCH_FIRST = compareByDescending<SearchMatch> { match -> match.score }
        .thenBy { match -> match.option.label.lowercase(Locale.ROOT) }
        .thenBy { match -> match.option.id }
}
