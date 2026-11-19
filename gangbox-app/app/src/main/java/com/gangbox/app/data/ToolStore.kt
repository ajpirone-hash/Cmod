package com.gangbox.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Saves the tool list, the current checks and the job info on the phone,
 * so nothing is lost if the app is closed.
 */
class ToolStore(context: Context) {

    private val prefs = context.getSharedPreferences("gangbox", Context.MODE_PRIVATE)

    fun loadTools(): List<Tool> {
        val raw = prefs.getString(KEY_TOOLS, null) ?: return DefaultTools.tools
        return runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                Tool(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    category = o.getString("category"),
                    custom = o.optBoolean("custom", false),
                )
            }
        }.getOrDefault(DefaultTools.tools)
    }

    fun saveTools(tools: List<Tool>) {
        val arr = JSONArray()
        tools.forEach { t ->
            arr.put(
                JSONObject()
                    .put("id", t.id)
                    .put("name", t.name)
                    .put("category", t.category)
                    .put("custom", t.custom)
            )
        }
        prefs.edit().putString(KEY_TOOLS, arr.toString()).apply()
    }

    fun loadPicks(): Map<String, Pick> {
        val raw = prefs.getString(KEY_PICKS, null) ?: return emptyMap()
        return runCatching {
            val o = JSONObject(raw)
            o.keys().asSequence().associateWith { id ->
                val p = o.getJSONObject(id)
                Pick(qty = p.optInt("qty", 1), note = p.optString("note", ""))
            }
        }.getOrDefault(emptyMap())
    }

    fun savePicks(picks: Map<String, Pick>) {
        val o = JSONObject()
        picks.forEach { (id, p) -> o.put(id, JSONObject().put("qty", p.qty).put("note", p.note)) }
        prefs.edit().putString(KEY_PICKS, o.toString()).apply()
    }

    fun loadJob(): JobInfo = JobInfo(
        jobName = prefs.getString("job_name", "") ?: "",
        requestedBy = prefs.getString("requested_by", "") ?: "",
        dateNeeded = prefs.getString("date_needed", "") ?: "",
        warehouseEmail = prefs.getString("warehouse_email", "") ?: "",
    )

    fun saveJob(job: JobInfo) {
        prefs.edit()
            .putString("job_name", job.jobName)
            .putString("requested_by", job.requestedBy)
            .putString("date_needed", job.dateNeeded)
            .putString("warehouse_email", job.warehouseEmail)
            .apply()
    }

    private companion object {
        const val KEY_TOOLS = "tools"
        const val KEY_PICKS = "picks"
    }
}
