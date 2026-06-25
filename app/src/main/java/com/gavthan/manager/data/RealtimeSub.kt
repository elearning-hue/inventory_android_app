package com.gavthan.manager.data

import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Subscribe to Postgres change events on the given tables and fire [onChange]
 * (debounced 250ms) whenever a row changes — the live transport behind the
 * dashboards, mirroring the web app's `useRealtime`.
 */
class RealtimeSub(
    private val scope: CoroutineScope,
    private val tables: List<String>,
    private val onChange: () -> Unit,
) {
    private val _live = MutableStateFlow(false)
    val live: StateFlow<Boolean> = _live

    private var channel: RealtimeChannel? = null
    private var debounce: Job? = null

    fun start() {
        if (channel != null) return
        val name = "rt-" + tables.joinToString("-").replace(Regex("[^a-zA-Z0-9]+"), "-")
        val ch = Supa.client.channel(name)
        channel = ch
        tables.forEach { t ->
            ch.postgresChangeFlow<PostgresAction>(schema = "public") { table = t }
                .onEach { ping() }
                .launchIn(scope)
        }
        scope.launch {
            runCatching { ch.subscribe(blockUntilSubscribed = true) }
                .onSuccess { _live.value = true }
        }
    }

    private fun ping() {
        debounce?.cancel()
        debounce = scope.launch {
            delay(250)
            onChange()
        }
    }

    fun stop() {
        val ch = channel ?: return
        channel = null
        _live.value = false
        scope.launch { runCatching { ch.unsubscribe() } }
    }
}
