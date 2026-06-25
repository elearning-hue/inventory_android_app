package com.gavthan.manager.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gavthan.manager.data.AuthRepo
import com.gavthan.manager.data.Config
import com.gavthan.manager.ui.components.AppButton
import com.gavthan.manager.ui.components.AppTextField
import com.gavthan.manager.ui.components.Banner
import com.gavthan.manager.ui.components.BannerTone
import com.gavthan.manager.ui.components.Crest
import com.gavthan.manager.ui.components.FieldLabel
import com.gavthan.manager.ui.theme.gav
import kotlinx.coroutines.launch

@Composable
fun LoginScreen() {
    val c = gav
    var email by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun submit() {
        if (email.isBlank() || pw.isBlank() || busy) return
        scope.launch {
            busy = true; err = ""
            runCatching { AuthRepo.signIn(email, pw) }
                .onFailure { err = it.message ?: "Login failed" }
            busy = false
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterVertically,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            Modifier.widthIn(max = 380.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterVertically,
        ) {
            Crest((Config.HOTEL_NAME.firstOrNull() ?: 'G').toString(), size = 56)
            Spacer(Modifier.height(14.dp))
            Text(
                "${Config.HOTEL_NAME} Manager",
                color = c.ink, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Ledger & Inventory · sign in with your staff account",
                color = c.muted, fontSize = 14.sp, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(22.dp))

            if (err.isNotEmpty()) {
                Banner(err, BannerTone.Info)
                Spacer(Modifier.height(8.dp))
            }

            Column(Modifier.fillMaxWidth()) {
                FieldLabel("Email")
                AppTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "you@hotel.com",
                    keyboardType = KeyboardType.Email,
                )
                FieldLabel("Password")
                AppTextField(
                    value = pw,
                    onValueChange = { pw = it },
                    placeholder = "••••••••",
                    password = true,
                )
            }
            Spacer(Modifier.height(18.dp))
            AppButton(
                text = if (busy) "Signing in…" else "Sign in",
                onClick = { submit() },
                enabled = email.isNotBlank() && pw.isNotBlank(),
                loading = busy,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Same login as your billing app. New staff must be added by an admin.",
                color = c.muted, fontSize = 12.sp, textAlign = TextAlign.Center,
            )
        }
    }
}
