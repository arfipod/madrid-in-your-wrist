package com.arfipod.madridinyourwrist.examples

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmtMadridArrivalTest {
    @Test
    fun accessTokenIsParsedFromLoginResponse() {
        val token = EmtMadridJson.findAccessToken(
            """{"data":[{"accessToken":"abc-123"}]}"""
        )

        assertEquals("abc-123", token)
    }

    @Test
    fun arrivalsAreFilteredByLineAndDestination() {
        val arrivals = EmtMadridJson.parseArrivals(
            """
                {
                  "data": [
                    {
                      "Arrive": [
                        {
                          "lineArrive": "E3",
                          "destination": "VALDERRIVAS",
                          "estimateArrive": 360,
                          "DistanceBus": 1200
                        },
                        {
                          "lineArrive": "E2",
                          "destination": "LAS MUSAS",
                          "estimateArrive": 120,
                          "DistanceBus": 400
                        }
                      ]
                    }
                  ]
                }
            """.trimIndent()
        )

        assertEquals(1, arrivals.size)
        assertEquals("E3", arrivals.first().lineId)
        assertEquals(6, arrivals.first().minutesUntil)
        assertEquals(1200, arrivals.first().metersAway)
    }

    @Test
    fun allArrivalsAreParsedAndSortedForGenericStops() {
        val arrivals = EmtMadridJson.parseAllArrivals(
            """
                {
                  "data": [
                    {
                      "Arrive": [
                        {
                          "lineArrive": "E3",
                          "destination": "VALDERRIVAS",
                          "estimateArrive": 360
                        },
                        {
                          "lineArrive": "100",
                          "destination": "MORATALAZ",
                          "estimateArrive": 120
                        }
                      ]
                    }
                  ]
                }
            """.trimIndent()
        )

        assertEquals(listOf("100", "E3"), arrivals.map { it.lineId })
    }

    @Test
    fun currentV2ArriveShapeIsParsed() {
        val arrivals = EmtMadridJson.parseArrivals(
            """
                {
                  "data": [
                    {
                      "Arrive": [
                        {
                          "line": "E3",
                          "stop": "1064",
                          "destination": "VALDERRIVAS",
                          "geometry": {
                            "type": "Point",
                            "coordinates": [
                              -3.6692320578255995,
                              40.421145073486166
                            ]
                          },
                          "estimateArrive": 225,
                          "DistanceBus": 1696
                        }
                      ]
                    }
                  ]
                }
            """.trimIndent()
        )

        assertEquals("4 min\nE3 -> VALDERRIVAS\n1696 m", arrivals.first().compactLabel())
    }

    @Test
    fun nonArrivalObjectsAreIgnored() {
        val arrivals = EmtMadridJson.parseArrivals(
            """
                {
                  "StopInfo": [
                    {
                      "lines": [
                        {
                          "line": "014",
                          "label": "14"
                        }
                      ]
                    }
                  ]
                }
            """.trimIndent()
        )

        assertEquals(0, arrivals.size)
    }

    @Test
    fun basicLoginCredentialsAreAccepted() {
        val credentials = EmtMadridCredentials(
            clientId = "",
            passKey = "",
            email = "user@example.com",
            password = "secret",
        )

        assertEquals(true, credentials.hasBasicLogin)
        assertEquals(true, credentials.hasAnyLogin)
    }

    @Test
    fun oldArrivesShapeIsAlsoParsed() {
        val arrivals = EmtMadridJson.parseArrivals(
            """
                {
                  "arrives": [
                    {
                      "lineId": "E3",
                      "destination": "VALDERRIVAS",
                      "busTimeLeft": 0,
                      "busDistance": 0
                    }
                  ]
                }
            """.trimIndent()
        )

        assertEquals("Now\nE3 -> VALDERRIVAS\n0 m", arrivals.first().compactLabel())
    }

    @Test
    fun missingTokenReturnsNull() {
        assertNull(EmtMadridJson.findAccessToken("""{"data":[]}"""))
    }
}
