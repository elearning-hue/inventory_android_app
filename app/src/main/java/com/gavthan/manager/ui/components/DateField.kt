package com.gavthan.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gavthan.manager.ui.theme.gav
import com.gavthan.manager.util.Fmt
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    placeholder: String = "Pick a date",
) {
    val c = gav
    var open by remember { mutableStateOf(false) }
    val display = if (value.isBlank()) placeholder else Fmt.fmtDate(value)

    Text(
        text = display,
        color = if (value.isBlank()) c.muted2 else c.ink,
        fontSize = 15.sp,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(c.surface2)
            .border(1.dp, c.lineStrong, RoundedCornerShape(10.dp))
            .clickable { open = true }
            .padding(horizontal = 12.dp, vertical = 14.dp),
    )

    if (open) {
        val initialMillis = runCatching {
            LocalDate.parse(value.take(10)).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }.getOrNull()
        val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onChange(LocalDate.ofEpochDay(millis / 86_400_000L).toString())
                    }
                    open = false
                }) { Text("OK", color = c.accent) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel", color = c.muted) } },
        ) {
            DatePicker(state = state)
        }
    }
}
