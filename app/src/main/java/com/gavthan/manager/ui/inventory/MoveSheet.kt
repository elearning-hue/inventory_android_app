package com.gavthan.manager.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gavthan.manager.data.InventoryItem
import com.gavthan.manager.data.InventoryRepo
import com.gavthan.manager.data.Party
import com.gavthan.manager.data.num
import com.gavthan.manager.ui.components.AppBottomSheet
import com.gavthan.manager.ui.components.AppButton
import com.gavthan.manager.ui.components.AppTextField
import com.gavthan.manager.ui.components.Banner
import com.gavthan.manager.ui.components.BannerTone
import com.gavthan.manager.ui.components.DateField
import com.gavthan.manager.ui.components.FieldLabel
import com.gavthan.manager.ui.components.Segmented
import com.gavthan.manager.ui.components.Toaster
import com.gavthan.manager.ui.theme.gav
import com.gavthan.manager.util.Fmt
import kotlinx.coroutines.launch

@Composable
fun MoveSheet(
    item: InventoryItem,
    suppliers: List<Party>,
    meEmail: String?,
    onClose: () -> Unit,
    onSaved: () -> Unit,
) {
    val c = gav
    val scope = rememberCoroutineScope()
    val supplier = suppliers.firstOrNull { it.id == item.supplierId }
    val cur = item.qty

    var type by remember { mutableStateOf("in") }
    var qtyStr by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf(item.costPrice.takeIf { it > 0 }?.let { trimQty(it) } ?: "") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(Fmt.todayIso()) }
    var supplierLedger by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    val q = num(qtyStr)
    val projected = when (type) { "in" -> cur + q; "adjust" -> q; else -> cur - q }

    fun save() {
        if (type != "adjust" && q <= 0) return
        if (busy) return
        busy = true
        scope.launch {
            runCatching {
                InventoryRepo.recordMove(
                    item = item, type = type, qtyInput = q, cost = num(cost),
                    note = note, date = date, supplierLedger = supplierLedger, meEmail = meEmail,
                )
            }.onSuccess {
                Toaster.show("Stock updated"); onSaved()
            }.onFailure {
                Toaster.error(it.message ?: "Save failed"); busy = false
            }
        }
    }

    AppBottomSheet(item.name, onClose) {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(item.category ?: "", color = c.muted, fontSize = 12.sp)
            Text("Current: ${trimQty(cur)} ${item.unit ?: ""}", color = c.muted, fontSize = 12.sp)
        }
        FieldLabel("Movement")
        Segmented(
            options = listOf("in" to "Purchase", "out" to "Used", "waste" to "Waste", "adjust" to "Set to"),
            selected = type, onSelect = { type = it }, alt = type == "out" || type == "waste",
        )
        FieldLabel((if (type == "adjust") "New counted stock" else "Quantity") + " (${item.unit ?: "unit"})")
        AppTextField(qtyStr, { qtyStr = it }, placeholder = "0", keyboardType = KeyboardType.Decimal)

        if (type == "in") {
            FieldLabel("Cost / ${item.unit ?: "unit"} (optional, updates item cost)")
            AppTextField(cost, { cost = it }, placeholder = "0", keyboardType = KeyboardType.Decimal)
            if (!item.supplierId.isNullOrBlank()) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Checkbox(
                        checked = supplierLedger,
                        onCheckedChange = { supplierLedger = it },
                        colors = CheckboxDefaults.colors(checkedColor = c.accent),
                    )
                    Text(
                        "Also add ${Fmt.money(q * num(cost))} to ${supplier?.name ?: "supplier"}’s ledger",
                        color = c.ink, fontSize = 14.sp,
                    )
                }
            }
        }

        FieldLabel("Date")
        DateField(date, { date = it })
        FieldLabel("Note (optional)")
        AppTextField(note, { note = it }, placeholder = "invoice no, reason…")

        Spacer(Modifier.height(12.dp))
        Banner(
            "New stock: ${trimQty(if (projected.isNaN()) cur else projected)} ${item.unit ?: ""}",
            BannerTone.Neutral,
        )
        Spacer(Modifier.height(14.dp))
        AppButton(
            text = if (busy) "Saving…" else "Record movement",
            onClick = { save() },
            enabled = type == "adjust" || q > 0,
            loading = busy,
        )
    }
}
