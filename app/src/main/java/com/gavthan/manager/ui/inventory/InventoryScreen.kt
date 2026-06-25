package com.gavthan.manager.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gavthan.manager.data.AppUser
import com.gavthan.manager.data.InventoryItem
import com.gavthan.manager.ui.components.AppBottomSheet
import com.gavthan.manager.ui.components.AppButton
import com.gavthan.manager.ui.components.BtnStyle
import com.gavthan.manager.ui.components.CardHeader
import com.gavthan.manager.ui.components.EmptyState
import com.gavthan.manager.ui.components.Pill
import com.gavthan.manager.ui.components.PillTone
import com.gavthan.manager.ui.components.SearchField
import com.gavthan.manager.ui.components.SectionCard
import com.gavthan.manager.ui.components.Segmented
import com.gavthan.manager.ui.components.StatTile
import com.gavthan.manager.ui.components.Tone
import com.gavthan.manager.ui.theme.gav
import com.gavthan.manager.util.Fmt

@Composable
fun InventoryScreen(me: AppUser?, online: Boolean) {
    val c = gav
    val vm: InventoryViewModel = viewModel()
    val data by vm.data.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val live by vm.live.collectAsStateWithLifecycle()

    val items = data.items
    val moves = data.moves
    val suppliers = data.suppliers

    var q by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("all") }

    var showItemSheet by remember { mutableStateOf(false) }
    var sheetItem by remember { mutableStateOf<InventoryItem?>(null) }
    var moveFor by remember { mutableStateOf<InventoryItem?>(null) }
    var showHistory by remember { mutableStateOf(false) }

    val totalValue = remember(items) { items.sumOf { it.qty * it.costPrice } }
    val lowCount = remember(items) { items.count { it.isLow } }
    val list = remember(items, q, filter) {
        val s = q.trim().lowercase()
        items
            .filter { filter == "all" || it.isLow }
            .filter { s.isBlank() || it.name.lowercase().contains(s) || (it.category ?: "").lowercase().contains(s) }
            .sortedWith(compareByDescending<InventoryItem> { it.isLow }.thenBy { it.name.lowercase() })
    }

    Box(Modifier.fillMaxSize()) {
        if (loading) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = c.accent)
                Spacer(Modifier.height(10.dp))
                Text("Loading stock…", color = c.muted)
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp).padding(bottom = 96.dp),
            ) {
                Spacer(Modifier.height(12.dp))
                InventoryDash(items = items, moves = moves, live = live)

                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Stock value", Fmt.money(totalValue), Tone.Neutral, Modifier.weight(1f))
                    StatTile("Low / out", lowCount.toString(), if (lowCount > 0) Tone.Warn else Tone.Neutral, Modifier.weight(1f))
                }

                Spacer(Modifier.height(12.dp))
                Segmented(
                    options = listOf("all" to "All items (${items.size})", "low" to "Low stock ($lowCount)"),
                    selected = filter, onSelect = { filter = it },
                )
                Spacer(Modifier.height(12.dp))
                SearchField(q, { q = it }, "Search items / category")

                Spacer(Modifier.height(12.dp))
                SectionCard {
                    if (list.isEmpty()) {
                        EmptyState("📦", if (items.isNotEmpty()) "Nothing matches." else "No items yet. Add your first stock item.")
                    } else {
                        list.forEachIndexed { idx, it ->
                            ItemRow(it = it, onClick = { moveFor = it }, onEdit = { sheetItem = it; showItemSheet = true })
                            if (idx < list.size - 1) HorizontalDivider(color = c.line)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                SectionCard {
                    CardHeader("Recent stock moves") {
                        AppButton("View all", { showHistory = true }, Modifier, BtnStyle.Ghost, small = true)
                    }
                    HorizontalDivider(color = c.line)
                    if (moves.isEmpty()) {
                        EmptyState("🗂", "No movements recorded")
                    } else {
                        moves.take(6).forEachIndexed { idx, mv ->
                            MoveRow(mv = mv, items = items)
                            if (idx < minOf(6, moves.size) - 1) HorizontalDivider(color = c.line)
                        }
                    }
                }
            }

            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 16.dp),
            ) {
                AppButton("＋  New item", { sheetItem = null; showItemSheet = true }, Modifier.fillMaxWidth())
            }
        }
    }

    if (showItemSheet) {
        ItemSheet(
            item = sheetItem, suppliers = suppliers, meEmail = me?.email,
            onClose = { showItemSheet = false },
            onSaved = { showItemSheet = false; vm.load() },
        )
    }
    moveFor?.let { item ->
        MoveSheet(
            item = item, suppliers = suppliers, meEmail = me?.email,
            onClose = { moveFor = null },
            onSaved = { moveFor = null; vm.load() },
        )
    }
    if (showHistory) {
        AppBottomSheet("Stock movement history", onClose = { showHistory = false }) {
            if (moves.isEmpty()) {
                EmptyState("🗂", "No movements")
            } else {
                moves.forEach { mv -> MoveRow(mv = mv, items = items) }
            }
        }
    }
}

@Composable
private fun ItemRow(it: InventoryItem, onClick: () -> Unit, onEdit: () -> Unit) {
    val c = gav
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(it.name, color = c.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (it.isLow) Pill("low", PillTone.Low) else Pill("ok", PillTone.Ok)
            }
            val sub = buildString {
                if (!it.category.isNullOrBlank()) append("${it.category} · ")
                if (it.costPrice > 0) append("${Fmt.money(it.costPrice)}/${it.unit ?: "unit"}")
                if (it.reorderLevel > 0) append(" · reorder @ ${trimQty(it.reorderLevel)}")
            }.trim().trimEnd('·', ' ')
            if (sub.isNotEmpty()) Text(sub, color = c.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(trimQty(it.qty), color = if (it.isLow) c.debit else c.ink, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            Text(it.unit ?: "units", color = c.muted, fontSize = 11.sp)
        }
        Box(
            Modifier.size(36.dp).clickable { onEdit() },
            contentAlignment = Alignment.Center,
        ) {
            Text("✎", color = c.muted, fontSize = 15.sp, textAlign = TextAlign.Center)
        }
    }
}
