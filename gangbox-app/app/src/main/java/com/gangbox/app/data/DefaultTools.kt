package com.gangbox.app.data

/**
 * The starter tool list loaded the first time the app opens.
 * Crews can add their own tools in the app; "Reset tool list" brings this list back.
 */
object DefaultTools {

    private val byCategory: Map<String, List<String>> = linkedMapOf(
        "Power Tools" to listOf(
            "Cordless Drill/Driver",
            "Cordless Impact Driver",
            "Hammer Drill",
            "Rotary Hammer (SDS)",
            "Right Angle Drill",
            "Sawzall (Reciprocating Saw)",
            "Portaband (Band Saw)",
            "Circular Saw",
            "Angle Grinder",
            "Oscillating Multi-Tool",
            "Spare Batteries",
            "Battery Chargers",
        ),
        "Plumbing" to listOf(
            "Pipe Wrench 14\"",
            "Pipe Wrench 18\"",
            "Pipe Wrench 24\"",
            "Basin Wrench",
            "Copper Tubing Cutter",
            "PVC Cutter",
            "Pipe Threader",
            "Press Tool (ProPress)",
            "Press Jaws 1/2\"-1\"",
            "Press Jaws 1-1/4\"-2\"",
            "PEX Crimp Tool",
            "PEX Expansion Tool",
            "Torch Kit",
            "Solder & Flux",
            "Drain Snake/Auger",
            "Test Plugs",
            "Test Gauge",
            "Pipe Vise/Tripod Stand",
        ),
        "Electrical" to listOf(
            "Multimeter",
            "Clamp Meter",
            "Non-Contact Voltage Tester",
            "Wire Strippers",
            "Lineman's Pliers",
            "Cable Cutters",
            "Crimpers",
            "Conduit Bender 1/2\"",
            "Conduit Bender 3/4\"",
            "Conduit Bender 1\"",
            "Knockout Set",
            "Fish Tape",
            "Wire Puller",
            "Cable Lube",
            "Label Maker",
        ),
        "Hand Tools" to listOf(
            "Screwdriver Set",
            "Nut Driver Set",
            "Channel Locks",
            "Adjustable Wrench Set",
            "Socket Set",
            "Hex Key Set",
            "Hammer",
            "Utility Knife",
            "Hacksaw",
            "Tape Measure",
            "Torpedo Level",
            "4' Level",
            "Chalk Line",
            "Flashlight",
        ),
        "Layout & Measuring" to listOf(
            "Laser Level",
            "Laser Tripod",
            "Stud Finder",
            "Marking Paint",
            "Sharpies",
        ),
        "Ladders & Access" to listOf(
            "6' Step Ladder",
            "8' Step Ladder",
            "10' Step Ladder",
            "Extension Ladder",
            "Rolling Scaffold",
        ),
        "Safety" to listOf(
            "Hard Hats",
            "Safety Glasses",
            "Gloves",
            "Hearing Protection",
            "Hi-Vis Vests",
            "Harness & Lanyard",
            "First Aid Kit",
            "Fire Extinguisher",
            "Lockout/Tagout Kit",
        ),
        "Site & Power" to listOf(
            "Extension Cords",
            "Spider Box",
            "Work Lights",
            "Shop Vac",
            "Gang Box Lock",
        ),
        "Consumables" to listOf(
            "Drill Bit Set",
            "Hole Saw Kit",
            "Sawzall Blades",
            "Portaband Blades",
            "Grinder Discs",
            "Teflon Tape",
            "Pipe Dope",
            "PVC Primer & Cement",
            "Wire Nuts",
            "Electrical Tape",
            "Zip Ties",
            "Anchors & Fasteners",
            "Rags",
        ),
    )

    val categories: List<String> = byCategory.keys.toList()

    val tools: List<Tool> = byCategory.flatMap { (category, names) ->
        names.map { name -> Tool(id = idFor(category, name), name = name, category = category) }
    }

    fun idFor(category: String, name: String): String =
        (category + "_" + name).lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
}
