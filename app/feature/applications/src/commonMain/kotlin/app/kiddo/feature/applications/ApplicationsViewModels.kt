package app.kiddo.feature.applications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.kiddo.core.database.ApplicationStore
import app.kiddo.core.database.MailStore
import app.kiddo.core.database.NoticeStore
import app.kiddo.core.database.SettingsStore
import app.kiddo.core.database.WorkspaceStore
import app.kiddo.core.mail.MailTransport
import app.kiddo.core.model.AppSettings
import app.kiddo.core.model.AppStatus
import app.kiddo.core.model.Application
import app.kiddo.core.model.Failure
import app.kiddo.core.model.Notice
import app.kiddo.core.model.NoticeKind
import app.kiddo.core.model.Outcome
import app.kiddo.core.model.OutgoingMail
import app.kiddo.core.security.Vault
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ApplicationsViewModel(
    private val workspaces: WorkspaceStore,
    private val applications: ApplicationStore,
    private val notices: NoticeStore,
    private val settings: SettingsStore,
    private val watcher: InboxWatcher,
) : ViewModel() {
    val list: StateFlow<List<Application>?> = workspaces.workspace.filterNotNull()
        .flatMapLatest { applications.applications(it.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER), null)

    val notes: StateFlow<List<Notice>?> = workspaces.workspace.filterNotNull()
        .flatMapLatest { notices.notices(it.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER), null)

    val prefs: StateFlow<AppSettings> = workspaces.workspace.filterNotNull()
        .flatMapLatest { settings.settings(it.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER), AppSettings())

    private val _checking = MutableStateFlow(false)
    val checking: StateFlow<Boolean> = _checking

    fun setStatus(id: String, status: AppStatus) {
        viewModelScope.launch { applications.setStatus(id, status) }
    }

    fun delete(id: String) {
        viewModelScope.launch { applications.delete(id) }
    }

    fun readAll() {
        viewModelScope.launch { workspaces.current()?.let { notices.readAll(it.id) } }
    }

    fun checkNow() {
        if (_checking.value) return
        _checking.value = true
        viewModelScope.launch {
            watcher.check()
            _checking.value = false
        }
    }

    fun updateSettings(change: (AppSettings) -> AppSettings) {
        viewModelScope.launch { workspaces.current()?.let { settings.update(it.id, change) } }
    }

    private companion object {
        const val STOP_AFTER = 5_000L
    }
}

// Sends a due follow-up in the same thread as the application, then counts it and clears the update.
class FollowUpSender(
    private val workspaces: WorkspaceStore,
    private val applications: ApplicationStore,
    private val notices: NoticeStore,
    private val mail: MailStore,
    private val transport: MailTransport,
    private val vault: Vault,
) {
    suspend fun send(app: Application, subject: String, body: String): Failure? {
        val ws = workspaces.current() ?: return Failure.Unknown
        val account = mail.accounts(ws.id).first().firstOrNull() ?: return Failure.BadInput
        val outgoing = OutgoingMail(account.address, ws.name, listOf(app.to), subject, body, inReplyTo = app.messageId)
        val sent = transport.send(account, vault.secrets.get(account.secretRef).orEmpty(), outgoing)
        if (sent is Outcome.Failed) return sent.reason
        applications.countFollowUp(app.id)
        notices.clear(app.id, NoticeKind.FollowUpDue)
        return null
    }

    suspend fun signOff(): String = workspaces.current()?.name.orEmpty()
}
