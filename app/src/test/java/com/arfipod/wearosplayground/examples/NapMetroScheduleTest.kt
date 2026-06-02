package com.arfipod.wearosplayground.examples

import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
            now = LocalDateTime.of(2026, 6, 1, 10, 0, 0),
        )

        assertNotNull(departure)
        assertEquals("Goya", departure!!.stopName)
        assertEquals("L2 Linea 2", departure.routeName)
        assertEquals("Las Rosas", departure.destination)
        assertEquals(5, departure.minutesUntil)
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

    private fun ZipOutputStream.putTextEntry(name: String, text: String) {
        putNextEntry(ZipEntry(name))
        write(text.toByteArray(Charsets.UTF_8))
        closeEntry()
    }
}
