package app.kiddo.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.kiddo.core.database.MailStore
import app.kiddo.core.database.ProviderStore
import app.kiddo.core.database.SettingsStore
import app.kiddo.core.database.WorkspaceStore
import app.kiddo.core.model.AppSettings
import app.kiddo.core.model.MailAccount
import app.kiddo.core.model.ProviderAccount
import app.kiddo.core.security.AppReset
import app.kiddo.core.security.HardwareInfo
import app.kiddo.core.security.Vault
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsState(
    val settings: AppSettings = AppSettings(),
    val providers: List<ProviderAccount> = emptyList(),
    val mail: MailAccount? = null,
    val hardware: HardwareInfo = HardwareInfo(strongBox = false, deviceLock = false),
)

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModel(
    private val workspaces: WorkspaceStore,
    private val settings: SettingsStore,
    providers: ProviderStore,
    mail: MailStore,
    private val vault: Vault,
    private val reset: AppReset,
) : ViewModel() {
    val state: StateFlow<SettingsState> = workspaces.workspace.filterNotNull().flatMapLatest { ws ->
        combine(settings.settings(ws.id), providers.accounts(ws.id), mail.accounts(ws.id)) { s, p, m ->
            SettingsState(s, p, m.firstOrNull(), vault.hardware())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER), SettingsState())

    fun update(change: (AppSettings) -> AppSettings) {
        viewModelScope.launch { workspaces.current()?.let { settings.update(it.id, change) } }
    }

    fun eraseEverything() = reset.eraseAll()

    private companion object {
        const val STOP_AFTER = 5_000L
    }
}
