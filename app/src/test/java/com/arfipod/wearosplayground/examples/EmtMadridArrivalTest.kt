package com.arfipod.wearosplayground.examples

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
