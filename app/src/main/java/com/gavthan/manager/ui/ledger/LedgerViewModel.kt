package com.gavthan.manager.ui.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gavthan.manager.data.LedgerData
import com.gavthan.manager.data.LedgerRepo
import com.gavthan.manager.data.RealtimeSub
import com.gavthan.manager.ui.components.Toaster
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LedgerViewModel : ViewModel() {

    private val _data = MutableStateFlow(LedgerData())
    val data: StateFlow<LedgerData> = _data

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val rt = RealtimeSub(viewModelScope, listOf("mh_ledger", "mh_customers")) { load() }
    val live: StateFlow<Boolean> = rt.live

    init {
        load()
        rt.start()
    }

    fun load() {
        viewModelScope.launch {
            runCatching { LedgerRepo.loadAll() }
                .onSuccess { _data.value = it }
                .onFailure { Toaster.error(it.message ?: "Load failed") }
            _loading.value = false
        }
    }

    /** Optimistic settle toggle, mirroring the web setSettle(). */
    fun setSettled(id: String, value: Boolean) {
        val cur = _data.value
        _data.value = cur.copy(entries = cur.entries.map { if (it.id == id) it.copy(settled = value) else it })
        viewModelScope.launch {
            runCatching { LedgerRepo.setSettled(id, value) }
                .onFailure {
                    Toaster.error(it.message ?: "Couldn't update")
                    val c2 = _data.value
                    _data.value = c2.copy(entries = c2.entries.map { e -> if (e.id == id) e.copy(settled = !value) else e })
                }
        }
    }

    override fun onCleared() {
        rt.stop()
    }
}
