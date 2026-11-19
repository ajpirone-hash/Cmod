package com.gangbox.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gangbox.app.data.Tool
import com.gangbox.app.export.Share

private val GangBoxColors = lightColorScheme(
    primary = Color(0xFFE65100),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0B2),
    onPrimaryContainer = Color(0xFF3E2723),
    secondary = Color(0xFF37474F),
    onSecondary = Color.White,
    surfaceVariant = Color(0xFFF1EDEA),
)

@Composable
fun GangBoxApp(vm: GangBoxViewModel = viewModel()) {
    MaterialTheme(colorScheme = GangBoxColors) {
        MainScreen(vm)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun MainScreen(vm: GangBoxViewModel) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var showReview by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<ConfirmAction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gang Box", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Uncheck all tools") },
                            onClick = { menuOpen = false; confirm = ConfirmAction.ClearChecks },
                        )
                        DropdownMenuItem(
                            text = { Text("Reset tool list") },
                            onClick = { menuOpen = false; confirm = ConfirmAction.ResetList },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add tool") },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 6.dp, shadowElevation = 6.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = when (vm.pickedCount) {
                            0 -> "No tools checked"
                            1 -> "1 tool checked"
                            else -> "${vm.pickedCount} tools checked"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = { showReview = true }, enabled = vm.pickedCount > 0) {
                        Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Send to warehouse")
                    }
                }
            }
        },
    ) { padding ->
        val groups = vm.visibleGroups()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item { JobCard(vm) }
            item { SearchField(vm.search) { vm.search = it } }
            if (groups.isEmpty()) {
                item {
                    Text(
                        "No tools match \"${vm.search}\". Tap Add tool to add it.",
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
            groups.forEach { (category, tools) ->
                stickyHeader(key = "h_$category") { CategoryHeader(category, tools.count { vm.isPicked(it) }) }
                items(tools, key = { it.id }) { tool ->
                    ToolRow(
                        tool = tool,
                        vm = vm,
                        onDelete = { confirm = ConfirmAction.DeleteTool(tool) },
                    )
                    HorizontalDivider()
                }
            }
            item { Spacer(Modifier.heightIn(min = 96.dp)) }
        }
    }

    if (showAdd) {
        AddToolDialog(
            categories = vm.categories,
            initialName = vm.search.trim(),
            onAdd = { name, cat -> vm.addTool(name, cat) },
            onDismiss = { showAdd = false },
        )
    }

    if (showReview) {
        ReviewDialog(
            vm = vm,
            onSend = {
                showReview = false
                Share.sendToWarehouse(context, vm.job, vm.orderLines())
            },
            onDismiss = { showReview = false },
        )
    }

    confirm?.let { action ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(action.title) },
            text = { Text(action.message) },
            confirmButton = {
                TextButton(onClick = {
                    when (action) {
                        ConfirmAction.ClearChecks -> vm.clearChecks()
                        ConfirmAction.ResetList -> vm.resetToolList()
                        is ConfirmAction.DeleteTool -> vm.deleteTool(action.tool)
                    }
                    confirm = null
                }) { Text(action.button) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } },
        )
    }
}

private sealed class ConfirmAction(val title: String, val message: String, val button: String) {
    data object ClearChecks : ConfirmAction(
        "Uncheck all tools?", "This clears every check mark, quantity and note.", "Uncheck all",
    )
    data object ResetList : ConfirmAction(
        "Reset tool list?",
        "This brings back the original starter list. Tools you added stay on the list.",
        "Reset",
    )
    class DeleteTool(val tool: Tool) : ConfirmAction(
        "Remove ${tool.name}?", "This takes it off the tool list on this phone.", "Remove",
    )
}

@Composable
private fun JobCard(vm: GangBoxViewModel) {
    val job = vm.job
    var expanded by rememberSaveable { mutableStateOf(job.jobName.isBlank()) }
    Card(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Job info", style = MaterialTheme.typography.titleMedium)
                    if (!expanded) {
                        Text(
                            listOf(job.jobName, job.requestedBy, job.dateNeeded)
                                .filter { it.isNotBlank() }
                                .joinToString("  •  ")
                                .ifEmpty { "Tap to fill in" },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Hide job info" else "Show job info",
                )
            }
            if (expanded) {
                JobField("Job name / address", job.jobName) { vm.updateJob(job.copy(jobName = it)) }
                JobField("Requested by", job.requestedBy) { vm.updateJob(job.copy(requestedBy = it)) }
                JobField("Date needed (e.g. Thu 11/20)", job.dateNeeded) { vm.updateJob(job.copy(dateNeeded = it)) }
                JobField(
                    "Warehouse email (optional)",
                    job.warehouseEmail,
                    keyboardType = KeyboardType.Email,
                ) { vm.updateJob(job.copy(warehouseEmail = it)) }
                Spacer(Modifier.heightIn(min = 8.dp))
            }
        }
    }
}

@Composable
private fun JobField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = if (keyboardType == KeyboardType.Email) KeyboardCapitalization.None else KeyboardCapitalization.Words,
            keyboardType = keyboardType,
        ),
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
    )
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text("Search tools") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

@Composable
private fun CategoryHeader(category: String, checked: Int) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                category.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (checked > 0) {
                Text("$checked checked", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ToolRow(tool: Tool, vm: GangBoxViewModel, onDelete: () -> Unit) {
    val pick = vm.picks[tool.id]
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { vm.toggle(tool) }
            .padding(start = 4.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = pick != null, onCheckedChange = { vm.toggle(tool) })
            Text(
                tool.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (pick != null) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
            )
            if (pick != null) {
                QtyStepper(pick.qty) { vm.setQty(tool, it) }
            }
            if (tool.custom) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove ${tool.name}")
                }
            }
        }
        if (pick != null) {
            OutlinedTextField(
                value = pick.note,
                onValueChange = { vm.setNote(tool, it) },
                placeholder = { Text("Note (size, brand, etc.)") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(start = 48.dp, bottom = 6.dp),
            )
        }
    }
}

@Composable
private fun QtyStepper(qty: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(
            onClick = { onChange(qty - 1) },
            enabled = qty > 1,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            modifier = Modifier.size(40.dp),
        ) { Text("−", fontSize = 20.sp) }
        Text(
            qty.toString(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(40.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        OutlinedButton(
            onClick = { onChange(qty + 1) },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            modifier = Modifier.size(40.dp),
        ) { Text("+", fontSize = 20.sp) }
    }
}

@Composable
private fun AddToolDialog(
    categories: List<String>,
    initialName: String,
    onAdd: (String, String) -> Boolean,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var category by remember { mutableStateOf(categories.firstOrNull() ?: "Other") }
    var newCategory by remember { mutableStateOf("") }
    var pickingCategory by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val newCatLabel = "+ New category"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a tool") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Tool name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Category", style = MaterialTheme.typography.labelLarge)
                Column {
                    OutlinedButton(onClick = { pickingCategory = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(category, modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = pickingCategory, onDismissRequest = { pickingCategory = false }) {
                        (categories + newCatLabel).forEach { c ->
                            DropdownMenuItem(text = { Text(c) }, onClick = { category = c; pickingCategory = false })
                        }
                    }
                }
                if (category == newCatLabel) {
                    OutlinedTextField(
                        value = newCategory,
                        onValueChange = { newCategory = it },
                        label = { Text("New category name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cat = if (category == newCatLabel) newCategory else category
                if (onAdd(name, cat)) onDismiss()
                else error = if (name.isBlank()) "Type a tool name." else "That tool is already on the list."
            }) { Text("Add & check") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ReviewDialog(vm: GangBoxViewModel, onSend: () -> Unit, onDismiss: () -> Unit) {
    val lines = vm.orderLines()
    val job = vm.job
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send ${lines.size} ${if (lines.size == 1) "item" else "items"}?") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (job.jobName.isBlank()) {
                    Text(
                        "No job name yet. The warehouse may not know where this goes.",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                } else {
                    Text("Job: ${job.jobName}", fontWeight = FontWeight.SemiBold)
                    if (job.dateNeeded.isNotBlank()) Text("Needed: ${job.dateNeeded}")
                    Spacer(Modifier.heightIn(min = 8.dp))
                }
                lines.forEach { l ->
                    Text(
                        "${l.qty} × ${l.tool}" + if (l.note.isNotBlank()) "  (${l.note})" else "",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.heightIn(min = 8.dp))
                Text(
                    "Next, pick how to send it (email, text, Teams...). The list is attached as a spreadsheet file.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = { Button(onClick = onSend) { Text("Send") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Back") } },
    )
}
