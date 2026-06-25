package com.gavthan.manager.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant

private val sb get() = Supa.client

/* ----------------------------- Auth ----------------------------- */
object AuthRepo {
    suspend fun signIn(email: String, password: String) {
        sb.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
    }

    suspend fun signOut() = sb.auth.signOut()

    /** Load the mh_users profile (role/active) for the signed-in email. */
    suspend fun loadMe(email: String): AppUser {
        return runCatching {
            sb.from("mh_users").select {
                filter { eq("email", email) }
            }.decodeList<JsonObject>().firstOrNull()?.let { AppUser.from(it) }
        }.getOrNull() ?: AppUser(email = email, role = "staff", active = true, displayName = null)
    }
}

/* ---------------------------- Ledger ---------------------------- */
data class LedgerData(
    val entries: List<LedgerEntry> = emptyList(),
    val bills: List<Bill> = emptyList(),
    val users: List<AppUser> = emptyList(),
)

object LedgerRepo {
    suspend fun loadAll(): LedgerData = coroutineScope {
        val eD = async {
            sb.from("mh_ledger").select {
                order("entry_date", Order.DESCENDING)
                order("created_at", Order.DESCENDING)
            }.decodeList<JsonObject>().map { LedgerEntry.from(it) }
        }
        val cD = async {
            sb.from("mh_customers").select().decodeList<JsonObject>().map { Bill.from(it) }
        }
        val uD = async {
            sb.from("mh_users").select().decodeList<JsonObject>().map { AppUser.from(it) }
        }
        LedgerData(
            entries = eD.await(),
            bills = cD.await().filter { it.isSettled },
            users = uD.await().filter { it.active != false },
        )
    }

    suspend fun insert(rows: List<JsonObject>) {
        if (rows.isEmpty()) return
        sb.from("mh_ledger").insert(rows)
    }

    suspend fun insertOne(row: JsonObject) = sb.from("mh_ledger").insert(row)

    suspend fun setSettled(id: String, value: Boolean) {
        sb.from("mh_ledger").update(buildJsonObject { put("settled", value) }) {
            filter { eq("id", id) }
        }
    }
}

/* --------------------------- Inventory -------------------------- */
data class InventoryData(
    val items: List<InventoryItem> = emptyList(),
    val moves: List<StockMove> = emptyList(),
    val suppliers: List<Party> = emptyList(),
)

object InventoryRepo {
    suspend fun loadAll(): InventoryData = coroutineScope {
        val iD = async {
            sb.from("mh_inventory_items").select {
                order("name", Order.ASCENDING)
            }.decodeList<JsonObject>().map { InventoryItem.from(it) }
        }
        val mD = async {
            sb.from("mh_stock_moves").select {
                order("created_at", Order.DESCENDING)
                limit(300L)
            }.decodeList<JsonObject>().map { StockMove.from(it) }
        }
        val sD = async {
            sb.from("mh_parties").select(Columns.list("id", "name")) {
                filter { eq("type", "supplier") }
                order("name", Order.ASCENDING)
            }.decodeList<JsonObject>().map { Party.from(it) }
        }
        InventoryData(
            items = iD.await().filter { it.active != false },
            moves = mD.await(),
            suppliers = sD.await(),
        )
    }

    suspend fun insertItem(row: JsonObject) = sb.from("mh_inventory_items").insert(row)

    suspend fun updateItem(id: String, row: JsonObject) {
        sb.from("mh_inventory_items").update(row) { filter { eq("id", id) } }
    }

    suspend fun archiveItem(id: String) {
        sb.from("mh_inventory_items").update(buildJsonObject { put("active", false) }) {
            filter { eq("id", id) }
        }
    }

    /**
     * Record a movement, update the item's stock (and latest cost on purchase),
     * and optionally post a purchase to the supplier's ledger — mirrors the
     * web MoveSheet.save().
     */
    suspend fun recordMove(
        item: InventoryItem,
        type: String,
        qtyInput: Double,
        cost: Double,
        note: String,
        date: String,
        supplierLedger: Boolean,
        meEmail: String?,
    ) {
        val cur = item.qty
        val q = qtyInput
        val projected = when (type) {
            "in" -> cur + q
            "adjust" -> q
            else -> cur - q
        }
        val delta = when (type) {
            "in" -> q
            "adjust" -> q - cur
            else -> -q
        }
        val moveQty = if (type == "adjust") Math.abs(delta) else q
        val unitCost: Double? = if (type == "in") (cost.takeIf { it > 0 }) else null
        val noteN: String? = note.trim().ifBlank { null }

        sb.from("mh_stock_moves").insert(buildJsonObject {
            put("item_id", item.id)
            put("move_type", type)
            put("qty", moveQty)
            put("unit_cost", unitCost)
            put("note", noteN)
            put("move_date", date)
            put("created_by", meEmail)
        })

        val newQty = Math.round(projected * 1000.0) / 1000.0
        sb.from("mh_inventory_items").update(buildJsonObject {
            put("qty", newQty)
            put("updated_at", Instant.now().toString())
            if (type == "in" && cost > 0) put("cost_price", cost)
        }) { filter { eq("id", item.id) } }

        if (type == "in" && supplierLedger && !item.supplierId.isNullOrBlank() && cost > 0) {
            val amount = q * cost
            sb.from("mh_ledger").insert(buildJsonObject {
                put("party_id", item.supplierId)
                put("entry_type", "credit")
                put("amount", amount)
                put("mode", "credit")
                put("note", "Purchase: ${item.name} ×${trimNum(q)}")
                put("entry_date", date)
                put("source", "manual")
                put("created_by", meEmail)
            })
        }
    }
}

/** Build a ledger insert row (manual entry / excel import). */
fun ledgerRow(
    entryType: String,
    amount: Double,
    category: String?,
    note: String?,
    entryDate: String?,
    mode: String?,
    source: String,
    createdBy: String?,
    ref: String? = null,
): JsonObject = buildJsonObject {
    put("entry_type", entryType)
    put("amount", amount)
    putNullable("category", category)
    putNullable("note", note)
    putNullable("entry_date", entryDate)
    putNullable("mode", mode)
    put("source", source)
    putNullable("created_by", createdBy)
    if (ref != null) put("ref", ref)
}

private fun JsonObjectBuilder.putNullable(key: String, value: String?) {
    put(key, value)
}

private fun trimNum(d: Double): String =
    if (d == Math.floor(d) && !d.isInfinite()) d.toLong().toString() else d.toString()
