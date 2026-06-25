package com.gavthan.manager.ui.ledger

val INCOME_CATS = listOf("Sales", "Investment", "Other income")

val EXPENSE_CATS = listOf(
    "Supplies / Purchase", "Salary / Wages", "Rent", "Utilities", "Gas / Fuel",
    "Equipment", "Repairs", "Marketing", "Transport", "Misc",
)

val CAT_EMOJI = mapOf(
    "Sales" to "🧾", "Investment" to "💰", "Other income" to "➕",
    "Supplies / Purchase" to "🛒", "Salary / Wages" to "👤", "Rent" to "🏠",
    "Utilities" to "💡", "Gas / Fuel" to "🔥", "Equipment" to "🔧", "Repairs" to "🛠",
    "Marketing" to "📣", "Transport" to "🚚", "Misc" to "📦",
)

fun catEmoji(cat: String?): String = CAT_EMOJI[cat] ?: ""
