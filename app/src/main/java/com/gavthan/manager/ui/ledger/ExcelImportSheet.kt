package com.gavthan.manager.ui.ledger

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gavthan.manager.data.AppUser
import com.gavthan.manager.data.LedgerRepo
import com.gavthan.manager.data.ledgerRow
import com.gavthan.manager.ui.components.AppBottomSheet
import com.gavthan.manager.ui.components.AppButton
import com.gavthan.manager.ui.components.AppDropdown
import com.gavthan.manager.ui.components.Banner
import com.gavthan.manager.ui.components.BannerTone
import com.gavthan.manager.ui.components.BtnStyle
import com.gavthan.manager.ui.components.FieldLabel
import com.gavthan.manager.ui.components.Pill
import com.gavthan.manager.ui.components.Segmented
import com.gavthan.manager.ui.components.StatTile
import com.gavthan.manager.ui.components.Toaster
import com.gavthan.manager.ui.components.Tone
import com.gavthan.manager.ui.theme.gav
import com.gavthan.manager.util.Exporter
import com.gavthan.manager.util.Fmt
import com.gavthan.manager.util.Spreadsheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class ParsedRow(val date: String, val desc: String, val user: String, val cat: String, val dr: Double, val cr: Double)
private data class ParseResult(val rows: List<ParsedRow>, val error: String?)

@Composable
fun ExcelImportSheet(
    users: List<AppUser>,
    userMap: Map<String, String>,
    meEmail: String,
    onClose: () -> Unit,
    onSaved: () -> Unit,
) {
    val c = gav
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf("pick") }
    var fileName by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf<List<ParsedRow>>(emptyList()) }
    var mapping by remember { mutableStateOf("dr_income") }
    var incCat by remember { mutableStateOf("Other income") }
    var expCat by remember { mutableStateOf("Misc") }
    var err by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    val nameToEmail = remember(users) {
        val m = HashMap<String, String>()
        users.forEach { u ->
            u.displayName?.let { m[it.lowercase().trim()] = u.email }
            if (u.email.isNotBlank()) m[u.email.lowercase().trim()] = u.email
        }
        m
    }
    fun resolveUser(s: String): String {
        if (s.isBlank()) return ""
        return nameToEmail[s.lowercase().trim()] ?: s.trim()
    }

    val drIsIncome = mapping == "dr_income"
    var inc = 0.0; var exp = 0.0; var skip = 0
    rows.forEach { r ->
        if (r.dr > 0) { if (drIsIncome) inc += r.dr else exp += r.dr }
        if (r.cr > 0) { if (drIsIncome) exp += r.cr else inc += r.cr }
        if (r.dr <= 0 && r.cr <= 0) skip++
    }

    fun buildEntries() = buildList {
        rows.forEach { r ->
            val d = r.date.ifBlank { Fmt.todayIso() }
            val creator = resolveUser(r.user).ifBlank { meEmail }
            fun mk(kind: String, amt: Double) = ledgerRow(
                entryType = kind, amount = amt,
                category = r.cat.ifBlank { if (kind == "income") incCat else expCat },
                note = r.desc.ifBlank { r.cat.ifBlank { if (kind == "income") incCat else expCat } },
                entryDate = d, mode = null, source = "excel", createdBy = creator,
            )
            if (r.dr > 0) add(mk(if (drIsIncome) "income" else "expense", r.dr))
            if (r.cr > 0) add(mk(if (drIsIncome) "expense" else "income", r.cr))
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            err = ""
            val bytes = withContext(Dispatchers.IO) { Exporter.readUri(ctx, uri) }
            if (bytes == null) { err = "Couldn't read the file."; return@launch }
            fileName = displayName(ctx, uri) ?: "spreadsheet"
            val res = withContext(Dispatchers.Default) { parseLedgerSheet(bytes) }
            if (res.error != null) { err = res.error; return@launch }
            rows = res.rows
            step = "preview"
        }
    }

    fun confirm() {
        val list = buildEntries()
        if (list.isEmpty()) { err = "No valid amounts to import."; return }
        busy = true; err = ""
        scope.launch {
            runCatching {
                list.chunked(200).forEach { chunk -> LedgerRepo.insert(chunk) }
            }.onSuccess {
                Toaster.show("Imported ${list.size} entries"); onSaved()
            }.onFailure {
                err = it.message ?: "Import failed"; busy = false
            }
        }
    }

    AppBottomSheet("Import ledger from Excel", onClose) {
        if (err.isNotEmpty()) { Banner(err, BannerTone.Info); Spacer(Modifier.height(4.dp)) }

        if (step == "pick") {
            Spacer(Modifier.height(4.dp))
            Text(
                "Upload an .xlsx / .xls / .csv file. The first sheet should have a header row including Date, Description, User, and Dr / Cr (or a single Amount) columns. Column order doesn't matter.",
                color = c.muted, fontSize = 12.sp,
            )
            FieldLabel("Spreadsheet file")
            AppButton("⬆ Choose file", { picker.launch("*/*") }, Modifier.fillMaxWidth(), BtnStyle.Secondary)
        } else {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("📄 $fileName · ${rows.size} rows", color = c.muted, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                AppButton("Change file", { step = "pick"; rows = emptyList(); fileName = "" }, Modifier, BtnStyle.Ghost, small = true)
            }
            FieldLabel("Column mapping")
            Segmented(
                options = listOf("dr_income" to "Dr = Income", "cr_income" to "Cr = Income"),
                selected = mapping, onSelect = { mapping = it },
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    FieldLabel("Default income category")
                    AppDropdown(incCat, INCOME_CATS.map { it to it }, { incCat = it })
                }
                Column(Modifier.weight(1f)) {
                    FieldLabel("Default expense category")
                    AppDropdown(expCat, EXPENSE_CATS.map { it to it }, { expCat = it })
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Each row uses its own Category column when present; the defaults fill in only where a row has none. Entries are attributed to the row's user.",
                color = c.muted, fontSize = 12.sp,
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Income", Fmt.money(inc), Tone.Green, Modifier.weight(1f))
                StatTile("Expense", Fmt.money(exp), Tone.Red, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Preview (first 8 of ${rows.size})" + (if (skip > 0) " · $skip rows have no amount and will be skipped" else ""),
                color = c.muted, fontSize = 12.sp,
            )
            rows.take(8).forEach { r ->
                Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(r.desc.ifBlank { "—" }, color = c.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "${Fmt.fmtDate(r.date).ifBlank { "(no date)" }} · ${Fmt.dispName(resolveUser(r.user), userMap).ifBlank { "unattributed" }}",
                                color = c.muted, fontSize = 11.sp,
                            )
                            if (r.cat.isNotBlank()) Pill(r.cat)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        if (r.dr > 0) Text("Dr ${Fmt.money(r.dr)}", color = if (drIsIncome) c.credit else c.debit, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        if (r.cr > 0) Text("Cr ${Fmt.money(r.cr)}", color = if (drIsIncome) c.debit else c.credit, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    }
                }
                HorizontalDivider(color = c.line)
            }
            Spacer(Modifier.height(12.dp))
            Banner(
                "Re-running an import adds new rows. To redo, an admin can clear prior Excel imports in Supabase: delete from mh_ledger where source='excel';",
                BannerTone.Net,
            )
            Spacer(Modifier.height(12.dp))
            AppButton(
                text = if (busy) "Importing…" else "Import ${buildEntries().size} entries",
                onClick = { confirm() },
                loading = busy,
            )
        }
    }
}

/* ---------- parsing (port of the web ExcelImportSheet.onFile) ---------- */

private fun parseLedgerSheet(bytes: ByteArray): ParseResult {
    val aoa = Spreadsheet.read(bytes)
    if (aoa.isEmpty()) return ParseResult(emptyList(), "That sheet looks empty.")

    // locate header row in the first 6 rows
    var hi = 0
    for (i in 0 until minOf(6, aoa.size)) {
        val r = aoa[i].map { it.text.lowercase() }
        val hasDate = r.any { Regex("date").containsMatchIn(it) }
        val hasAmt = r.any { Regex("dr|debit|cr|credit|amount").containsMatchIn(it) }
        if (hasDate && hasAmt) { hi = i; break }
    }
    val hdr = aoa.getOrElse(hi) { emptyList() }.map { it.text.lowercase().trim() }
    fun find(re: Regex) = hdr.indexOfFirst { re.containsMatchIn(it) }
    val ciDate = find(Regex("date|dt"))
    val ciDesc = find(Regex("desc|particular|narration|detail|note|remark"))
    val ciUser = find(Regex("account|user|staff|\\bby\\b|name|cashier|emp"))
    val ciCat = find(Regex("categ|head|group"))
    val ciDr = find(Regex("^dr$|debit"))
    val ciCr = find(Regex("^cr$|credit"))
    val ciAmt = find(Regex("amount|amt"))

    if (ciDr < 0 && ciCr < 0 && ciAmt < 0) {
        return ParseResult(emptyList(), "Couldn't find a Dr/Cr or Amount column. Headers seen: " + hdr.filter { it.isNotBlank() }.joinToString(", "))
    }

    fun cellAt(row: List<Spreadsheet.Cell>, idx: Int) = if (idx in row.indices) row[idx] else Spreadsheet.Cell("", null)
    fun numCell(cell: Spreadsheet.Cell) = cell.num ?: cell.text.trim().toDoubleOrNull() ?: 0.0

    val out = ArrayList<ParsedRow>()
    for (i in hi + 1 until aoa.size) {
        val row = aoa[i]
        val date = if (ciDate >= 0) Spreadsheet.parseDate(cellAt(row, ciDate)) else ""
        val desc = if (ciDesc >= 0) cellAt(row, ciDesc).text.trim() else ""
        val user = if (ciUser >= 0) cellAt(row, ciUser).text.trim() else ""
        val cat = if (ciCat >= 0) cellAt(row, ciCat).text.trim() else ""
        var dr = if (ciDr >= 0) numCell(cellAt(row, ciDr)) else 0.0
        val cr = if (ciCr >= 0) numCell(cellAt(row, ciCr)) else 0.0
        if (ciDr < 0 && ciCr < 0 && ciAmt >= 0) dr = numCell(cellAt(row, ciAmt))
        if (date.isBlank() && desc.isBlank() && dr == 0.0 && cr == 0.0) continue
        out.add(ParsedRow(date, desc, user, cat, dr, cr))
    }
    if (out.isEmpty()) return ParseResult(emptyList(), "No data rows found below the header.")
    return ParseResult(out, null)
}

private fun displayName(ctx: Context, uri: Uri): String? = runCatching {
    ctx.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
    }
}.getOrNull()
