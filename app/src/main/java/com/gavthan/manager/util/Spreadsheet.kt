package com.gavthan.manager.util

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Dependency-free spreadsheet I/O. Reads .xlsx (OOXML zip) and .csv into typed
 * cells, and writes a valid .xlsx — replacing the web app's SheetJS (xlsx) usage
 * with zero fragile native dependencies.
 */
object Spreadsheet {

    /** A cell carries its display text plus a numeric value when the source was numeric. */
    data class Cell(val text: String, val num: Double?)

    /* =============================== READ =============================== */

    fun read(bytes: ByteArray): List<List<Cell>> {
        val isZip = bytes.size > 2 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()
        return if (isZip) readXlsx(bytes) else readCsv(String(bytes, Charsets.UTF_8))
    }

    private fun readCsv(textIn: String): List<List<Cell>> {
        val text = textIn.removePrefix("﻿")
        val rows = ArrayList<List<Cell>>()
        var row = ArrayList<Cell>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        fun pushField() { row.add(Cell(field.toString(), null)); field.setLength(0) }
        fun pushRow() { pushField(); rows.add(row); row = ArrayList() }
        while (i < text.length) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') { field.append('"'); i++ } else inQuotes = false
                } else field.append(c)
            } else when (c) {
                '"' -> inQuotes = true
                ',' -> pushField()
                '\n' -> pushRow()
                '\r' -> { /* handled by following \n */ }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) pushRow()
        return rows
    }

    private fun readXlsx(bytes: ByteArray): List<List<Cell>> {
        val entries = HashMap<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var e = zis.nextEntry
            while (e != null) {
                if (!e.isDirectory) entries[e.name] = zis.readBytes()
                zis.closeEntry()
                e = zis.nextEntry
            }
        }
        val shared = entries["xl/sharedStrings.xml"]?.let { parseSharedStrings(it) } ?: emptyList()
        val sheetKey = when {
            entries.containsKey("xl/worksheets/sheet1.xml") -> "xl/worksheets/sheet1.xml"
            else -> entries.keys.filter {
                it.startsWith("xl/worksheets/sheet") && it.endsWith(".xml")
            }.minOrNull()
        } ?: return emptyList()
        return parseSheet(entries[sheetKey]!!, shared)
    }

    private fun parseSharedStrings(data: ByteArray): List<String> {
        val list = ArrayList<String>()
        val p = Xml.newPullParser()
        p.setInput(ByteArrayInputStream(data), null)
        var cur: StringBuilder? = null
        var inT = false
        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            when (ev) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "si" -> cur = StringBuilder()
                    "t" -> inT = true
                }
                XmlPullParser.TEXT -> if (inT) cur?.append(p.text)
                XmlPullParser.END_TAG -> when (p.name) {
                    "t" -> inT = false
                    "si" -> { list.add(cur?.toString() ?: ""); cur = null }
                }
            }
            ev = p.next()
        }
        return list
    }

    private fun parseSheet(data: ByteArray, shared: List<String>): List<List<Cell>> {
        val rows = ArrayList<List<Cell>>()
        val p = Xml.newPullParser()
        p.setInput(ByteArrayInputStream(data), null)
        var ev = p.eventType
        var curRow: ArrayList<Cell>? = null
        var cellType: String? = null
        var cellRef: String? = null
        var inV = false
        var inT = false
        val value = StringBuilder()
        while (ev != XmlPullParser.END_DOCUMENT) {
            when (ev) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "row" -> curRow = ArrayList()
                    "c" -> {
                        cellType = p.getAttributeValue(null, "t")
                        cellRef = p.getAttributeValue(null, "r")
                        value.setLength(0)
                    }
                    "v" -> inV = true
                    "t" -> inT = true
                }
                XmlPullParser.TEXT ->
                    if (inV) value.append(p.text)
                    else if (inT && cellType == "inlineStr") value.append(p.text)
                XmlPullParser.END_TAG -> when (p.name) {
                    "v" -> inV = false
                    "t" -> inT = false
                    "c" -> {
                        val r = curRow ?: ArrayList<Cell>().also { curRow = it }
                        val idx = cellRef?.let { colFromRef(it) } ?: r.size
                        while (r.size < idx) r.add(Cell("", null))
                        val raw = value.toString()
                        val cell = when (cellType) {
                            "s" -> Cell(raw.trim().toIntOrNull()?.let { shared.getOrNull(it) } ?: "", null)
                            "inlineStr", "str" -> Cell(raw, null)
                            "b" -> Cell(if (raw.trim() == "1") "TRUE" else "FALSE", null)
                            else -> Cell(raw.trim(), raw.trim().toDoubleOrNull())
                        }
                        r.add(cell)
                    }
                    "row" -> { rows.add(curRow ?: ArrayList()); curRow = null }
                }
            }
            ev = p.next()
        }
        return rows
    }

    private fun colFromRef(ref: String): Int {
        var n = 0
        for (c in ref) {
            if (c.isLetter()) n = n * 26 + (c.uppercaseChar() - 'A' + 1) else break
        }
        return (n - 1).coerceAtLeast(0)
    }

    /* ============================== DATES ============================== */

    /** Port of the web app's parseDate(): Excel serials, dd/mm/yyyy, ISO. */
    fun parseDate(cell: Cell): String {
        cell.num?.let { serial ->
            val millis = Math.round((serial - 25569.0) * 86400000.0)
            return runCatching {
                Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
            }.getOrDefault("")
        }
        return parseDateText(cell.text)
    }

    fun parseDateText(raw: String): String {
        val s = raw.trim()
        if (s.isEmpty()) return ""
        val m = Regex("^(\\d{1,2})[/\\-.](\\d{1,2})[/\\-.](\\d{2,4})$").find(s)
        if (m != null) {
            val dd = m.groupValues[1].toInt()
            val mm = m.groupValues[2].toInt()
            var yy = m.groupValues[3].toInt()
            if (yy < 100) yy += 2000
            return runCatching { LocalDate.of(yy, mm, dd).toString() }.getOrDefault("")
        }
        runCatching { return LocalDate.parse(s.take(10)).toString() }
        runCatching { return OffsetDateTime.parse(s).toLocalDate().toString() }
        return ""
    }

    /* =============================== WRITE ============================== */

    fun writeXlsx(rows: List<List<Any?>>, sheetName: String): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zos ->
            fun put(name: String, content: String) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            put("[Content_Types].xml", CONTENT_TYPES)
            put("_rels/.rels", RELS)
            put("xl/workbook.xml", workbookXml(sheetName.take(31).ifBlank { "Sheet1" }))
            put("xl/_rels/workbook.xml.rels", WB_RELS)
            put("xl/styles.xml", STYLES)
            put("xl/worksheets/sheet1.xml", sheetXml(rows))
        }
        return bos.toByteArray()
    }

    fun writeCsv(rows: List<List<Any?>>): ByteArray {
        val sb = StringBuilder("﻿")
        rows.forEach { row ->
            sb.append(row.joinToString(",") { csvEscape(it?.toString() ?: "") })
            sb.append("\n")
        }
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    private fun csvEscape(s: String): String =
        if (s.contains('"') || s.contains(',') || s.contains('\n'))
            "\"" + s.replace("\"", "\"\"") + "\"" else s

    private fun sheetXml(rows: List<List<Any?>>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
        rows.forEachIndexed { rIdx, row ->
            val rowNum = rIdx + 1
            sb.append("<row r=\"$rowNum\">")
            row.forEachIndexed { cIdx, value ->
                val ref = colLetter(cIdx) + rowNum
                when (value) {
                    null -> {}
                    is Number -> sb.append("<c r=\"$ref\"><v>${numToStr(value)}</v></c>")
                    else -> {
                        val s = value.toString()
                        if (s.isNotEmpty())
                            sb.append("<c r=\"$ref\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${xmlEscape(s)}</t></is></c>")
                    }
                }
            }
            sb.append("</row>")
        }
        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun colLetter(index: Int): String {
        var i = index
        val sb = StringBuilder()
        while (true) {
            sb.insert(0, ('A' + (i % 26)))
            i = i / 26 - 1
            if (i < 0) break
        }
        return sb.toString()
    }

    private fun numToStr(n: Number): String {
        val d = n.toDouble()
        return if (d == Math.floor(d) && !d.isInfinite()) d.toLong().toString() else d.toString()
    }

    private fun xmlEscape(s: String): String = buildString {
        for (c in s) when (c) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&apos;")
            else -> if (c.code < 0x20 && c != '\t' && c != '\n' && c != '\r') append(' ') else append(c)
        }
    }

    private fun workbookXml(name: String) =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" """ +
            """xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""" +
            """<sheets><sheet name="${xmlEscape(name)}" sheetId="1" r:id="rId1"/></sheets></workbook>"""

    private const val CONTENT_TYPES =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""" +
            """<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""" +
            """<Default Extension="xml" ContentType="application/xml"/>""" +
            """<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""" +
            """<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""" +
            """<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""" +
            """</Types>"""

    private const val RELS =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
            """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>""" +
            """</Relationships>"""

    private const val WB_RELS =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
            """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>""" +
            """<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""" +
            """</Relationships>"""

    private const val STYLES =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""" +
            """<fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts>""" +
            """<fills count="1"><fill><patternFill patternType="none"/></fill></fills>""" +
            """<borders count="1"><border/></borders>""" +
            """<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>""" +
            """<cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/></cellXfs>""" +
            """</styleSheet>"""
}
