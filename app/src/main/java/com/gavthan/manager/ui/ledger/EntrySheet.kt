package com.gavthan.manager.ui.ledger

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gavthan.manager.data.AppUser
import com.gavthan.manager.data.LedgerRepo
import com.gavthan.manager.data.ledgerRow
import com.gavthan.manager.data.num
import com.gavthan.manager.ui.components.AppBottomSheet
import com.gavthan.manager.ui.components.AppButton
import com.gavthan.manager.ui.components.AppDropdown
import com.gavthan.manager.ui.components.AppTextField
import com.gavthan.manager.ui.components.Banner
import com.gavthan.manager.ui.components.BannerTone
import com.gavthan.manager.ui.components.DateField
import com.gavthan.manager.ui.components.FieldLabel
import com.gavthan.manager.ui.components.Segmented
import com.gavthan.manager.ui.components.Toaster
import com.gavthan.manager.util.Fmt
import kotlinx.coroutines.launch

@Composable
fun EntrySheet(
    kind0: String,
    meEmail: String,
    isAdmin: Boolean,
    users: List<AppUser>,
    userMap: Map<String, String>,
    onClose: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var kind by remember { mutableStateOf(kind0) }
    val cats = if (kind == "income") INCOME_CATS else EXPENSE_CATS
    var amt by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf(cats.first()) }
    var desc by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(Fmt.todayIso()) }
    var mode by remember { mutableStateOf("cash") }
    var target by remember { mutableStateOf(meEmail) }
    var busy by remember { mutableStateOf(false) }

    fun switchKind(k: String) {
        kind = k
        cat = (if (k == "income") INCOME_CATS else EXPENSE_CATS).first()
    }

    fun save() {
        val a = num(amt)
        if (a <= 0 || busy) return
        busy = true
        scope.launch {
            val creator = if (isAdmin) target.ifBlank { meEmail } else meEmail
            runCatching {
                LedgerRepo.insertOne(
                    ledgerRow(
                        entryType = kind, amount = a, category = cat,
                        note = desc.trim().ifBlank { cat }, entryDate = date,
                        mode = if (kind == "income") mode else null, source = "manual", createdBy = creator,
                    )
                )
            }.onSuccess {
                Toaster.show(if (kind == "income") "Income added" else "Expense added")
                onSaved()
            }.onFailure {
                Toaster.error(it.message ?: "Save failed"); busy = false
            }
        }
    }

    AppBottomSheet("New entry", onClose) {
        Segmented(
            options = listOf("income" to "Income", "expense" to "Expense"),
            selected = kind, onSelect = { switchKind(it) }, alt = kind == "expense",
        )
        FieldLabel("Amount")
        AppTextField(amt, { amt = it }, placeholder = "0", keyboardType = KeyboardType.Decimal)
        FieldLabel("Category")
        AppDropdown(cat, cats.map { it to (catEmoji(it) + " " + it) }, { cat = it })
        FieldLabel("Date")
        DateField(date, { date = it })
        FieldLabel("Description")
        AppTextField(
            desc, { desc = it },
            placeholder = if (kind == "income") "e.g. owner investment" else "e.g. vegetables from market",
        )
        if (kind == "income") {
            FieldLabel("Received via")
            AppDropdown(
                mode,
                listOf("cash" to "Cash", "upi" to "UPI", "card" to "Card", "bank" to "Bank transfer"),
                { mode = it },
            )
        }
        Spacer(Modifier.height(12.dp))
        if (isAdmin) {
            FieldLabel("Recorded by (admin)")
            AppDropdown(
                target,
                users.map {
                    it.email to (Fmt.initCap(it.displayName ?: it.email.substringBefore("@")) +
                        if (it.email == meEmail) " (you)" else "")
                },
                { target = it },
            )
            Spacer(Modifier.height(6.dp))
            Banner("As admin you can log this entry on behalf of another staff member.", BannerTone.Neutral)
        } else {
            Banner("Recorded by ${Fmt.dispName(meEmail, userMap)}", BannerTone.Neutral)
        }
        Spacer(Modifier.height(14.dp))
        AppButton(
            text = if (busy) "Saving…" else "Save $kind",
            onClick = { save() },
            enabled = num(amt) > 0,
            loading = busy,
        )
    }
}
