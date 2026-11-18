package com.gangbox.app.data

/** One tool the crew can pick. */
data class Tool(
    val id: String,
    val name: String,
    val category: String,
    /** True for tools a user added on the phone (these can be deleted). */
    val custom: Boolean = false,
)

/** A checked tool, with how many and an optional note. */
data class Pick(
    val qty: Int = 1,
    val note: String = "",
)

/** Header info that goes at the top of the order. */
data class JobInfo(
    val jobName: String = "",
    val requestedBy: String = "",
    val dateNeeded: String = "",
    val warehouseEmail: String = "",
)

/** One row of the exported list. */
data class OrderLine(
    val category: String,
    val tool: String,
    val qty: Int,
    val note: String,
)
