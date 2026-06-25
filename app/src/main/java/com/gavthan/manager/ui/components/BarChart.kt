package com.gavthan.manager.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gavthan.manager.ui.theme.White
import com.gavthan.manager.ui.theme.gav
import kotlin.math.ceil
import kotlin.math.max

data class BarGroup(val label: String, val values: List<Float>)

@Composable
fun PeriodSelector(value: String, onChange: (String) -> Unit) {
    val c = gav
    FlowRowCompat {
        Charts.PERIODS.forEach { p ->
            val on = p.key == value
            Box(
                Modifier
                    .padding(end = 4.dp, bottom = 4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (on) c.accent else c.surface2)
                    .border(1.dp, if (on) c.accent else c.lineStrong, RoundedCornerShape(999.dp))
                    .clickable { onChange(p.key) }
                    .padding(horizontal = 11.dp, vertical = 4.dp),
            ) {
                Text(p.label, color = if (on) White else c.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowCompat(content: @Composable () -> Unit) {
    FlowRow(Modifier.fillMaxWidth()) { content() }
}

/**
 * Grouped bar chart drawn on a Compose Canvas (no chart dependency).
 * - [seriesColors] / [seriesNames] define the legend; each group holds one value per series.
 * - [barColorOverride] lets a single-series chart (stock levels) color each bar individually.
 */
@Composable
fun GroupedBarChart(
    groups: List<BarGroup>,
    seriesColors: List<Color>,
    seriesNames: List<String>? = null,
    barColorOverride: ((groupIndex: Int) -> Color)? = null,
    height: Dp = 200.dp,
    yFormatter: (Float) -> String = { v -> if (v >= 1000f) "${(v / 1000f).toInt()}k" else v.toInt().toString() },
) {
    val c = gav
    val tm = rememberTextMeasurer()
    val seriesCount = max(1, groups.maxOfOrNull { it.values.size } ?: 1)
    val maxVal = max(1f, groups.maxOfOrNull { g -> g.values.maxOrNull() ?: 0f } ?: 0f)
    val labelStyle = TextStyle(color = c.muted, fontSize = 10.sp)

    Column(Modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            val leftPad = 40.dp.toPx()
            val bottomPad = 22.dp.toPx()
            val topPad = 8.dp.toPx()
            val plotW = size.width - leftPad
            val plotH = size.height - bottomPad - topPad
            val baseY = topPad + plotH
            val headroom = maxVal * 1.1f

            // Y gridlines + labels
            val ticks = 4
            for (t in 0..ticks) {
                val frac = t.toFloat() / ticks
                val y = baseY - plotH * frac
                drawLine(c.line, Offset(leftPad, y), Offset(size.width, y), strokeWidth = 1f)
                val txt = yFormatter(headroom * frac)
                val measured = tm.measure(txt, labelStyle)
                drawText(tm, txt, Offset(leftPad - measured.size.width - 6.dp.toPx(), y - measured.size.height / 2f), labelStyle)
            }

            val n = groups.size
            if (n == 0) return@Canvas
            val groupW = plotW / n
            val innerPad = groupW * 0.18f
            val barArea = groupW - innerPad
            val rawBarW = barArea / seriesCount
            val barW = rawBarW.coerceAtMost(26.dp.toPx())
            val totalBarsW = barW * seriesCount
            val barGap = (barArea - totalBarsW) / max(1, seriesCount)

            val labelStep = max(1, ceil(n / 8.0).toInt())

            groups.forEachIndexed { gi, g ->
                val slotX = leftPad + groupW * gi
                val startX = slotX + (groupW - (totalBarsW + barGap * (seriesCount - 1))) / 2f
                for (s in 0 until seriesCount) {
                    val value = g.values.getOrElse(s) { 0f }
                    val h = if (headroom <= 0f) 0f else plotH * (value / headroom)
                    val x = startX + s * (barW + barGap)
                    val color = barColorOverride?.invoke(gi) ?: seriesColors.getOrElse(s) { c.accent }
                    if (h > 0f) {
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(x, baseY - h),
                            size = Size(barW, h),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                        )
                    }
                }
                if (gi % labelStep == 0 || gi == n - 1) {
                    val measured = tm.measure(g.label, labelStyle)
                    val cx = slotX + groupW / 2f - measured.size.width / 2f
                    drawText(tm, g.label, Offset(cx.coerceIn(0f, size.width - measured.size.width), baseY + 5.dp.toPx()), labelStyle)
                }
            }
        }

        if (seriesNames != null) {
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                seriesNames.forEachIndexed { i, name ->
                    Row(
                        Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(9.dp).clip(CircleShape).background(seriesColors.getOrElse(i) { c.accent }))
                        Spacer(Modifier.size(5.dp))
                        Text(name, color = c.muted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
