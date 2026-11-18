package com.gangbox.app.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.gangbox.app.data.JobInfo
import com.gangbox.app.data.OrderLine
import java.io.File
import java.time.LocalDate

/** Writes the CSV and opens Android's share menu (email, text, Drive, Teams...). */
object Share {

    fun sendToWarehouse(context: Context, job: JobInfo, lines: List<OrderLine>) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }

        val today = LocalDate.now().toString()
        val file = File(dir, CsvExporter.fileName(job, today))
        file.writeText(CsvExporter.build(job, lines), Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val jobLabel = job.jobName.ifBlank { "Gang box" }
        val subject = "Gang box list - $jobLabel" + (if (job.dateNeeded.isNotBlank()) " (needed ${job.dateNeeded})" else "")
        val body = buildString {
            append("Gang box tool list attached")
            append(" (").append(lines.size).append(if (lines.size == 1) " item" else " items").append(").\n\n")
            if (job.jobName.isNotBlank()) append("Job: ").append(job.jobName).append('\n')
            if (job.requestedBy.isNotBlank()) append("Requested by: ").append(job.requestedBy).append('\n')
            if (job.dateNeeded.isNotBlank()) append("Needed: ").append(job.dateNeeded).append('\n')
            append('\n')
            lines.forEach { l ->
                append("- ").append(l.qty).append(" x ").append(l.tool)
                if (l.note.isNotBlank()) append(" (").append(l.note).append(')')
                append('\n')
            }
        }

        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            if (job.warehouseEmail.isNotBlank()) {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(job.warehouseEmail.trim()))
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Send gang box list"))
    }
}
