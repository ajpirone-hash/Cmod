package com.gangbox.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.gangbox.app.data.DefaultTools
import com.gangbox.app.data.JobInfo
import com.gangbox.app.data.OrderLine
import com.gangbox.app.data.Pick
import com.gangbox.app.data.Tool
import com.gangbox.app.data.ToolStore

class GangBoxViewModel(app: Application) : AndroidViewModel(app) {

    private val store = ToolStore(app)

    var tools by mutableStateOf(store.loadTools())
        private set
    var picks by mutableStateOf(store.loadPicks())
        private set
    var job by mutableStateOf(store.loadJob())
        private set
    var search by mutableStateOf("")

    val categories: List<String>
        get() = (DefaultTools.categories + tools.map { it.category }).distinct()

    val pickedCount: Int get() = picks.size

    fun isPicked(tool: Tool) = picks.containsKey(tool.id)

    fun toggle(tool: Tool) {
        picks = if (isPicked(tool)) picks - tool.id else picks + (tool.id to Pick())
        store.savePicks(picks)
    }

    fun setQty(tool: Tool, qty: Int) {
        val current = picks[tool.id] ?: return
        picks = picks + (tool.id to current.copy(qty = qty.coerceIn(1, 999)))
        store.savePicks(picks)
    }

    fun setNote(tool: Tool, note: String) {
        val current = picks[tool.id] ?: return
        picks = picks + (tool.id to current.copy(note = note))
        store.savePicks(picks)
    }

    fun updateJob(newJob: JobInfo) {
        job = newJob
        store.saveJob(job)
    }

    /** Adds a tool and checks it. Returns false if the name is blank or already on the list. */
    fun addTool(name: String, category: String): Boolean {
        val cleanName = name.trim()
        val cleanCategory = category.trim().ifEmpty { "Other" }
        if (cleanName.isEmpty()) return false
        if (tools.any { it.name.equals(cleanName, true) && it.category.equals(cleanCategory, true) }) return false
        val tool = Tool(
            id = "custom_" + DefaultTools.idFor(cleanCategory, cleanName) + "_" + System.currentTimeMillis(),
            name = cleanName,
            category = cleanCategory,
            custom = true,
        )
        tools = tools + tool
        store.saveTools(tools)
        picks = picks + (tool.id to Pick())
        store.savePicks(picks)
        return true
    }

    fun deleteTool(tool: Tool) {
        tools = tools.filterNot { it.id == tool.id }
        picks = picks - tool.id
        store.saveTools(tools)
        store.savePicks(picks)
    }

    fun clearChecks() {
        picks = emptyMap()
        store.savePicks(picks)
    }

    /** Puts the starter list back but keeps tools the crew added. */
    fun resetToolList() {
        tools = DefaultTools.tools + tools.filter { it.custom }
        picks = picks.filterKeys { id -> tools.any { it.id == id } }
        store.saveTools(tools)
        store.savePicks(picks)
    }

    /** Tools that match the search, grouped by category in list order. */
    fun visibleGroups(): List<Pair<String, List<Tool>>> {
        val q = search.trim()
        val filtered = if (q.isEmpty()) tools else tools.filter {
            it.name.contains(q, ignoreCase = true) || it.category.contains(q, ignoreCase = true)
        }
        return categories.mapNotNull { cat ->
            val inCat = filtered.filter { it.category == cat }
            if (inCat.isEmpty()) null else cat to inCat
        }
    }

    fun orderLines(): List<OrderLine> =
        categories.flatMap { cat ->
            tools.filter { it.category == cat && picks.containsKey(it.id) }.map { t ->
                val p = picks.getValue(t.id)
                OrderLine(category = t.category, tool = t.name, qty = p.qty, note = p.note.trim())
            }
        }
}
