package com.arfipod.wearosplayground.examples

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object EmtMadridTarget {
    const val STOP_ID = "755"
    const val LINE_ID = "E3"
    const val DESTINATION = "VALDERRIVAS"
    const val LABEL = "E3 Felipe II -> Valderrivas"
    const val API_BASE_URL = "https://openapi.emtmadrid.es"
}

data class EmtBusArrival(
    val lineId: String,
    val destination: String,
    val secondsUntil: Int,
    val metersAway: Int?,
) {
    val minutesUntil: Int
        get() = if (secondsUntil >= 999_999) {
            20
        } else {
            (secondsUntil + 59) / 60
        }

    fun compactLabel(): String {
        val time = when {
            secondsUntil == 0 -> "Now"
            secondsUntil >= 999_999 -> "+20 min"
            else -> "$minutesUntil min"
        }
        val distance = metersAway?.let { "\n${it} m" }.orEmpty()
        return "$time\n$lineId -> $destination$distance"
    }
}

object EmtMadridJson {
    fun findAccessToken(rawBody: String): String? = Regex(""""accessToken"\s*:\s*"([^"]+)"""")
        .find(rawBody)
        ?.groupValues
        ?.get(1)

    fun parseArrivals(
        rawBody: String,
        lineId: String = EmtMadridTarget.LINE_ID,
        destination: String = EmtMadridTarget.DESTINATION,
    ): List<EmtBusArrival> {
        return Regex("""\{[^{}]*(?:"lineId"|"lineArrive")[^{}]*}""")
            .findAll(rawBody)
            .mapNotNull { match -> parseArrivalObject(match.value) }
            .filter { arrival ->
                arrival.lineId.equals(lineId, ignoreCase = true) &&
                    arrival.destination.normalized().contains(destination.normalized())
            }
            .sortedBy { it.secondsUntil }
            .toList()
    }

    private fun parseArrivalObject(rawObject: String): EmtBusArrival? {
        val line = stringField(rawObject, "lineId")
            ?: stringField(rawObject, "lineArrive")
            ?: stringField(rawObject, "line")
            ?: return null
        val destination = stringField(rawObject, "destination")
            ?: stringField(rawObject, "Destination")
            ?: ""
        val seconds = intField(rawObject, "busTimeLeft")
            ?: intField(rawObject, "estimateArrive")
            ?: intField(rawObject, "EstimateArrive")
            ?: return null
        val meters = intField(rawObject, "busDistance")
            ?: intField(rawObject, "DistanceBus")
            ?: intField(rawObject, "distanceBus")

        return EmtBusArrival(
            lineId = line,
            destination = destination,
            secondsUntil = seconds,
            metersAway = meters,
        )
    }

    private fun stringField(rawObject: String, field: String): String? =
        Regex(""""$field"\s*:\s*"([^"]*)"""")
            .find(rawObject)
            ?.groupValues
            ?.get(1)

    private fun intField(rawObject: String, field: String): Int? =
        Regex(""""$field"\s*:\s*(\d+)""")
            .find(rawObject)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
}

class EmtMadridClient(
    private val clientId: String,
    private val passKey: String,
    private val baseUrl: String = EmtMadridTarget.API_BASE_URL,
) {
    suspend fun fetchNextE3Arrival(): EmtBusArrival? = withContext(Dispatchers.IO) {
        require(clientId.isNotBlank()) { "EMT client id is blank." }
        require(passKey.isNotBlank()) { "EMT pass key is blank." }

        val token = login()
        val arrivals = requestArrivals(token)
        EmtMadridJson.parseArrivals(arrivals).firstOrNull()
    }

    private fun login(): String {
        val body = request(
            method = "GET",
            path = "/v3/mobilitylabs/user/login/",
            headers = mapOf(
                "X-ClientId" to clientId,
                "passKey" to passKey,
            ),
            body = null,
        )
        return EmtMadridJson.findAccessToken(body)
            ?: throw IOException("EMT login response did not include accessToken.")
    }

    private fun requestArrivals(accessToken: String): String = request(
        method = "POST",
        path = "/v2/transport/busemtmad/stops/${EmtMadridTarget.STOP_ID}/arrives/${EmtMadridTarget.LINE_ID}/",
        headers = mapOf(
            "accessToken" to accessToken,
            "Accept" to "application/json",
            "Content-Type" to "application/json",
        ),
        body = """
            {
              "cultureInfo": "ES",
              "Text_StopRequired_YN": "Y",
              "Text_EstimationsRequired_YN": "Y",
              "Text_IncidencesRequired_YN": "N",
              "statistics": "N"
            }
        """.trimIndent(),
    )

    private fun request(
        method: String,
        path: String,
        headers: Map<String, String>,
        body: String?,
    ): String {
        val connection = URI("$baseUrl$path").toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.requestMethod = method
        connection.setRequestProperty("User-Agent", "wearos-playground")
        headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }

        if (body != null) {
            connection.doOutput = true
            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
            }
        }

        return try {
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val response = stream?.use { it.readBytes() }?.toString(Charsets.UTF_8).orEmpty()
            if (responseCode !in 200..299) {
                throw IOException("EMT HTTP $responseCode ${response.take(120)}")
            }
            response
        } finally {
            connection.disconnect()
        }
    }
}

private fun String.normalized(): String = lowercase(Locale.ROOT)
    .replace("á", "a")
    .replace("é", "e")
    .replace("í", "i")
    .replace("ó", "o")
    .replace("ú", "u")
