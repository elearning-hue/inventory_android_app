package com.gavthan.manager.ui.inventory

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gavthan.manager.data.InventoryItem
import com.gavthan.manager.data.StockMove
import com.gavthan.manager.ui.components.BarGroup
import com.gavthan.manager.ui.components.CardHeader
import com.gavthan.manager.ui.components.Charts
import com.gavthan.manager.ui.components.GroupedBarChart
import com.gavthan.manager.ui.components.LiveBadge
import com.gavthan.manager.ui.components.PeriodSelector
import com.gavthan.manager.ui.components.SectionCard
import com.gavthan.manager.ui.theme.gav

@Composable
fun InventoryDash(items: List<InventoryItem>, moves: List<StockMove>, live: Boolean) {
    val c = gav
    var period by rememberSaveable { mutableStateOf("week") }
    val def = Charts.def(period)

    val levels = remember(items) {
        items.sortedByDescending { it.qty }.take(8)
    }
    val levelGroups = remember(levels) {
        levels.map { BarGroup(truncate(it.name), listOf(it.qty.toFloat())) }
    }

    val activity = remember(moves, period) {
        val b = Charts.buckets(def)
        val inArr = FloatArray(b.keys.size)
        val outArr = FloatArray(b.keys.size)
        moves.forEach { mv ->
            val key = Charts.keyOf(mv.createdAt ?: mv.moveDate, b.unit)
            val idx = b.index[key] ?: return@forEach
            val qy = mv.qty.toFloat()
            when (mv.moveType) {
                "in" -> inArr[idx] += qy
                "out", "waste" -> outArr[idx] += qy
                "adjust" -> if (qy >= 0) inArr[idx] += qy else outArr[idx] += -qy
            }
        }
        b.labels.indices.map { BarGroup(b.labels[it], listOf(inArr[it], outArr[it])) }
    }
    val hasActivity = activity.any { g -> g.values.any { it > 0f } }

    SectionCard {
        CardHeader("Live stock levels") { LiveBadge(live) }
        HorizontalDivider(color = c.line)
        Column(Modifier.padding(14.dp)) {
            if (levels.isEmpty()) {
                com.gavthan.manager.ui.components.EmptyState("📦", "No items yet")
            } else {
                GroupedBarChart(
                    groups = levelGroups,
                    seriesColors = listOf(c.accent),
                    barColorOverride = { i -> if (levels[i].isLow) c.debit else c.accent },
                )
                Spacer(Modifier.height(8.dp))
                Text("Bars in red are at or below reorder level.", color = c.muted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(16.dp))
            Text("MOVEMENT · ${def.title}", color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            PeriodSelector(period) { period = it }
            Spacer(Modifier.height(6.dp))
            if (!hasActivity) {
                com.gavthan.manager.ui.components.EmptyState("📊", "No stock movement in this period")
            } else {
                GroupedBarChart(
                    groups = activity,
                    seriesColors = listOf(c.credit, c.debit),
                    seriesNames = listOf("In", "Out"),
                    height = 170.dp,
                )
            }
        }
    }
}

private fun truncate(s: String): String = if (s.length > 9) s.take(8) + "…" else s
