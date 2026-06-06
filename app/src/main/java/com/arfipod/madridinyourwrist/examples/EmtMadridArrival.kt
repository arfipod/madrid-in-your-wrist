package com.arfipod.madridinyourwrist.examples

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object EmtMadridTarget {
    const val STOP_ID = "1064"
    const val LINE_ID = "E3"
    const val DESTINATION = "VALDERRIVAS"
    const val LABEL = "E3 stop 1064 -> Valderrivas"
    const val API_BASE_URL = "https://openapi.emtmadrid.es"

    val DEFAULT_STOP = EmtMadridStopTarget(
        stopId = STOP_ID,
        label = LABEL,
        lineId = LINE_ID,
        destination = DESTINATION,
    )
}

data class EmtMadridStopTarget(
    val stopId: String,
    val label: String,
    val lineId: String? = null,
    val destination: String? = null,
)

data class EmtMadridCredentials(
    val clientId: String,
    val passKey: String,
    val email: String,
    val password: String,
) {
    val hasProtectedLogin: Boolean
        get() = clientId.isNotBlank() && passKey.isNotBlank()

    val hasBasicLogin: Boolean
        get() = email.isNotBlank() && password.isNotBlank()

    val hasAnyLogin: Boolean
        get() = hasProtectedLogin || hasBasicLogin
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

    fun parseAllArrivals(rawBody: String): List<EmtBusArrival> {
        return arrivalObjects(rawBody)
            .asSequence()
            .mapNotNull { rawObject -> parseArrivalObject(rawObject) }
            .sortedBy { it.secondsUntil }
            .toList()
    }

    fun parseArrivals(
        rawBody: String,
        lineId: String? = EmtMadridTarget.LINE_ID,
        destination: String? = EmtMadridTarget.DESTINATION,
    ): List<EmtBusArrival> {
        return parseAllArrivals(rawBody)
            .asSequence()
            .filter { arrival ->
                lineId.isNullOrBlank() || arrival.lineId.equals(lineId, ignoreCase = true)
            }
            .filter { arrival ->
                destination.isNullOrBlank() ||
                    arrival.destination.normalized().contains(destination.normalized())
            }
            .toList()
    }

    private fun arrivalObjects(rawBody: String): List<String> {
        val objects = extractObjectsFromArray(rawBody, "Arrive") +
            extractObjectsFromArray(rawBody, "arrives")
        if (objects.isNotEmpty()) {
            return objects
        }

        return Regex("""\{[^{}]*(?:"lineId"|"lineArrive"|"line")[^{}]*\}""")
            .findAll(rawBody)
            .map { it.value }
            .toList()
    }

    private fun extractObjectsFromArray(rawBody: String, fieldName: String): List<String> {
        val objects = mutableListOf<String>()
        val marker = "\"$fieldName\""
        var searchFrom = 0

        while (searchFrom < rawBody.length) {
            val markerIndex = rawBody.indexOf(marker, startIndex = searchFrom)
            if (markerIndex < 0) break

            val colonIndex = rawBody.indexOf(':', startIndex = markerIndex + marker.length)
            val arrayStart = if (colonIndex < 0) -1 else rawBody.indexOf('[', startIndex = colonIndex + 1)
            if (arrayStart < 0) {
                searchFrom = markerIndex + marker.length
                continue
            }

            var arrayDepth = 0
            var objectDepth = 0
            var objectStart = -1
            var inString = false
            var escaped = false
            var index = arrayStart

            while (index < rawBody.length) {
                val char = rawBody[index]
                if (inString) {
                    when {
                        escaped -> escaped = false
                        char == '\\' -> escaped = true
                        char == '"' -> inString = false
                    }
                    index += 1
                    continue
                }

                when (char) {
                    '"' -> inString = true
                    '[' -> arrayDepth += 1
                    ']' -> {
                        arrayDepth -= 1
                        if (arrayDepth == 0) {
                            index += 1
                            break
                        }
                    }
                    '{' -> {
                        if (objectDepth == 0 && arrayDepth == 1) {
                            objectStart = index
                        }
                        if (objectDepth > 0 || arrayDepth == 1) {
                            objectDepth += 1
                        }
                    }
                    '}' -> {
                        if (objectDepth > 0) {
                            objectDepth -= 1
                            if (objectDepth == 0 && objectStart >= 0) {
                                objects += rawBody.substring(objectStart, index + 1)
                                objectStart = -1
                            }
                        }
                    }
                }

                index += 1
            }

            searchFrom = index
        }

        return objects
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
    private val email: String,
    private val password: String,
    private val baseUrl: String = EmtMadridTarget.API_BASE_URL,
) {
    constructor(
        credentials: EmtMadridCredentials,
        baseUrl: String = EmtMadridTarget.API_BASE_URL,
    ) : this(
        clientId = credentials.clientId,
        passKey = credentials.passKey,
        email = credentials.email,
        password = credentials.password,
        baseUrl = baseUrl,
    )

    private var cachedAccessToken: String? = null

    suspend fun fetchNextE3Arrival(): EmtBusArrival? = fetchArrivals(
        target = EmtMadridTarget.DEFAULT_STOP,
        limit = 1,
    ).firstOrNull()

    suspend fun fetchArrivals(
        target: EmtMadridStopTarget,
        limit: Int,
    ): List<EmtBusArrival> = withContext(Dispatchers.IO) {
        val credentials = EmtMadridCredentials(
            clientId = clientId,
            passKey = passKey,
            email = email,
            password = password,
        )
        require(credentials.hasAnyLogin) { "EMT credentials are blank." }

        val token = accessToken(credentials)
        val arrivals = requestArrivals(
            accessToken = token,
            stopId = target.stopId,
        )
        EmtMadridJson.parseArrivals(
            rawBody = arrivals,
            lineId = target.lineId,
            destination = target.destination,
        ).take(limit.coerceAtLeast(1))
    }

    private fun accessToken(credentials: EmtMadridCredentials): String {
        val cached = cachedAccessToken
        if (!cached.isNullOrBlank()) return cached

        return login(credentials).also { cachedAccessToken = it }
    }

    private fun login(credentials: EmtMadridCredentials): String {
        val protectedFailure = if (credentials.hasProtectedLogin) {
            runCatching { loginWithHeaders("X-ClientId" to clientId, "passKey" to passKey) }
                .fold(
                    onSuccess = { return it },
                    onFailure = { it },
                )
        } else {
            null
        }

        if (credentials.hasBasicLogin) {
            return loginWithHeaders("email" to email, "password" to password)
        }

        if (protectedFailure != null) {
            throw protectedFailure
        }

        throw IOException("EMT credentials are incomplete.")
    }

    private fun loginWithHeaders(vararg headers: Pair<String, String>): String {
        val body = request(
            method = "GET",
            path = "/v1/mobilitylabs/user/login/",
            headers = headers.toMap(),
            body = null,
        )
        return EmtMadridJson.findAccessToken(body)
            ?: throw IOException("EMT login response did not include accessToken.")
    }

    private fun requestArrivals(accessToken: String, stopId: String): String = request(
        method = "POST",
        path = "/v2/transport/busemtmad/stops/$stopId/arrives/",
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
