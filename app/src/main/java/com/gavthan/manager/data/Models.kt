package com.gavthan.manager.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/* ============================ mh_users ============================ */
data class AppUser(
    val email: String,
    val role: String?,
    val active: Boolean?,
    val displayName: String?,
) {
    val isAdmin get() = role == "admin"

    companion object {
        fun from(o: JsonObject) = AppUser(
            email = o.string("email") ?: "",
            role = o.string("role"),
            active = o.bool("active"),
            displayName = o.string("display_name"),
        )
    }
}

/* ============================ mh_ledger ============================ */
data class LedgerEntry(
    val id: String,
    val entryType: String,   // income | expense | credit
    val amount: Double,
    val category: String?,
    val note: String?,
    val entryDate: String?,  // yyyy-MM-dd
    val mode: String?,
    val source: String?,     // manual | bill | excel
    val createdBy: String?,
    val settled: Boolean?,   // null/true => "Yes"; false => "No"
    val ref: String?,
    val createdAt: String?,
) {
    val kind: String get() = if (entryType == "expense") "expense" else "income"
    val dateKey: String get() = (entryDate ?: createdAt ?: "").take(10)

    companion object {
        fun from(o: JsonObject) = LedgerEntry(
            id = o.string("id") ?: "",
            entryType = o.string("entry_type") ?: "expense",
            amount = o.double("amount") ?: 0.0,
            category = o.string("category"),
            note = o.string("note"),
            entryDate = o.string("entry_date"),
            mode = o.string("mode"),
            source = o.string("source"),
            createdBy = o.string("created_by"),
            settled = o.bool("settled"),
            ref = o.string("ref"),
            createdAt = o.string("created_at"),
        )
    }
}

/* ===================== mh_customers (bills) ====================== */
data class Bill(
    val id: String,
    val billNo: String?,
    val name: String?,
    val addedBy: String?,
    val status: String?,
    val items: JsonElement?,
    val discountOn: Boolean?,
    val discountPct: Double?,
    val adjustmentOn: Boolean?,
    val adjustment: Double?,
    val settledAt: String?,
    val date: String?,
    val billTime: String?,
    val createdAt: String?,
    val phone: String?,
) {
    val isSettled get() = status == "settled"

    /**
     * EXACT copy of the web billing app's finalTotal():
     *   total = sum(price*qty) - discountAmt + adjustAmt
     */
    fun total(): Double {
        val arr: List<JsonObject> = when (val it = items) {
            is JsonArray -> it.filterIsInstance<JsonObject>()
            is JsonPrimitive -> if (it.isString) {
                runCatching { Supa.json.parseToJsonElement(it.content) }
                    .getOrNull()
                    ?.let { e -> (e as? JsonArray)?.filterIsInstance<JsonObject>() }
                    ?: emptyList()
            } else emptyList()
            else -> emptyList()
        }
        val raw = arr.sumOf { num(it["price"]) * num(it["qty"]) }
        val disc = if (discountOn == true && num(discountPct) > 0)
            Math.round(raw * num(discountPct) / 100.0).toDouble() else 0.0
        val adj = if (adjustmentOn == true) num(adjustment) else 0.0
        return raw - disc + adj
    }

    companion object {
        fun from(o: JsonObject) = Bill(
            id = o.string("id") ?: "",
            billNo = o.string("bill_no"),
            name = o.string("name"),
            addedBy = o.string("added_by"),
            status = o.string("status"),
            items = o["items"],
            discountOn = o.bool("discount_on"),
            discountPct = o.double("discount_pct"),
            adjustmentOn = o.bool("adjustment_on"),
            adjustment = o.double("adjustment"),
            settledAt = o.string("settled_at"),
            date = o.string("date"),
            billTime = o.string("bill_time"),
            createdAt = o.string("created_at"),
            phone = o.string("phone"),
        )
    }
}

/* ===================== mh_inventory_items ======================= */
data class InventoryItem(
    val id: String,
    val name: String,
    val category: String?,
    val unit: String?,
    val qty: Double,
    val reorderLevel: Double,
    val costPrice: Double,
    val supplierId: String?,
    val active: Boolean?,
) {
    val isLow get() = reorderLevel > 0 && qty <= reorderLevel

    companion object {
        fun from(o: JsonObject) = InventoryItem(
            id = o.string("id") ?: "",
            name = o.string("name") ?: "(item)",
            category = o.string("category"),
            unit = o.string("unit"),
            qty = o.double("qty") ?: 0.0,
            reorderLevel = o.double("reorder_level") ?: 0.0,
            costPrice = o.double("cost_price") ?: 0.0,
            supplierId = o.string("supplier_id"),
            active = o.bool("active"),
        )
    }
}

/* ====================== mh_stock_moves ========================== */
data class StockMove(
    val id: String,
    val itemId: String?,
    val moveType: String,  // in | out | waste | adjust
    val qty: Double,
    val unitCost: Double?,
    val note: String?,
    val moveDate: String?,
    val createdBy: String?,
    val createdAt: String?,
) {
    companion object {
        fun from(o: JsonObject) = StockMove(
            id = o.string("id") ?: "",
            itemId = o.string("item_id"),
            moveType = o.string("move_type") ?: "",
            qty = o.double("qty") ?: 0.0,
            unitCost = o.double("unit_cost"),
            note = o.string("note"),
            moveDate = o.string("move_date"),
            createdBy = o.string("created_by"),
            createdAt = o.string("created_at"),
        )
    }
}

/* ========================= mh_parties =========================== */
data class Party(
    val id: String,
    val name: String,
    val type: String?,
) {
    companion object {
        fun from(o: JsonObject) = Party(
            id = o.string("id") ?: "",
            name = o.string("name") ?: "",
            type = o.string("type"),
        )
    }
}
