package com.arfipod.madridinyourwrist.examples

import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NapMetroScheduleTest {
    @Test
    fun csvParserHandlesQuotedCommas() {
        val row = GtfsCsv.parseLine("1,\"Goya, Felipe II\",\"Metro \"\"L2\"\"\"")

        assertEquals(listOf("1", "Goya, Felipe II", "Metro \"L2\""), row)
    }

    @Test
    fun apiJsonFindsDownloadUrlAndGtfsFileId() {
        val json = """
            {
              "ficheros": [
                { "id": 12, "nombre": "detalle" },
                { "id": 9331, "tipoFichero": "GTFS-ZIP" }
              ],
              "link": "https://example.test/metro.zip"
            }
        """.trimIndent()

        assertEquals(9331, NapMetroApiJson.findFileId(json))
        assertEquals("https://example.test/metro.zip", NapMetroApiJson.findDownloadUrl(json))
    }

    @Test
    fun nextDepartureUsesActiveGtfsService() {
        val schedule = MetroGtfsSchedule()
        val departure = schedule.nextDeparture(
            gtfsZipBytes = metroFixtureZip(),
            target = MetroScheduleTarget(
                label = "Goya / Felipe II",
                stopNameQuery = "Goya",
                routeNameQuery = "2",
                destinationQuery = "Las Rosas",
            ),
            now = LocalDateTime.of(2026, 6, 1, 10, 0, 0),
        )

        assertNotNull(departure)
        assertEquals("Goya", departure!!.stopName)
        assertEquals("L2 Linea 2", departure.routeName)
        assertEquals("Las Rosas", departure.destination)
        assertEquals(5, departure.minutesUntil)
    }

    @Test
    fun nextDepartureUsesFrequencyGtfsForDefaultArguellesTarget() {
        val schedule = MetroGtfsSchedule()
        val departure = schedule.nextDeparture(
            gtfsZipBytes = metroFrequencyFixtureZip(),
            now = LocalDateTime.of(2026, 6, 1, 10, 1, 0),
        )

        assertNotNull(departure)
        assertEquals("ARGÜELLES", departure!!.stopName)
        assertEquals("4 Argüelles-Pinar de Chamartín", departure.routeName)
        assertEquals("Pinar de Chamartín", departure.destination)
        assertEquals(2, departure.minutesUntil)
    }

    @Test
    fun nextDeparturesReturnsRequestedFrequencyDepartures() {
        val schedule = MetroGtfsSchedule()
        val departures = schedule.nextDepartures(
            gtfsZipBytes = metroFrequencyFixtureZip(),
            count = 3,
            now = LocalDateTime.of(2026, 6, 1, 10, 1, 0),
        )

        assertEquals(listOf(2L, 7L, 12L), departures.map { it.minutesUntil })
    }

    @Test
    fun nextDepartureResultFallsBackToExpiredWeekdayPattern() {
        val schedule = MetroGtfsSchedule()
        val result = schedule.nextDepartureResult(
            gtfsZipBytes = metroFrequencyFixtureZip(),
            now = LocalDateTime.of(2027, 6, 4, 10, 1, 0),
        )

        assertTrue(result.isStale)
        assertEquals(LocalDate.of(2026, 12, 31), result.validUntil)
        assertNotNull(result.departure)
        assertEquals("Pinar de Chamartín", result.departure!!.destination)
    }

    @Test
    fun nextDepartureReturnsNullForUnknownStop() {
        val schedule = MetroGtfsSchedule()
        val departure = schedule.nextDeparture(
            gtfsZipBytes = metroFixtureZip(),
            target = MetroScheduleTarget(stopNameQuery = "No existe"),
            now = LocalDateTime.of(2026, 6, 1, 10, 0, 0),
        )

        assertNull(departure)
    }

    private fun metroFixtureZip(): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putTextEntry(
                "stops.txt",
                """
                    stop_id,stop_name
                    goya,Goya
                    sol,Sol
                """.trimIndent(),
            )
            zip.putTextEntry(
                "routes.txt",
                """
                    route_id,route_short_name,route_long_name
                    l2,L2,Linea 2
                """.trimIndent(),
            )
            zip.putTextEntry(
                "trips.txt",
                """
                    route_id,service_id,trip_id,trip_headsign
                    l2,weekday,trip-a,Las Rosas
                """.trimIndent(),
            )
            zip.putTextEntry(
                "stop_times.txt",
                """
                    trip_id,arrival_time,departure_time,stop_id,stop_sequence
                    trip-a,09:55:00,09:55:00,sol,1
                    trip-a,10:05:00,10:05:00,goya,2
                """.trimIndent(),
            )
            zip.putTextEntry(
                "calendar.txt",
                """
                    service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date
                    weekday,1,1,1,1,1,0,0,20260101,20261231
                """.trimIndent(),
            )
            zip.putTextEntry(
                "calendar_dates.txt",
                """
                    service_id,date,exception_type
                """.trimIndent(),
            )
        }
        return output.toByteArray()
    }

    private fun metroFrequencyFixtureZip(): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putTextEntry(
                "stops.txt",
                """
                    stop_id,stop_name
                    arguelles,ARGÜELLES
                    san_bernardo,SAN BERNARDO
                """.trimIndent(),
            )
            zip.putTextEntry(
                "routes.txt",
                """
                    ﻿route_id,route_short_name,route_long_name
                    l4,4,Argüelles-Pinar de Chamartín
                """.trimIndent(),
            )
            zip.putTextEntry(
                "trips.txt",
                """
                    route_id,service_id,trip_id,trip_headsign
                    l4,weekday,trip-l4,Pinar de Chamartín
                """.trimIndent(),
            )
            zip.putTextEntry(
                "stop_times.txt",
                """
                    trip_id,arrival_time,departure_time,stop_id,stop_sequence
                    trip-l4,00:02:10,00:02:10,arguelles,1
                    trip-l4,00:04:20,00:04:20,san_bernardo,2
                """.trimIndent(),
            )
            zip.putTextEntry(
                "frequencies.txt",
                """
                    trip_id,start_time,end_time,headway_secs
                    trip-l4,10:00:00,11:00:00,300
                """.trimIndent(),
            )
            zip.putTextEntry(
                "calendar.txt",
                """
                    service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date
                    weekday,1,1,1,1,1,0,0,20260101,20261231
                """.trimIndent(),
            )
            zip.putTextEntry(
                "calendar_dates.txt",
                """
                    service_id,date,exception_type
                """.trimIndent(),
            )
        }
        return output.toByteArray()
    }

    private fun ZipOutputStream.putTextEntry(name: String, text: String) {
        putNextEntry(ZipEntry(name))
        write(text.toByteArray(Charsets.UTF_8))
        closeEntry()
    }
}
