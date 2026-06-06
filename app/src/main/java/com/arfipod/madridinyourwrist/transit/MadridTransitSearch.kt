package com.arfipod.madridinyourwrist.transit

import java.util.IdentityHashMap
import java.util.LinkedHashSet
import java.util.Locale
import java.util.PriorityQueue

object MadridTransitSearch {
    private const val MAX_QUERY_CHARS = 48
    private const val FIELD_SEPARATOR: Char = '\u001F'
    private val COMMON_SEARCH_STOP_WORDS = setOf("de", "del", "la", "las", "el", "los", "y")
    private val indexCache = IdentityHashMap<List<MadridTransitOption>, SearchIndex>()

    fun search(
        options: List<MadridTransitOption>,
        query: String,
        limit: Int = 8,
    ): List<MadridTransitOption> {
        val terms = query.normalizedSearchTerms().map { term -> SearchTerm(term) }
        if (terms.isEmpty()) return options
        val phrase = terms.joinToString(" ") { term -> term.value }
        val searchIndex = indexFor(options)

        val resultLimit = limit.coerceAtLeast(1)
        val bestMatches = PriorityQueue<SearchMatch>(
            compareBy<SearchMatch> { match -> match.score }
                .thenByDescending { match -> match.option.label }
                .thenByDescending { match -> match.option.id }
        )

        searchIndex.documents.forEach { document ->
            val score = document.searchScore(terms, phrase)
            if (score <= 0) return@forEach

            val match = SearchMatch(option = document.option, score = score)
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

    fun prepare(options: List<MadridTransitOption>) {
        if (options.isNotEmpty()) {
            indexFor(options)
        }
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

    internal fun String.normalizedSearchTerms(): List<String> {
        val terms = normalizedSearchText()
            .split(' ')
            .filter { term -> term.isNotBlank() }
        if (terms.size <= 1) return terms

        return terms
            .filterNot { term -> term in COMMON_SEARCH_STOP_WORDS }
            .ifEmpty { terms }
    }

    private fun indexFor(options: List<MadridTransitOption>): SearchIndex {
        synchronized(indexCache) {
            indexCache[options]?.let { index -> return index }
            val index = SearchIndex(
                documents = options.map { option -> option.toSearchDocument() },
            )
            indexCache[options] = index
            return index
        }
    }

    private fun MadridTransitOption.toSearchDocument(): SearchDocument {
        return SearchDocument(
            option = this,
            fieldsBlob = normalizedFieldBlob(searchFields()),
            primaryFieldsBlob = normalizedFieldBlob(primarySearchFields()),
            phraseField = label.normalizedSearchText(),
        )
    }

    private fun SearchDocument.searchScore(terms: List<SearchTerm>, phrase: String): Int {
        var termScore = 0
        terms.forEach { term ->
            val fieldScore = fieldsBlob.bestBlobScore(term)
            if (fieldScore == 0) return 0
            termScore += fieldScore
        }

        var primaryScore = 0
        terms.forEach { term ->
            primaryScore += primaryFieldsBlob.bestBlobScore(term)
        }

        val phraseScore = if (terms.size > 1 && phrase in phraseField) {
            30
        } else {
            0
        }
        return termScore + primaryScore + phraseScore
    }

    private fun String.bestBlobScore(term: SearchTerm): Int = when {
        isBlank() -> 0
        term.exactNeedle in this -> 40
        term.prefixNeedle in this -> 25
        term.value in this -> 15
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

    private fun normalizedFieldBlob(fields: List<String>): String {
        val normalizedFields = LinkedHashSet<String>()
        fields.forEach { field ->
            val normalized = field.normalizedSearchText()
            if (normalized.isNotBlank()) {
                normalizedFields += normalized
            }
        }
        if (normalizedFields.isEmpty()) return ""

        return buildString {
            normalizedFields.forEach { field ->
                append(FIELD_SEPARATOR)
                append(field)
            }
            append(FIELD_SEPARATOR)
        }
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

    private data class SearchIndex(
        val documents: List<SearchDocument>,
    )

    private data class SearchDocument(
        val option: MadridTransitOption,
        val fieldsBlob: String,
        val primaryFieldsBlob: String,
        val phraseField: String,
    )

    private data class SearchTerm(
        val value: String,
    ) {
        val exactNeedle: String = "$FIELD_SEPARATOR$value$FIELD_SEPARATOR"
        val prefixNeedle: String = "$FIELD_SEPARATOR$value"
    }

    private val BETTER_MATCH_FIRST = compareByDescending<SearchMatch> { match -> match.score }
        .thenBy { match -> match.option.label.lowercase(Locale.ROOT) }
        .thenBy { match -> match.option.id }
}
