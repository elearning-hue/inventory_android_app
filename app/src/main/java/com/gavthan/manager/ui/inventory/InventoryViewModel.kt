package com.gavthan.manager.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gavthan.manager.data.InventoryData
import com.gavthan.manager.data.InventoryRepo
import com.gavthan.manager.data.RealtimeSub
import com.gavthan.manager.ui.components.Toaster
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class InventoryViewModel : ViewModel() {

    private val _data = MutableStateFlow(InventoryData())
    val data: StateFlow<InventoryData> = _data

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val rt = RealtimeSub(viewModelScope, listOf("mh_inventory_items", "mh_stock_moves")) { load() }
    val live: StateFlow<Boolean> = rt.live

    init {
        load()
        rt.start()
    }

    fun load() {
        viewModelScope.launch {
            runCatching { InventoryRepo.loadAll() }
                .onSuccess { _data.value = it }
                .onFailure { Toaster.error(it.message ?: "Load failed") }
            _loading.value = false
        }
    }

    override fun onCleared() {
        rt.stop()
    }
}
