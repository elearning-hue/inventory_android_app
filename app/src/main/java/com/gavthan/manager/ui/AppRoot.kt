package com.gavthan.manager.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gavthan.manager.data.AppUser
import com.gavthan.manager.data.Config
import com.gavthan.manager.ui.components.Banner
import com.gavthan.manager.ui.components.BannerTone
import com.gavthan.manager.ui.components.Crest
import com.gavthan.manager.ui.components.ToastOverlay
import com.gavthan.manager.ui.inventory.InventoryScreen
import com.gavthan.manager.ui.ledger.LedgerScreen
import com.gavthan.manager.ui.theme.GavthanTheme
import com.gavthan.manager.ui.theme.White
import com.gavthan.manager.ui.theme.gav
import com.gavthan.manager.util.ThemePref
import io.github.jan.supabase.auth.status.SessionStatus

@Composable
fun AppRoot() {
    val ctx = LocalContext.current
    var dark by remember { mutableStateOf(ThemePref.isDark(ctx)) }

    GavthanTheme(darkTheme = dark) {
        val vm: AppViewModel = viewModel()
        val status by vm.sessionStatus.collectAsStateWithLifecycle()
        val me by vm.me.collectAsStateWithLifecycle()
        val c = gav

        Surface(Modifier.fillMaxSize(), color = c.bg) {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .widthIn(max = 620.dp)
                        .fillMaxSize()
                        .align(Alignment.TopCenter)
                ) {
                    when (status) {
                        is SessionStatus.Authenticated -> {
                            if (me?.active == false) {
                                DisabledScreen(onSignOut = vm::signOut)
                            } else {
                                MainScaffold(
                                    me = me,
                                    dark = dark,
                                    onToggleTheme = {
                                        dark = !dark
                                        ThemePref.setDark(ctx, dark)
                                    },
                                    onSignOut = vm::signOut,
                                )
                            }
                        }
                        is SessionStatus.NotAuthenticated, is SessionStatus.RefreshFailure ->
                            LoginScreen()
                        else -> BootScreen()
                    }
                }
                ToastOverlay()
            }
        }
    }
}

@Composable
private fun MainScaffold(
    me: AppUser?,
    dark: Boolean,
    onToggleTheme: () -> Unit,
    onSignOut: () -> Unit,
) {
    var module by remember { mutableStateOf("ledger") }
    val online by rememberOnline()

    Column(Modifier.fillMaxSize()) {
        Header(
            me = me,
            module = module,
            dark = dark,
            onToggleTheme = onToggleTheme,
            onSignOut = onSignOut,
        )
        if (!online) {
            Box(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Banner("⚠ Offline — changes will fail until reconnected.", BannerTone.Net)
            }
        }
        ModuleTabs(module = module, onSelect = { module = it })
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (module == "ledger") LedgerScreen(me = me, online = online)
            else InventoryScreen(me = me, online = online)
        }
    }
}

@Composable
private fun Header(
    me: AppUser?,
    module: String,
    dark: Boolean,
    onToggleTheme: () -> Unit,
    onSignOut: () -> Unit,
) {
    val c = gav
    var confirm by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.surface)
            .statusBarsPadding()
    ) {
        androidx.compose.foundation.layout.Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Crest((Config.HOTEL_NAME.firstOrNull() ?: 'G').toString())
            Column(Modifier.weight(1f)) {
                Text("${Config.HOTEL_NAME} Manager", color = c.ink, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text(
                    if (module == "ledger") "KHATA · RECEIVABLES & PAYABLES" else "STOCK · INVENTORY CONTROL",
                    color = c.muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                )
            }
            IconBtn(if (dark) "☀" else "☾", circle = true, onClick = onToggleTheme)
            Spacer(Modifier.size(8.dp))
            IconBtn("⎋", onClick = { confirm = true })
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            confirmButton = { TextButton(onClick = { confirm = false; onSignOut() }) { Text("Sign out", color = c.debit) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel", color = c.muted) } },
            title = { Text("Sign out?", color = c.ink) },
            text = { Text(me?.email ?: "", color = c.muted) },
            containerColor = c.surface,
        )
    }
}

@Composable
private fun IconBtn(label: String, circle: Boolean = false, onClick: () -> Unit) {
    val c = gav
    Box(
        Modifier
            .size(36.dp)
            .clip(if (circle) CircleShape else RoundedCornerShape(10.dp))
            .background(c.surface2)
            .border(1.dp, c.lineStrong, if (circle) CircleShape else RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = c.muted, fontSize = 15.sp)
    }
}

@Composable
private fun ModuleTabs(module: String, onSelect: (String) -> Unit) {
    val c = gav
    androidx.compose.foundation.layout.Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ModuleTab("📒", "Ledger", module == "ledger", Modifier.weight(1f)) { onSelect("ledger") }
        ModuleTab("📦", "Inventory", module == "inventory", Modifier.weight(1f)) { onSelect("inventory") }
    }
}

@Composable
private fun ModuleTab(emoji: String, label: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = gav
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (on) c.accent else c.surface2)
            .border(1.dp, if (on) c.accent else c.surface2, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 17.sp)
        Spacer(Modifier.height(2.dp))
        Text(label, color = if (on) White else c.muted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun BootScreen() {
    val c = gav
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterVertically,
    ) {
        Crest((Config.HOTEL_NAME.firstOrNull() ?: 'G').toString(), size = 56)
        Spacer(Modifier.height(14.dp))
        Text("Loading…", color = c.muted)
    }
}

@Composable
private fun DisabledScreen(onSignOut: () -> Unit) {
    val c = gav
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterVertically,
    ) {
        Text("Account disabled", color = c.ink, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
        Spacer(Modifier.height(8.dp))
        Text("Ask an admin to re-enable your login.", color = c.muted)
        Spacer(Modifier.height(18.dp))
        com.gavthan.manager.ui.components.AppButton(
            "Sign out",
            onClick = onSignOut,
            style = com.gavthan.manager.ui.components.BtnStyle.Secondary,
            modifier = Modifier.widthIn(max = 240.dp).fillMaxWidth(),
        )
    }
}

/* ---- connectivity ---- */
@Composable
fun rememberOnline(): State<Boolean> {
    val ctx = LocalContext.current
    return produceState(initialValue = isOnlineNow(ctx)) {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { value = true }
            override fun onLost(network: Network) { value = isOnlineNow(ctx) }
            override fun onUnavailable() { value = false }
        }
        cm.registerDefaultNetworkCallback(cb)
        awaitDispose { runCatching { cm.unregisterNetworkCallback(cb) } }
    }
}

private fun isOnlineNow(ctx: Context): Boolean {
    val cm = ctx.getSystemService(ConnectivityManager::class.java) ?: return true
    val nc = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
