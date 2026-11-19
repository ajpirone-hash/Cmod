package com.gangbox.app.export

import com.gangbox.app.data.JobInfo
import com.gangbox.app.data.OrderLine

/**
 * Builds the CSV file the warehouse opens in Excel.
 * Pure Kotlin (no Android code) so it can be unit tested.
 */
object CsvExporter {

    /** Byte-order mark so Excel reads characters like ” and é correctly. */
    const val UTF8_BOM = "﻿"

    val HEADER = listOf("Job", "Requested By", "Date Needed", "Category", "Tool", "Qty", "Notes")

    fun build(job: JobInfo, lines: List<OrderLine>): String {
        val sb = StringBuilder(UTF8_BOM)
        sb.append(row(HEADER))
        lines.forEach { line ->
            sb.append(
                row(
                    listOf(
                        job.jobName,
                        job.requestedBy,
                        job.dateNeeded,
                        line.category,
                        line.tool,
                        line.qty.toString(),
                        line.note,
                    )
                )
            )
        }
        return sb.toString()
    }

    /** A safe file name like "GangBox_Smith_Residence_2026-11-18.csv". */
    fun fileName(job: JobInfo, today: String): String {
        val jobPart = job.jobName.trim().replace(Regex("[^A-Za-z0-9-]+"), "_").trim('_').take(40)
        return if (jobPart.isEmpty()) "GangBox_$today.csv" else "GangBox_${jobPart}_$today.csv"
    }

    private fun row(cells: List<String>): String =
        cells.joinToString(",") { escape(it) } + "\r\n"

    internal fun escape(value: String): String {
        var v = value.replace("\r\n", " ").replace('\n', ' ').replace('\r', ' ')
        // Stop Excel from treating text that starts with = + - @ as a formula.
        if (v.isNotEmpty() && v[0] in "=+-@") v = "'$v"
        val needsQuotes = v.any { it == ',' || it == '"' } || v != v.trim()
        return if (needsQuotes) "\"" + v.replace("\"", "\"\"") + "\"" else v
    }
}
