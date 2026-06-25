package com.gavthan.manager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gavthan.manager.data.AppUser
import com.gavthan.manager.data.AuthRepo
import com.gavthan.manager.data.Supa
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AppViewModel : ViewModel() {

    val sessionStatus: StateFlow<SessionStatus> = Supa.client.auth.sessionStatus

    private val _me = MutableStateFlow<AppUser?>(null)
    val me: StateFlow<AppUser?> = _me

    init {
        viewModelScope.launch {
            sessionStatus.collect { st ->
                when (st) {
                    is SessionStatus.Authenticated -> {
                        val email = st.session.user?.email
                        if (email != null) {
                            if (_me.value?.email != email) {
                                _me.value = AppUser(email = email, role = "staff", active = true, displayName = null)
                            }
                            _me.value = AuthRepo.loadMe(email)
                        }
                    }
                    is SessionStatus.NotAuthenticated -> _me.value = null
                    else -> { /* Initializing / RefreshFailure */ }
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { runCatching { AuthRepo.signOut() } }
    }
}
