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

class AddViewModel(
    private val workspaces: WorkspaceStore,
    private val providers: ProviderStore,
    private val settings: SettingsStore,
    private val vault: Vault,
    private val ai: AiClient,
    private val ids: Ids,
) : ViewModel() {
    private val _state = MutableStateFlow(AddState())
    val state: StateFlow<AddState> = _state

    fun load(accountId: String?) {
        if (accountId == null || _state.value.editing?.id == accountId) return
        viewModelScope.launch {
            val ws = workspaces.current() ?: return@launch
            val account = providers.accounts(ws.id).first().firstOrNull { it.id == accountId } ?: return@launch
            _state.value = AddState(account.provider, "", account.baseUrl, account.model, editing = account)
        }
    }

    fun provider(p: AiProvider) = _state.update { AddState(provider = p, baseUrl = p.baseUrl, editing = it.editing) }

    fun key(value: String) = _state.update { it.copy(key = value.trim(), failure = null) }

    fun baseUrl(value: String) = _state.update { it.copy(baseUrl = value.trim(), failure = null) }

    fun model(value: String) = _state.update { it.copy(model = value) }

    // Lists the models with the key: proves the key works without spending tokens, and fills the model picker.
    fun testAndSave(onSaved: () -> Unit) {
        val now = _state.value
        if (!now.canTest) return
        _state.update { it.copy(testing = true, failure = null) }
        viewModelScope.launch {
            val key = now.key.ifBlank { now.editing?.keyRef?.let(vault.secrets::get).orEmpty() }
            when (val found = ai.models(AiTarget(now.provider, now.baseUrl, now.model, key))) {
                is Outcome.Failed -> _state.update { it.copy(testing = false, failure = found.reason) }
                is Outcome.Done -> {
                    val model = now.model.ifBlank { pickDefault(now.provider, found.value) }
                    if (model.isBlank()) {
                        _state.update { it.copy(testing = false, models = found.value, failure = Failure.NotFound) }
                    } else {
                        save(now, key, model)
                        onSaved()
                    }
                }
            }
        }
    }

    fun refreshModels() {
        val now = _state.value
        viewModelScope.launch {
            val key = now.key.ifBlank { now.editing?.keyRef?.let(vault.secrets::get).orEmpty() }
            val found = ai.models(AiTarget(now.provider, now.baseUrl, now.model, key))
            _state.update {
                when (found) {
                    is Outcome.Done -> it.copy(models = found.value, failure = null)
                    is Outcome.Failed -> it.copy(failure = found.reason)
                }
            }
        }
    }

    fun remove(onDone: () -> Unit) {
        val account = _state.value.editing ?: return
        viewModelScope.launch {
            providers.delete(account.id)
            vault.secrets.remove(account.keyRef)
            onDone()
        }
    }

    private suspend fun save(now: AddState, key: String, model: String) {
        val ws = workspaces.current() ?: return
        val editing = now.editing
        if (editing != null) {
            if (now.key.isNotBlank()) vault.secrets.put(editing.keyRef, key)
            providers.update(editing.id, now.baseUrl, model)
            return
        }
        val keyRef = "ai.${ids.next()}"
        vault.secrets.put(keyRef, key)
        val id = providers.add(ws.id, now.provider, now.baseUrl, model, keyRef)
        settings.update(ws.id) { if (it.chatModel == null) it.copy(chatModel = id) else it }
    }

    // A sensible first model when the user didn't type one: the provider's everyday model if listed.
    private fun pickDefault(provider: AiProvider, models: List<String>): String {
        val hints = when (provider) {
            AiProvider.Anthropic -> listOf("sonnet")
            AiProvider.OpenAi -> listOf("gpt-4o-mini", "gpt")
            AiProvider.Gemini -> listOf("flash")
            AiProvider.DeepSeek -> listOf("deepseek-chat")
            else -> emptyList()
        }
        return hints.firstNotNullOfOrNull { hint -> models.firstOrNull { it.contains(hint, ignoreCase = true) } }
            ?: models.firstOrNull().orEmpty()
    }
}

private const val STOP_AFTER = 5_000L
