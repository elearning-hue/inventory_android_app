package com.gavthan.manager.ui.ledger

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gavthan.manager.data.LedgerEntry
import com.gavthan.manager.ui.components.BarGroup
import com.gavthan.manager.ui.components.CardHeader
import com.gavthan.manager.ui.components.Charts
import com.gavthan.manager.ui.components.EmptyState
import com.gavthan.manager.ui.components.GroupedBarChart
import com.gavthan.manager.ui.components.LiveBadge
import com.gavthan.manager.ui.components.PeriodSelector
import com.gavthan.manager.ui.components.SectionCard
import com.gavthan.manager.ui.theme.gav

@Composable
fun LedgerDash(entries: List<LedgerEntry>, live: Boolean) {
    val c = gav
    var period by rememberSaveable { mutableStateOf("month") }
    val def = Charts.def(period)

    val groups = remember(entries, period) {
        val b = Charts.buckets(def)
        val income = FloatArray(b.keys.size)
        val expense = FloatArray(b.keys.size)
        entries.forEach { e ->
            val key = Charts.keyOf(e.entryDate ?: e.createdAt, b.unit)
            val idx = b.index[key] ?: return@forEach
            if (e.kind == "expense") expense[idx] += e.amount.toFloat() else income[idx] += e.amount.toFloat()
        }
        b.labels.indices.map { BarGroup(b.labels[it], listOf(income[it], expense[it])) }
    }
    val hasData = groups.any { g -> g.values.any { it > 0f } }

    SectionCard {
        CardHeader("Cashflow · ${def.title}") { LiveBadge(live) }
        HorizontalDivider(color = c.line)
        Column(Modifier.padding(14.dp)) {
            PeriodSelector(period) { period = it }
            Spacer(Modifier.height(6.dp))
            if (!hasData) {
                EmptyState("📈", "No entries in this period yet")
            } else {
                GroupedBarChart(
                    groups = groups,
                    seriesColors = listOf(c.credit, c.debit),
                    seriesNames = listOf("Income", "Expense"),
                )
            }
        }
    }
}
