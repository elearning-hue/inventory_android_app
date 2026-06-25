package com.gavthan.manager.ui.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gavthan.manager.data.AppUser
import com.gavthan.manager.data.Bill
import com.gavthan.manager.data.LedgerEntry
import com.gavthan.manager.data.LedgerRepo
import com.gavthan.manager.data.ledgerRow
import com.gavthan.manager.ui.components.AppButton
import com.gavthan.manager.ui.components.AppDropdown
import com.gavthan.manager.ui.components.BtnStyle
import com.gavthan.manager.ui.components.CardHeader
import com.gavthan.manager.ui.components.DateField
import com.gavthan.manager.ui.components.EmptyState
import com.gavthan.manager.ui.components.FieldLabel
import com.gavthan.manager.ui.components.SearchField
import com.gavthan.manager.ui.components.SectionCard
import com.gavthan.manager.ui.components.Segmented
import com.gavthan.manager.ui.components.StatTile
import com.gavthan.manager.ui.components.Toaster
import com.gavthan.manager.ui.components.Tone
import com.gavthan.manager.ui.theme.White
import com.gavthan.manager.ui.theme.gav
import com.gavthan.manager.util.Exporter
import com.gavthan.manager.util.Fmt
import com.gavthan.manager.util.Spreadsheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

data class BillRow(
    val id: String, val billNo: String?, val name: String?, val addedBy: String?,
    val date: String, val amount: Double,
)

private const val PAGE_SIZE = 10

@Composable
fun LedgerScreen(me: AppUser?, online: Boolean) {
    val c = gav
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val vm: LedgerViewModel = viewModel()

    val data by vm.data.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val live by vm.live.collectAsStateWithLifecycle()

    val meEmail = me?.email ?: "staff"
    val isAdmin = me?.isAdmin == true

    val entries = data.entries
    val users = data.users
    val bills = data.bills

    // ----- filter state -----
    var mode by rememberSaveable { mutableStateOf("month") }
    var month by rememberSaveable { mutableStateOf(LocalDate.now().toString().take(7)) }
    var year by rememberSaveable { mutableStateOf(LocalDate.now().year.toString()) }
    var dFrom by rememberSaveable { mutableStateOf("") }
    var dTo by rememberSaveable { mutableStateOf("") }
    var staff by rememberSaveable { mutableStateOf("all") }
    var catF by rememberSaveable { mutableStateOf("all") }
    var kind by rememberSaveable { mutableStateOf("all") }
    var q by rememberSaveable { mutableStateOf("") }
    var page by rememberSaveable { mutableIntStateOf(0) }

    // ----- sheet state -----
    var showEntry by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    var showRecon by remember { mutableStateOf(false) }
    var showExcel by remember { mutableStateOf(false) }
    var excelOpen by rememberSaveable { mutableStateOf(false) }

    val userMap = remember(users) {
        users.filter { it.email.isNotBlank() }
            .associate { it.email to Fmt.initCap(it.displayName ?: it.email.substringBefore("@")) }
    }

    fun dateOf(e: LedgerEntry) = e.dateKey
    fun inPeriod(d: String): Boolean = when (mode) {
        "all" -> true
        "month" -> d.take(7) == month
        "year" -> d.take(4) == year
        "dates" -> (dFrom.isBlank() || d >= dFrom) && (dTo.isBlank() || d <= dTo)
        else -> true
    }
    fun matchStaff(e: LedgerEntry) = staff == "all" || e.createdBy == staff
    fun matchCat(e: LedgerEntry) = catF == "all" || (e.category ?: "") == catF
    fun matchText(e: LedgerEntry): Boolean {
        val t = q.trim()
        if (t.isEmpty()) return true
        return ((e.note ?: "") + " " + (e.category ?: "")).lowercase().contains(t.lowercase())
    }

    val months = remember(entries) {
        (entries.map { dateOf(it).take(7) }.filter { it.isNotBlank() }.toSet() + LocalDate.now().toString().take(7))
            .sortedDescending()
    }
    val years = remember(entries) {
        (entries.map { dateOf(it).take(4) }.filter { it.isNotBlank() }.toSet() + LocalDate.now().year.toString())
            .sortedDescending()
    }
    val staffOpts = remember(users, entries, userMap) {
        val opts = users.map { it.email to Fmt.initCap(it.displayName ?: it.email.substringBefore("@")) }.toMutableList()
        val known = opts.map { it.first }.toMutableSet()
        entries.forEach { e ->
            val cb = e.createdBy
            if (!cb.isNullOrBlank() && cb !in known) {
                known.add(cb); opts.add(cb to Fmt.dispName(cb, userMap))
            }
        }
        opts.sortedBy { it.second.lowercase() }
    }
    val catOpts = remember(entries) {
        (entries.mapNotNull { it.category }.toSet() + INCOME_CATS + EXPENSE_CATS).sorted()
    }

    val periodEntries = remember(entries, mode, month, year, dFrom, dTo, staff, catF, q) {
        entries.filter { inPeriod(dateOf(it)) && matchStaff(it) && matchCat(it) && matchText(it) }
    }
    val income = remember(periodEntries) { periodEntries.filter { it.kind == "income" }.sumOf { it.amount } }
    val expense = remember(periodEntries) { periodEntries.filter { it.kind == "expense" }.sumOf { it.amount } }
    val net = income - expense
    val shown = remember(periodEntries, kind) { periodEntries.filter { kind == "all" || it.kind == kind } }
    val chartBase = remember(entries, staff, catF, q) {
        entries.filter { matchStaff(it) && matchCat(it) && matchText(it) }
    }

    // pagination
    val totalPages = maxOf(1, (shown.size + PAGE_SIZE - 1) / PAGE_SIZE)
    val safePage = minOf(page, totalPages - 1).coerceAtLeast(0)
    val pageItems = remember(shown, safePage) { shown.drop(safePage * PAGE_SIZE).take(PAGE_SIZE) }
    LaunchedEffect(mode, month, year, dFrom, dTo, staff, catF, kind, q) { page = 0 }

    // ----- sales reconciliation -----
    val importedRefs = remember(entries) {
        entries.filter { it.source == "bill" && it.ref != null }.map { it.ref!! }.toSet()
    }
    val billRows = remember(bills) {
        bills.map { b -> BillRow(b.id, b.billNo, b.name, b.addedBy, billDate(b), b.total()) }
    }
    val liveSales = remember(billRows) { billRows.sumOf { it.amount } }
    val importedSales = remember(entries) { entries.filter { it.source == "bill" }.sumOf { it.amount } }
    val pending = remember(billRows, importedRefs) { billRows.filter { it.id !in importedRefs } }
    val zeroBills = remember(billRows) { billRows.count { it.amount <= 0 } }

    suspend fun importSales(assignTo: String?, subset: List<BillRow>?) {
        val list = subset ?: pending
        if (list.isEmpty()) { Toaster.show("No new bills to import"); return }
        val rows = list.map { b ->
            ledgerRow(
                entryType = "income", amount = b.amount, category = "Sales",
                note = "Bill " + (b.billNo?.let { "#$it" } ?: "(no number)"),
                entryDate = b.date.ifBlank { Fmt.todayIso() }, mode = "bill", source = "bill",
                createdBy = assignTo ?: b.addedBy ?: meEmail, ref = b.id,
            )
        }
        LedgerRepo.insert(rows)
        Toaster.show("Imported ${rows.size} sale${if (rows.size > 1) "s" else ""}")
        vm.load()
    }

    fun exportData() {
        if (shown.isEmpty()) { Toaster.show("Nothing to export"); return }
        scope.launch {
            val header = listOf("Date", "Type", "Category", "Description", "Amount", "Mode", "Staff", "Source", "Recorded at", "Ref")
            val aoa = ArrayList<List<Any?>>()
            aoa.add(header)
            var inc = 0.0; var exp = 0.0
            shown.forEach { e ->
                if (e.kind == "income") inc += e.amount else exp += e.amount
                aoa.add(listOf(
                    dateOf(e), e.kind, e.category ?: "", e.note ?: "", e.amount, e.mode ?: "",
                    Fmt.dispName(e.createdBy, userMap), e.source ?: "",
                    e.createdAt?.let { Fmt.fmtDateTime(it) } ?: "", e.ref ?: "",
                ))
            }
            aoa.add(emptyList())
            aoa.add(listOf("Income", "", "", "", inc))
            aoa.add(listOf("Expense", "", "", "", exp))
            aoa.add(listOf("Net", "", "", "", inc - exp))
            val tag = when (mode) {
                "all" -> "all"; "month" -> month; "year" -> year
                else -> (dFrom.ifBlank { "start" }) + "_" + (dTo.ifBlank { "now" })
            }
            val bytes = withContext(Dispatchers.Default) { Spreadsheet.writeXlsx(aoa, "Ledger") }
            runCatching { Exporter.share(ctx, "gavthan-ledger-$tag.xlsx", bytes, Exporter.XLSX_MIME) }
                .onSuccess { Toaster.show("Exported ${shown.size} entries") }
                .onFailure { Toaster.error(it.message ?: "Export failed") }
        }
    }

    val periodLabel = when (mode) {
        "all" -> "all time"; "month" -> Fmt.monthLabel(month); "year" -> year
        else -> if (dFrom.isNotBlank() || dTo.isNotBlank())
            "${if (dFrom.isNotBlank()) Fmt.fmtDate(dFrom) else "start"} – ${if (dTo.isNotBlank()) Fmt.fmtDate(dTo) else "now"}"
        else "selected dates"
    }

    // ===================== UI =====================
    Box(Modifier.fillMaxSize()) {
        if (loading) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(color = c.accent)
                Spacer(Modifier.height(10.dp))
                Text("Loading cashbook…", color = c.muted)
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 96.dp),
            ) {
                Spacer(Modifier.height(12.dp))
                LedgerDash(entries = chartBase, live = live)

                // filters
                Spacer(Modifier.height(12.dp))
                SectionCard {
                    Column(Modifier.padding(14.dp)) {
                        Segmented(
                            options = listOf("all" to "All", "month" to "Month", "year" to "Year", "dates" to "Dates"),
                            selected = mode, onSelect = { mode = it },
                        )
                        Spacer(Modifier.height(10.dp))
                        when (mode) {
                            "month" -> AppDropdown(month, months.map { it to Fmt.monthLabel(it) }, { month = it })
                            "year" -> AppDropdown(year, years.map { it to it }, { year = it })
                            "dates" -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DateField(dFrom, { dFrom = it }, Modifier.weight(1f), "From")
                                Text("to", color = c.muted, fontSize = 12.sp)
                                DateField(dTo, { dTo = it }, Modifier.weight(1f), "To")
                            }
                        }
                        FieldLabel("Staff")
                        AppDropdown(staff, listOf("all" to "All staff") + staffOpts, { staff = it })
                        FieldLabel("Category")
                        AppDropdown(catF, listOf("all" to "All categories") + catOpts.map { it to (catEmoji(it) + " " + it) }, { catF = it })
                        Spacer(Modifier.height(10.dp))
                        SearchField(q, { q = it }, "Search description / category")
                    }
                }

                // stats
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Income", Fmt.money(income), Tone.Green, Modifier.weight(1f))
                    StatTile("Expense", Fmt.money(expense), Tone.Red, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                StatTile(
                    "Net · $periodLabel" +
                        (if (staff != "all") " · ${Fmt.dispName(staff, userMap)}" else "") +
                        (if (catF != "all") " · $catF" else ""),
                    Fmt.money(net),
                    if (net >= 0) Tone.Green else Tone.Red,
                    Modifier.fillMaxWidth(),
                )

                // sales from bills
                if (pending.isNotEmpty() || liveSales != importedSales) {
                    Spacer(Modifier.height(12.dp))
                    SectionCard {
                        Column(Modifier.padding(14.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("Sales from bills", color = c.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        "On record ${Fmt.money(liveSales)} · imported ${Fmt.money(importedSales)}",
                                        color = c.muted, fontSize = 12.sp,
                                    )
                                }
                                if (pending.isNotEmpty()) {
                                    AppButton("Import ${pending.size}", { showImport = true }, Modifier, BtnStyle.Primary, enabled = online, small = true)
                                }
                            }
                            if (liveSales != importedSales || zeroBills > 0) {
                                Spacer(Modifier.height(10.dp))
                                AppButton(
                                    "🔎 Reconcile ${billRows.size} bills" + (if (zeroBills > 0) " · $zeroBills show ₹0" else ""),
                                    { showRecon = true }, Modifier.fillMaxWidth(), BtnStyle.Ghost, small = true,
                                )
                            }
                        }
                    }
                }

                // admin excel import
                if (isAdmin) {
                    Spacer(Modifier.height(12.dp))
                    AppButton(
                        if (excelOpen) "▾ Hide Excel import" else "▸ Import from Excel",
                        { excelOpen = !excelOpen }, Modifier.fillMaxWidth(), BtnStyle.Ghost, small = true,
                    )
                    if (excelOpen) {
                        Spacer(Modifier.height(8.dp))
                        SectionCard {
                            Row(
                                Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("Import from Excel", color = c.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Admin · Date · Description · User · Dr · Cr", color = c.muted, fontSize = 12.sp)
                                }
                                AppButton("⬆ Upload", { showExcel = true }, Modifier, BtnStyle.Secondary, small = true)
                            }
                        }
                    }
                }

                // kind seg
                Spacer(Modifier.height(12.dp))
                Segmented(
                    options = listOf("all" to "All", "income" to "Income", "expense" to "Expense"),
                    selected = kind, onSelect = { kind = it },
                )

                // entry list
                Spacer(Modifier.height(12.dp))
                SectionCard {
                    CardHeader(
                        "${shown.size} entr${if (shown.size == 1) "y" else "ies"}" +
                            (if (shown.size > PAGE_SIZE) " · page ${safePage + 1}/$totalPages" else ""),
                    ) {
                        if (shown.isNotEmpty()) AppButton("⬇ Export", { exportData() }, Modifier, BtnStyle.Ghost, small = true)
                    }
                    HorizontalDivider(color = c.line)
                    if (shown.isEmpty()) {
                        EmptyState("📒", "No entries match this filter")
                    } else {
                        EntryTableHeader()
                        pageItems.forEach { e ->
                            EntryRow(
                                e = e,
                                creatorName = Fmt.dispName(e.createdBy, userMap),
                                onToggleSettle = { vm.setSettled(e.id, e.settled == false) },
                            )
                        }
                        if (shown.size > PAGE_SIZE) {
                            Pager(safePage, totalPages, onPrev = { page = (safePage - 1).coerceAtLeast(0) },
                                onNext = { page = (safePage + 1).coerceAtMost(totalPages - 1) }, onJump = { page = it })
                        }
                    }
                }
            }

            // FAB
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 16.dp),
            ) {
                AppButton("＋  Add income / expense", { showEntry = true }, Modifier.fillMaxWidth())
            }
        }
    }

    // ===================== sheets =====================
    if (showEntry) {
        EntrySheet(
            kind0 = "expense", meEmail = meEmail, isAdmin = isAdmin, users = users, userMap = userMap,
            onClose = { showEntry = false }, onSaved = { showEntry = false; vm.load() },
        )
    }
    if (showImport) {
        ImportSalesSheet(
            pending = pending, isAdmin = isAdmin, users = users, userMap = userMap, meEmail = meEmail,
            onClose = { showImport = false }, onConfirm = { assignTo, subset -> importSales(assignTo, subset) },
        )
    }
    if (showRecon) {
        ReconSheet(rows = billRows, importedRefs = importedRefs, live = liveSales, imported = importedSales, onClose = { showRecon = false })
    }
    if (showExcel) {
        ExcelImportSheet(
            users = users, userMap = userMap, meEmail = meEmail,
            onClose = { showExcel = false }, onSaved = { showExcel = false; vm.load() },
        )
    }
}

/* ---------------- entry table ---------------- */

@Composable
private fun EntryTableHeader() {
    val c = gav
    Row(
        Modifier.fillMaxWidth().background(c.surface2).padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HCell("DATE", 52.dp)
        Text("DESCRIPTION", color = c.muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(horizontal = 4.dp))
        HCell("DR", 52.dp, TextAlign.End)
        HCell("CR", 52.dp, TextAlign.End)
        HCell("SET", 46.dp, TextAlign.Center)
    }
    HorizontalDivider(color = c.lineStrong)
}

@Composable
private fun HCell(text: String, width: androidx.compose.ui.unit.Dp, align: TextAlign = TextAlign.Start) {
    val c = gav
    Text(text, color = c.muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = align, modifier = Modifier.width(width).padding(horizontal = 4.dp))
}

@Composable
private fun EntryRow(e: LedgerEntry, creatorName: String, onToggleSettle: () -> Unit) {
    val c = gav
    val isInc = e.kind == "income"
    Row(
        Modifier.fillMaxWidth().padding(vertical = 9.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(Fmt.dmy(e.dateKey), color = c.muted, fontSize = 10.5.sp, modifier = Modifier.width(52.dp).padding(horizontal = 4.dp))
        Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
            Text(e.note ?: (if (isInc) "Income" else "Expense"), color = c.ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${catEmoji(e.category)} ${e.category ?: "—"} · ${creatorName.ifBlank { "—" }}" + (if (e.source == "bill") " · auto" else ""),
                color = c.muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Text(if (isInc) Fmt.money(e.amount) else "", color = c.credit, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, maxLines = 1, modifier = Modifier.width(52.dp).padding(horizontal = 4.dp))
        Text(if (isInc) "" else Fmt.money(e.amount), color = c.debit, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, maxLines = 1, modifier = Modifier.width(52.dp).padding(horizontal = 4.dp))
        Box(Modifier.width(46.dp).padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
            val no = e.settled == false
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (no) c.redBg else c.credit)
                    .border(1.dp, if (no) c.redBorder else c.credit, RoundedCornerShape(7.dp))
                    .clickable { onToggleSettle() }
                    .padding(vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (no) "No" else "Yes", color = if (no) c.debit else White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
    HorizontalDivider(color = c.line)
}

@Composable
private fun Pager(safePage: Int, totalPages: Int, onPrev: () -> Unit, onNext: () -> Unit, onJump: (Int) -> Unit) {
    val c = gav
    val lo = (safePage - 2).coerceAtLeast(0)
    val hi = (lo + 4).coerceAtMost(totalPages - 1)
    val start = (hi - 4).coerceAtLeast(0)
    Row(
        Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppButton("← Prev", onPrev, Modifier, BtnStyle.Ghost, enabled = safePage > 0, small = true)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (p in start..hi) {
                val on = p == safePage
                Box(
                    Modifier
                        .height(30.dp).width(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (on) c.accent else c.surface2)
                        .border(1.dp, if (on) c.accent else c.lineStrong, RoundedCornerShape(8.dp))
                        .clickable { onJump(p) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${p + 1}", color = if (on) White else c.muted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
        AppButton("Next →", onNext, Modifier, BtnStyle.Ghost, enabled = safePage < totalPages - 1, small = true)
    }
}

/* ---------------- helpers ---------------- */

private fun billDate(b: Bill): String {
    val ts = b.settledAt ?: b.date ?: b.billTime ?: b.createdAt ?: return ""
    return runCatching {
        OffsetDateTime.parse(ts).atZoneSameInstant(ZoneId.of("Asia/Kolkata")).toLocalDate().toString()
    }.getOrElse {
        runCatching { LocalDate.parse(ts.take(10)).toString() }.getOrDefault("")
    }
}
