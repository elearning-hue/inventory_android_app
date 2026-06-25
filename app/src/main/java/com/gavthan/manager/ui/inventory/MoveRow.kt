package com.gavthan.manager.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gavthan.manager.data.InventoryItem
import com.gavthan.manager.data.StockMove
import com.gavthan.manager.ui.theme.gav
import com.gavthan.manager.util.Fmt

private data class MoveMeta(val sign: String, val kind: String, val label: String)

private fun metaOf(type: String): MoveMeta = when (type) {
    "in" -> MoveMeta("+", "c", "Purchase / in")
    "out" -> MoveMeta("−", "d", "Used / out")
    "waste" -> MoveMeta("−", "d", "Wastage")
    "adjust" -> MoveMeta("±", "", "Adjustment")
    else -> MoveMeta("", "", type)
}

@Composable
fun MoveRow(mv: StockMove, items: List<InventoryItem>) {
    val c = gav
    val item = items.firstOrNull { it.id == mv.itemId }
    val meta = metaOf(mv.moveType)
    val dotColor: Color = when (meta.kind) { "c" -> c.credit; "d" -> c.debit; else -> c.muted }
    val amtColor: Color = when (meta.kind) { "c" -> c.credit; "d" -> c.debit; else -> c.ink }

    Row(
        Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(dotColor))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(item?.name ?: "(item)", color = c.ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("— ${meta.label}", color = c.muted, fontSize = 11.sp)
            }
            Text(
                Fmt.fmtDateTime(mv.createdAt ?: mv.moveDate) +
                    (mv.note?.let { " · $it" } ?: "") +
                    (mv.createdBy?.let { " · $it" } ?: ""),
                color = c.muted, fontSize = 11.sp,
            )
        }
        Text(
            "${meta.sign}${trimQty(mv.qty)} ${item?.unit ?: ""}".trim(),
            color = amtColor, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
        )
    }
}

fun trimQty(d: Double): String =
    if (d == Math.floor(d) && !d.isInfinite()) d.toLong().toString()
    else (Math.round(d * 1000.0) / 1000.0).toString()
