package com.gavthan.manager.util

import com.gavthan.manager.data.Config
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/** Formatting helpers that mirror the web app's en-IN locale output. */
object Fmt {
    val CUR = Config.CURRENCY
    private val IN = Locale("en", "IN")
    private val dDate = DateTimeFormatter.ofPattern("dd MMM yy", Locale.ENGLISH)
    private val dDateTime = DateTimeFormatter.ofPattern("dd MMM, hh:mm a", Locale.ENGLISH)
    private val dMonth = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

    /** ₹ + Indian-grouped amount, rounded to 2 decimals (trailing zeros trimmed). */
    fun money(n: Double): String {
        val cents = Math.round(abs(n) * 100.0)
        val intPart = cents / 100
        val frac = (cents % 100).toInt()
        val grouped = groupIndian(intPart)
        val dec = when {
            frac == 0 -> ""
            frac % 10 == 0 -> "." + (frac / 10)
            else -> "." + frac.toString().padStart(2, '0')
        }
        val sign = if (n < 0) "-" else ""
        return CUR + sign + grouped + dec
    }

    fun absMoney(n: Double): String = money(abs(n))

    private fun groupIndian(n: Long): String {
        val s = n.toString()
        if (s.length <= 3) return s
        val last3 = s.takeLast(3)
        var rest = s.dropLast(3)
        val parts = ArrayList<String>()
        while (rest.length > 2) {
            parts.add(0, rest.takeLast(2))
            rest = rest.dropLast(2)
        }
        if (rest.isNotEmpty()) parts.add(0, rest)
        return parts.joinToString(",") + "," + last3
    }

    fun initials(s: String?): String {
        val parts = (s ?: "?").trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val ini = parts.take(2).mapNotNull { it.firstOrNull() }.joinToString("")
        return (ini.ifEmpty { "?" }).uppercase()
    }

    /** Uppercase the first letter of each word — matches JS `replace(/\b\w/g, …)`. */
    fun initCap(s: String?): String {
        val str = s ?: ""
        val sb = StringBuilder(str.length)
        var atBoundary = true
        for (c in str) {
            val word = c.isLetterOrDigit()
            sb.append(if (atBoundary && word) c.uppercaseChar() else c)
            atBoundary = !word
        }
        return sb.toString()
    }

    /** Resolve a creator email to a display name using the users map. */
    fun dispName(key: String?, map: Map<String, String>): String {
        if (key.isNullOrEmpty()) return ""
        map[key]?.let { return it }
        return if (key.contains("@")) initCap(key.substringBefore("@")) else key
    }

    fun todayIso(): String = LocalDate.now().toString()

    fun fmtDate(d: String?): String {
        if (d.isNullOrBlank()) return ""
        return runCatching { LocalDate.parse(d.take(10)).format(dDate) }.getOrNull() ?: d
    }

    fun fmtDateTime(d: String?): String {
        if (d.isNullOrBlank()) return ""
        return runCatching {
            val odt = parseInstant(d) ?: return@runCatching d
            odt.atZoneSameInstant(ZoneId.systemDefault()).format(dDateTime)
        }.getOrNull() ?: d
    }

    fun monthLabel(m: String?): String {
        if (m.isNullOrBlank()) return ""
        return runCatching { YearMonth.parse(m.take(7)).format(dMonth) }.getOrNull() ?: m
    }

    /** "2026-06-25" -> "25/06/26" (web's list cell format). */
    fun dmy(iso: String?): String {
        val p = (iso ?: "").split("-")
        return if (p.size == 3) "${p[2]}/${p[1]}/${p[0].takeLast(2)}" else (iso ?: "")
    }

    private fun parseInstant(s: String): OffsetDateTime? {
        runCatching { return OffsetDateTime.parse(s) }
        runCatching {
            val normalized = if (s.endsWith("Z")) s else if (s.contains("+") || s.contains("T")) s else "${s}T00:00:00Z"
            return OffsetDateTime.parse(normalized)
        }
        runCatching {
            return LocalDate.parse(s.take(10)).atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime()
        }
        return null
    }
}
