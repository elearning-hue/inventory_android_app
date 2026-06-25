package com.gavthan.manager.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gavthan.manager.ui.theme.White
import com.gavthan.manager.ui.theme.gav

/* ------------------------------ Cards ------------------------------ */

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = gav
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(14.dp))
    ) { content() }
}

@Composable
fun CardHeader(title: String, trailing: @Composable (RowScope.() -> Unit)? = null) {
    val c = gav
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = c.ink, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        if (trailing != null) Row(verticalAlignment = Alignment.CenterVertically) { trailing() }
    }
}

/* ------------------------------ Stats ------------------------------ */

enum class Tone { Neutral, Green, Red, Warn }

@Composable
fun StatTile(label: String, value: String, tone: Tone = Tone.Neutral, modifier: Modifier = Modifier) {
    val c = gav
    val vColor = when (tone) {
        Tone.Green -> c.credit
        Tone.Red -> c.debit
        Tone.Warn -> c.warn
        Tone.Neutral -> c.ink
    }
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(c.surface2)
            .border(1.dp, c.line, RoundedCornerShape(12.dp))
            .padding(horizontal = 13.dp, vertical = 12.dp)
    ) {
        Text(label.uppercase(), color = c.muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Text(value, color = vColor, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/* ------------------------------ Pills ------------------------------ */

enum class PillTone { Neutral, Low, Ok }

@Composable
fun Pill(text: String, tone: PillTone = PillTone.Neutral) {
    val c = gav
    val (fg, bg, border) = when (tone) {
        PillTone.Low -> Triple(c.debit, c.redBg, c.redBorder)
        PillTone.Ok -> Triple(c.credit, c.greenBg, c.greenBorder)
        PillTone.Neutral -> Triple(c.muted, c.surface2, c.lineStrong)
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

/* ----------------------------- Buttons ----------------------------- */

enum class BtnStyle { Primary, Secondary, Ghost, Danger }

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    style: BtnStyle = BtnStyle.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    small: Boolean = false,
) {
    val c = gav
    val (bg, fg, border) = when (style) {
        BtnStyle.Primary -> Triple(c.accent, White, c.accent)
        BtnStyle.Secondary -> Triple(c.surface, c.accent, c.lineStrong)
        BtnStyle.Ghost -> Triple(Color.Transparent, c.muted, c.line)
        BtnStyle.Danger -> Triple(c.surface, c.debit, c.redBorder)
    }
    val realEnabled = enabled && !loading
    val shape = RoundedCornerShape(if (small) 9.dp else 11.dp)
    Box(
        modifier
            .clip(shape)
            .background(if (realEnabled) bg else bg.copy(alpha = 0.5f))
            .border(1.dp, if (realEnabled) border else border.copy(alpha = 0.5f), shape)
            .clickable(enabled = realEnabled) { onClick() }
            .padding(
                horizontal = if (small) 12.dp else 14.dp,
                vertical = if (small) 8.dp else 11.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(15.dp), color = fg, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text,
                color = if (realEnabled) fg else fg.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold,
                fontSize = if (small) 13.sp else 14.sp,
            )
        }
    }
}

/* ----------------------------- Inputs ------------------------------ */

@Composable
fun FieldLabel(text: String) {
    val c = gav
    Text(
        text.uppercase(),
        color = c.muted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 12.dp, bottom = 5.dp),
    )
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    leading: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    password: Boolean = false,
) {
    val c = gav
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        singleLine = singleLine,
        placeholder = if (placeholder.isEmpty()) null else { { Text(placeholder, color = c.muted2) } },
        leadingIcon = leading,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboardType),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = c.accent,
            unfocusedBorderColor = c.lineStrong,
            disabledBorderColor = c.line,
            focusedContainerColor = c.surface,
            unfocusedContainerColor = c.surface2,
            disabledContainerColor = c.surface2,
            cursorColor = c.accent,
            focusedTextColor = c.ink,
            unfocusedTextColor = c.ink,
        ),
    )
}

@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        leading = { Text("🔍", fontSize = 15.sp) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AppDropdown(
    value: T,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    val c = gav
    var expanded by remember { mutableStateOf(false) }
    val label = options.firstOrNull { it.first == value }?.second ?: ""
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = c.accent,
                unfocusedBorderColor = c.lineStrong,
                focusedContainerColor = c.surface,
                unfocusedContainerColor = c.surface2,
                focusedTextColor = c.ink,
                unfocusedTextColor = c.ink,
            ),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (v, lbl) ->
                DropdownMenuItem(
                    text = { Text(lbl, color = c.ink) },
                    onClick = { onSelect(v); expanded = false },
                )
            }
        }
    }
}

/* ---------------------------- Segmented ---------------------------- */

@Composable
fun Segmented(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    alt: Boolean = false,
) {
    val c = gav
    val onColor = if (alt) c.debit else c.accent
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (key, lbl) ->
            val on = key == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) onColor else c.surface2)
                    .border(1.dp, if (on) onColor else c.lineStrong, RoundedCornerShape(10.dp))
                    .clickable { onSelect(key) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    lbl,
                    color = if (on) White else c.muted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/* --------------------------- Misc widgets -------------------------- */

@Composable
fun LiveBadge(live: Boolean) {
    val c = gav
    val fg = if (live) c.credit else c.muted2
    val bg = if (live) c.greenBg else c.surface2
    val border = if (live) c.greenBorder else c.lineStrong
    Row(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(fg))
        Text(if (live) "LIVE" else "…", color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

enum class BannerTone { Info, Net, Error, Neutral }

@Composable
fun Banner(text: String, tone: BannerTone = BannerTone.Info, modifier: Modifier = Modifier.fillMaxWidth()) {
    val c = gav
    val (fg, bg, border) = when (tone) {
        BannerTone.Net -> Triple(c.warn, c.warnBg, c.warnBorder)
        BannerTone.Error, BannerTone.Info -> Triple(c.debit, c.redBg, c.redBorder)
        BannerTone.Neutral -> Triple(c.muted, c.surface2, c.lineStrong)
    }
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Text(text, color = fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EmptyState(emoji: String, text: String) {
    val c = gav
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 34.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 34.sp)
        Spacer(Modifier.height(6.dp))
        Text(text, color = c.muted2, fontSize = 13.sp)
    }
}

@Composable
fun Crest(letter: String, size: Int = 34) {
    val c = gav
    Box(
        Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 3).dp))
            .background(c.accent),
        contentAlignment = Alignment.Center,
    ) {
        Text(letter, color = White, fontWeight = FontWeight.ExtraBold, fontSize = (size * 0.5).sp)
    }
}

@Composable
fun Avatar(initials: String, supplier: Boolean = false) {
    val c = gav
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (supplier) c.avSup else c.accent),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
