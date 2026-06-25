package com.gavthan.manager.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gavthan.manager.data.InventoryItem
import com.gavthan.manager.data.InventoryRepo
import com.gavthan.manager.data.Party
import com.gavthan.manager.data.num
import com.gavthan.manager.ui.components.AppBottomSheet
import com.gavthan.manager.ui.components.AppButton
import com.gavthan.manager.ui.components.AppDropdown
import com.gavthan.manager.ui.components.AppTextField
import com.gavthan.manager.ui.components.Banner
import com.gavthan.manager.ui.components.BannerTone
import com.gavthan.manager.ui.components.BtnStyle
import com.gavthan.manager.ui.components.FieldLabel
import com.gavthan.manager.ui.components.Toaster
import com.gavthan.manager.ui.theme.gav
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant

@Composable
fun ItemSheet(
    item: InventoryItem?,
    suppliers: List<Party>,
    meEmail: String?,
    onClose: () -> Unit,
    onSaved: () -> Unit,
) {
    val c = gav
    val scope = rememberCoroutineScope()
    val isNew = item == null

    var name by remember { mutableStateOf(item?.name ?: "") }
    var category by remember { mutableStateOf(item?.category ?: "") }
    var unit by remember { mutableStateOf(item?.unit ?: "kg") }
    var qty by remember { mutableStateOf(item?.qty?.let { trimQty(it) } ?: "0") }
    var reorder by remember { mutableStateOf(item?.reorderLevel?.let { trimQty(it) } ?: "0") }
    var cost by remember { mutableStateOf(item?.costPrice?.takeIf { it > 0 }?.let { trimQty(it) } ?: "") }
    var supplierId by remember { mutableStateOf(item?.supplierId ?: "") }
    var busy by remember { mutableStateOf(false) }
    var confirmArchive by remember { mutableStateOf(false) }

    fun save() {
        if (name.isBlank() || busy) return
        busy = true
        scope.launch {
            val base = buildJsonObject {
                put("name", name.trim())
                put("category", category.trim().ifBlank { null })
                put("unit", unit.trim().ifBlank { "unit" })
                put("reorder_level", num(reorder))
                put("cost_price", num(cost))
                put("supplier_id", supplierId.ifBlank { null })
                put("updated_at", Instant.now().toString())
            }
            runCatching {
                if (isNew) {
                    val row = buildJsonObject {
                        base.forEach { (k, v) -> put(k, v) }
                        put("qty", num(qty))
                        put("created_by", meEmail)
                        put("active", true)
                    }
                    InventoryRepo.insertItem(row)
                } else {
                    InventoryRepo.updateItem(item!!.id, base)
                }
            }.onSuccess {
                Toaster.show(if (isNew) "Item added" else "Item updated"); onSaved()
            }.onFailure {
                Toaster.error(it.message ?: "Save failed"); busy = false
            }
        }
    }

    fun archive() {
        busy = true
        scope.launch {
            runCatching { InventoryRepo.archiveItem(item!!.id) }
                .onSuccess { Toaster.show("Archived"); onSaved() }
                .onFailure { Toaster.error(it.message ?: "Failed"); busy = false }
        }
    }

    AppBottomSheet(if (isNew) "New item" else "Edit item", onClose) {
        FieldLabel("Name")
        AppTextField(name, { name = it })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                FieldLabel("Category")
                AppTextField(category, { category = it }, placeholder = "Grocery, Veg…")
            }
            Column(Modifier.width(120.dp)) {
                FieldLabel("Unit")
                AppTextField(unit, { unit = it }, placeholder = "kg / L / pcs")
            }
        }
        if (isNew) {
            FieldLabel("Opening stock")
            AppTextField(qty, { qty = it }, keyboardType = KeyboardType.Decimal)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                FieldLabel("Reorder level")
                AppTextField(reorder, { reorder = it }, keyboardType = KeyboardType.Decimal)
            }
            Column(Modifier.weight(1f)) {
                FieldLabel("Cost / ${unit.ifBlank { "unit" }}")
                AppTextField(cost, { cost = it }, placeholder = "0", keyboardType = KeyboardType.Decimal)
            }
        }
        FieldLabel("Supplier (optional)")
        AppDropdown(
            supplierId,
            listOf("" to "— none —") + suppliers.map { it.id to it.name },
            { supplierId = it },
        )
        if (!isNew) {
            Spacer(Modifier.height(10.dp))
            Banner("To change the stock count, tap the item and record a movement instead — that keeps an audit trail.", BannerTone.Neutral)
        }
        Spacer(Modifier.height(14.dp))
        AppButton(
            text = if (busy) "Saving…" else if (isNew) "Add item" else "Save changes",
            onClick = { save() },
            enabled = name.isNotBlank(),
            loading = busy,
        )
        if (!isNew) {
            Spacer(Modifier.height(10.dp))
            AppButton("Archive item", { confirmArchive = true }, Modifier.fillMaxWidth(), BtnStyle.Danger, enabled = !busy, small = true)
        }
    }

    if (confirmArchive && item != null) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            confirmButton = { TextButton(onClick = { confirmArchive = false; archive() }) { Text("Archive", color = c.debit) } },
            dismissButton = { TextButton(onClick = { confirmArchive = false }) { Text("Cancel", color = c.muted) } },
            title = { Text("Archive “${item.name}”?", color = c.ink) },
            text = { Text("It will be hidden but its history is kept.", color = c.muted) },
            containerColor = c.surface,
        )
    }
}
