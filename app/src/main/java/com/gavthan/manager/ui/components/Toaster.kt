package com.gavthan.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gavthan.manager.ui.theme.gav
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Lightweight global toast, equivalent to the web app's toast()/toastErr(). */
object Toaster {
    data class Msg(val text: String, val error: Boolean, val id: Long)

    private val _msg = MutableStateFlow<Msg?>(null)
    val msg: StateFlow<Msg?> = _msg.asStateFlow()

    fun show(text: String) { _msg.value = Msg(text, false, System.nanoTime()) }
    fun error(text: String) { _msg.value = Msg(text, true, System.nanoTime()) }
    fun clear() { _msg.value = null }
}

@Composable
fun BoxScope.ToastOverlay() {
    val c = gav
    val msg by Toaster.msg.collectAsStateWithLifecycle()
    val m = msg ?: return
    LaunchedEffect(m.id) {
        delay(if (m.error) 3500 else 2200)
        Toaster.clear()
    }
    Box(
        Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = 90.dp, start = 20.dp, end = 20.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(if (m.error) c.debit else if (c.isDark) c.surface3 else Color(0xFF211E1A))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            m.text,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
