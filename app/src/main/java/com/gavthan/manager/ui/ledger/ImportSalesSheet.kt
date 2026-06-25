package com.gavthan.manager.ui.ledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gavthan.manager.data.AppUser
import com.gavthan.manager.ui.components.AppBottomSheet
import com.gavthan.manager.ui.components.AppButton
import com.gavthan.manager.ui.components.AppDropdown
import com.gavthan.manager.ui.components.Banner
import com.gavthan.manager.ui.components.BannerTone
import com.gavthan.manager.ui.components.BtnStyle
import com.gavthan.manager.ui.components.EmptyState
import com.gavthan.manager.ui.components.FieldLabel
import androidx.compose.material3.Text
import com.gavthan.manager.ui.components.Segmented
import com.gavthan.manager.ui.components.StatTile
import com.gavthan.manager.ui.components.Toaster
import com.gavthan.manager.ui.components.Tone
import com.gavthan.manager.ui.theme.gav
import com.gavthan.manager.util.Fmt
import kotlinx.coroutines.launch

@Composable
fun ImportSalesSheet(
    pending: List<BillRow>,
    isAdmin: Boolean,
    users: List<AppUser>,
    userMap: Map<String, String>,
    meEmail: String,
    onClose: () -> Unit,
    onConfirm: suspend (assignTo: String?, subset: List<BillRow>?) -> Unit,
) {
    val c = gav
    val scope = rememberCoroutineScope()
    val count = pending.size
    val total = pending.sumOf { it.amount }

    var tab by remember { mutableStateOf("bulk") }
    var assign by remember { mutableStateOf("original") }
    val perAssign: SnapshotStateMap<String, String> = remember { mutableStateMapOf() }
    var busy by remember { mutableStateOf(false) }
    var busyId by remember { mutableStateOf<String?>(null) }

    val userOptions = users.map {
        it.email to (Fmt.initCap(it.displayName ?: it.email.substringBefore("@")) +
            if (it.email == meEmail) " (you)" else "")
    }

    AppBottomSheet("Import sales from bills", onClose) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("New bills", count.toString(), Tone.Neutral, Modifier.weight(1f))
            StatTile("Total", Fmt.money(total), Tone.Green, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Segmented(
            options = listOf("bulk" to "Import once", "one" to "One by one"),
            selected = tab, onSelect = { tab = it },
        )

        if (tab == "bulk") {
            FieldLabel("Attribute these sales to")
            if (isAdmin) {
                AppDropdown(
                    assign,
                    listOf("original" to "Each bill's original staff") + userOptions,
                    { assign = it },
                )
                Spacer(Modifier.height(6.dp))
                Banner(
                    "Default keeps each bill credited to whoever raised it. Choose a staff member to reassign all $count imported sales instead.",
                    BannerTone.Neutral,
                )
            } else {
                Banner("Credited to each bill's original staff. (Reassigning requires an admin.)", BannerTone.Neutral)
            }
            Spacer(Modifier.height(14.dp))
            AppButton(
                text = if (busy) "Importing…" else "Import $count sale${if (count == 1) "" else "s"}",
                onClick = {
                    if (busy || count == 0) return@AppButton
                    busy = true
                    scope.launch {
                        runCatching { onConfirm(if (assign == "original") null else assign, null) }
                            .onSuccess { onClose() }
                            .onFailure { Toaster.error(it.message ?: "Import failed"); busy = false }
                    }
                },
                enabled = count > 0,
                loading = busy,
            )
        } else {
            Spacer(Modifier.height(10.dp))
            Text(
                "Import each bill on its own${if (isAdmin) ", choosing who it's credited to" else ""}. $count pending.",
                color = c.muted, fontSize = 12.sp,
            )
            Spacer(Modifier.height(6.dp))
            if (count == 0) {
                EmptyState("✅", "All bills imported")
            } else {
                pending.forEach { b ->
                    BillImportRow(
                        b = b, isAdmin = isAdmin, userOptions = userOptions, userMap = userMap,
                        selected = perAssign[b.id] ?: "original",
                        onSelect = { perAssign[b.id] = it },
                        busy = busyId == b.id, anyBusy = busyId != null,
                        onImport = {
                            busyId = b.id
                            scope.launch {
                                val who = perAssign[b.id]
                                runCatching { onConfirm(if (who == null || who == "original") null else who, listOf(b)) }
                                    .onFailure { Toaster.error(it.message ?: "Import failed") }
                                busyId = null
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BillImportRow(
    b: BillRow,
    isAdmin: Boolean,
    userOptions: List<Pair<String, String>>,
    userMap: Map<String, String>,
    selected: String,
    onSelect: (String) -> Unit,
    busy: Boolean,
    anyBusy: Boolean,
    onImport: () -> Unit,
) {
    val c = gav
    Row(
        Modifier.fillMaxWidth().padding(vertical = 11.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(b.billNo?.let { "#$it" } ?: "(no number)", color = c.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(b.name ?: "", color = c.muted, fontSize = 12.sp)
            }
            Text("${Fmt.fmtDate(b.date)} · ${Fmt.money(b.amount)}", color = c.muted, fontSize = 12.sp)
            if (isAdmin) {
                Spacer(Modifier.height(6.dp))
                AppDropdown(
                    selected,
                    listOf("original" to ("Original staff" + (b.addedBy?.let { " (${Fmt.dispName(it, userMap)})" } ?: ""))) + userOptions,
                    onSelect,
                )
            }
        }
        AppButton(
            text = if (busy) "…" else "Import",
            onClick = onImport,
            modifier = Modifier,
            style = BtnStyle.Primary,
            enabled = !anyBusy,
            loading = busy,
            small = true,
        )
    }
    androidx.compose.material3.HorizontalDivider(color = c.line)
}
