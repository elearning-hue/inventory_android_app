package com.gavthan.manager.ui.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gavthan.manager.ui.components.AppBottomSheet
import com.gavthan.manager.ui.components.Banner
import com.gavthan.manager.ui.components.BannerTone
import com.gavthan.manager.ui.components.StatTile
import com.gavthan.manager.ui.components.Tone
import com.gavthan.manager.ui.theme.gav
import com.gavthan.manager.util.Fmt
import kotlin.math.abs

@Composable
fun ReconSheet(
    rows: List<BillRow>,
    importedRefs: Set<String>,
    live: Double,
    imported: Double,
    onClose: () -> Unit,
) {
    val c = gav
    val sorted = rows.sortedBy { it.billNo?.toIntOrNull() ?: 0 }

    AppBottomSheet("Sales reconciliation", onClose) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("On record", Fmt.money(live), Tone.Neutral, Modifier.weight(1f))
            StatTile("Imported", Fmt.money(imported), Tone.Neutral, Modifier.weight(1f))
        }
        if (live != imported) {
            Spacer(Modifier.height(10.dp))
            Banner(
                "Difference ${Fmt.money(abs(live - imported))}. Rows in red read as ₹0 — the amount couldn't be parsed from that bill.",
                BannerTone.Info,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text("${rows.size} settled bills (oldest first)", color = c.muted, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        sorted.forEach { b ->
            val zero = b.amount <= 0
            Row(
                Modifier.fillMaxWidth().padding(vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(if (zero) c.debit else c.credit))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(b.billNo?.let { "#$it" } ?: "(no number)", color = c.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(b.name ?: "", color = c.muted, fontSize = 12.sp)
                    }
                    Text(
                        "${Fmt.fmtDate(b.date)} · ${if (b.id in importedRefs) "imported" else "not imported"}",
                        color = c.muted, fontSize = 12.sp,
                    )
                }
                Text(Fmt.money(b.amount), color = if (zero) c.debit else c.ink, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
            HorizontalDivider(color = c.line)
        }
    }
}
