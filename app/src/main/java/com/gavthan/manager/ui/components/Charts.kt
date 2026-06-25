package com.gavthan.manager.ui.components

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Shared time-series bucketing for the dashboards (ported from the web app). */
object Charts {

    data class Period(
        val key: String,
        val label: String,
        val title: String,
        val days: Int?,
        val months: Int?,
        val unit: String, // "day" | "month"
    )

    val PERIODS = listOf(
        Period("week", "1W", "last 7 days", 7, null, "day"),
        Period("15", "15D", "last 15 days", 15, null, "day"),
        Period("month", "1M", "last 30 days", 30, null, "day"),
        Period("6m", "6M", "last 6 months", null, 6, "month"),
        Period("year", "1Y", "last 12 months", null, 12, "month"),
    )

    fun def(key: String): Period = PERIODS.firstOrNull { it.key == key } ?: PERIODS[2]

    data class Buckets(
        val keys: List<String>,
        val labels: List<String>,
        val index: Map<String, Int>,
        val unit: String,
    )

    private val dayFmt = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH)
    private val monthFmt = DateTimeFormatter.ofPattern("MMM yy", Locale.ENGLISH)

    fun buckets(def: Period): Buckets {
        val today = LocalDate.now()
        val keys = ArrayList<String>()
        val labels = ArrayList<String>()
        val index = HashMap<String, Int>()
        if (def.unit == "day") {
            val n = def.days ?: 30
            for (i in n - 1 downTo 0) {
                val d = today.minusDays(i.toLong())
                val k = d.toString()
                index[k] = keys.size
                keys.add(k)
                labels.add(d.format(dayFmt))
            }
        } else {
            val n = def.months ?: 6
            val ym0 = YearMonth.from(today)
            for (i in n - 1 downTo 0) {
                val ym = ym0.minusMonths(i.toLong())
                val k = ym.toString()
                index[k] = keys.size
                keys.add(k)
                labels.add(ym.format(monthFmt))
            }
        }
        return Buckets(keys, labels, index, def.unit)
    }

    fun keyOf(dateStr: String?, unit: String): String =
        if (unit == "day") (dateStr ?: "").take(10) else (dateStr ?: "").take(7)
}
