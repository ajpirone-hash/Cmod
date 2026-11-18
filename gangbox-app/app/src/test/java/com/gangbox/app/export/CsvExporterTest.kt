package com.gangbox.app.export

import com.gangbox.app.data.JobInfo
import com.gangbox.app.data.OrderLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {

    private val job = JobInfo(jobName = "Smith Residence", requestedBy = "Nick", dateNeeded = "11/20")

    @Test
    fun buildsHeaderAndRows() {
        val csv = CsvExporter.build(job, listOf(OrderLine("Plumbing", "Pipe Wrench 18\"", 2, "")))
        assertTrue(csv.startsWith(CsvExporter.UTF8_BOM))
        val lines = csv.removePrefix(CsvExporter.UTF8_BOM).split("\r\n")
        assertEquals("Job,Requested By,Date Needed,Category,Tool,Qty,Notes", lines[0])
        assertEquals("Smith Residence,Nick,11/20,Plumbing,\"Pipe Wrench 18\"\"\",2,", lines[1])
    }

    @Test
    fun quotesCommasAndFlattensNewlines() {
        assertEquals("\"a, b\"", CsvExporter.escape("a, b"))
        assertEquals("line one line two", CsvExporter.escape("line one\nline two"))
    }

    @Test
    fun blocksFormulas() {
        assertEquals("'=SUM(A1)", CsvExporter.escape("=SUM(A1)"))
    }

    @Test
    fun makesSafeFileName() {
        assertEquals("GangBox_Smith_Residence_2026-11-18.csv", CsvExporter.fileName(job, "2026-11-18"))
        assertEquals("GangBox_2026-11-18.csv", CsvExporter.fileName(JobInfo(), "2026-11-18"))
    }
}
