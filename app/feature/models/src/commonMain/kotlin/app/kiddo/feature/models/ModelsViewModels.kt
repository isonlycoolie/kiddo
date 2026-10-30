package app.kiddo.feature.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.kiddo.core.ai.AiClient
import app.kiddo.core.ai.AiTarget
import app.kiddo.core.database.ProviderStore
import app.kiddo.core.database.SettingsStore
import app.kiddo.core.database.WorkspaceStore
import app.kiddo.core.model.AiProvider
import app.kiddo.core.model.AppSettings
import app.kiddo.core.model.Failure
import app.kiddo.core.model.Ids
import app.kiddo.core.model.Outcome
import app.kiddo.core.model.ProviderAccount
import app.kiddo.core.security.Vault
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiState(val accounts: List<ProviderAccount> = emptyList(), val settings: AppSettings = AppSettings()) {
    val chat: ProviderAccount? get() = accounts.firstOrNull { it.id == settings.chatModel } ?: accounts.firstOrNull()
}

@OptIn(ExperimentalCoroutinesApi::class)
class AiViewModel(private val workspaces: WorkspaceStore, providers: ProviderStore, private val settings: SettingsStore) : ViewModel() {
    val state: StateFlow<AiState> = workspaces.workspace.filterNotNull().flatMapLatest { ws ->
        combine(providers.accounts(ws.id), settings.settings(ws.id), ::AiState)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER), AiState())

    fun useForChat(accountId: String) {
        viewModelScope.launch { workspaces.current()?.let { ws -> settings.update(ws.id) { it.copy(chatModel = accountId) } } }
    }
}

data class AddState(
    val provider: AiProvider = AiProvider.Anthropic,
    val key: String = "",
    val baseUrl: String = AiProvider.Anthropic.baseUrl,
    val model: String = "",
    val models: List<String> = emptyList(),
    val editing: ProviderAccount? = null,
    val testing: Boolean = false,
    val failure: Failure? = null,
) {
    val showsAddress: Boolean get() = provider == AiProvider.Ollama || provider == AiProvider.Other
    val canTest: Boolean get() =
        !testing && baseUrl.isNotBlank() && (!provider.needsKey || key.isNotBlank() || editing != null)
}

