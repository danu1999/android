package com.posbah.app.ui.screens.bmp.clients

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.local.entities.BmpClientEntity
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.repository.BmpClientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClientsUiState(
    val query: String = "",
    val isLoading: Boolean = false
)

@HiltViewModel
class ClientsViewModel @Inject constructor(
    private val repo: BmpClientRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val tenantId = authRepository.activeTenantId().orEmpty()
    private val _query = MutableStateFlow("")
    val ui = MutableStateFlow(ClientsUiState()).asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val clients = _query
        .flatMapLatest { q ->
            if (q.isBlank()) repo.observe(tenantId) else repo.search(tenantId, q)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setQuery(q: String) {
        _query.value = q
    }

    fun delete(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }
}

@HiltViewModel
class ClientEditViewModel @Inject constructor(
    private val repo: BmpClientRepository,
    private val authRepository: AuthRepository,
    savedState: SavedStateHandle
) : ViewModel() {
    private val tenantId = authRepository.activeTenantId().orEmpty()
    private val editingId: Long = savedState.get<String>("id")?.toLongOrNull()?.takeIf { it > 0 } ?: -1L

    private val _form = MutableStateFlow(
        BmpClientEntity(
            tenantId = tenantId,
            clientName = ""
        )
    )
    val form = _form.asStateFlow()
    private val _saved = MutableStateFlow(false)
    val saved = _saved.asStateFlow()

    init {
        if (editingId > 0) viewModelScope.launch {
            repo.getById(editingId)?.let { _form.value = it }
        }
    }

    fun update(transform: (BmpClientEntity) -> BmpClientEntity) {
        _form.update(transform)
    }

    fun save() {
        viewModelScope.launch {
            repo.upsert(_form.value)
            _saved.value = true
        }
    }
}
