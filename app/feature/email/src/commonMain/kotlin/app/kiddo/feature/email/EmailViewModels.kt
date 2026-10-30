package app.kiddo.feature.email

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.kiddo.core.database.MailStore
import app.kiddo.core.database.WorkspaceStore
import app.kiddo.core.mail.MailCheck
import app.kiddo.core.mail.MailTransport
import app.kiddo.core.mail.SendlyTransport
import app.kiddo.core.model.Clock
import app.kiddo.core.model.Failure
import app.kiddo.core.model.GmailServers
import app.kiddo.core.model.Ids
import app.kiddo.core.model.MailAccount
import app.kiddo.core.model.MailKind
import app.kiddo.core.model.MailSecurity
import app.kiddo.core.model.MailServer
import app.kiddo.core.model.Outcome
import app.kiddo.core.model.OutgoingMail
import app.kiddo.core.model.SendlyConfig
import app.kiddo.core.security.Vault
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConnectForm(
    val address: String = "",
    val secret: String = "",
    val smtpHost: String = "",
    val smtpPort: String = "465",
    val security: MailSecurity = MailSecurity.SslTls,
    val username: String = "",
    val imapHost: String = "",
    val imapPort: String = "993",
    val sendlyFrom: String = "",
    val testing: Boolean = false,
    val failure: Failure? = null,
)

// One view model for every way to connect: it builds the account, tests it for real, and only then stores it.
@OptIn(ExperimentalCoroutinesApi::class)
class EmailViewModel(
    private val workspaces: WorkspaceStore,
    private val mail: MailStore,
    private val transport: MailTransport,
    private val vault: Vault,
    private val ids: Ids,
    private val clock: Clock,
) : ViewModel() {
    val account: StateFlow<MailAccount?> = workspaces.workspace.filterNotNull()
        .flatMapLatest { ws -> mail.accounts(ws.id).map { it.firstOrNull() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER), null)

    private val _form = MutableStateFlow(ConnectForm())
    val form: StateFlow<ConnectForm> = _form

    private val _check = MutableStateFlow<MailCheck?>(null)
    val check: StateFlow<MailCheck?> = _check

    fun prefill() {
        viewModelScope.launch {
            val email = workspaces.current()?.email.orEmpty()
            _form.update { if (it.address.isEmpty()) it.copy(address = email, sendlyFrom = email, username = email) else it }
        }
    }

    fun edit(change: (ConnectForm) -> ConnectForm) = _form.update { change(it).copy(failure = null) }

    fun connectGmail(onDone: () -> Unit) = connect(onDone, gmail = true) { f, ref, ws ->
        account(MailKind.GmailAppPassword, f.address, ws, f.address, GmailServers.Smtp, GmailServers.Imap, null, ref)
    }

    fun connectSmtp(onDone: () -> Unit) = connect(onDone) { f, ref, ws ->
        val smtp = MailServer(f.smtpHost.trim(), f.smtpPort.toIntOrNull() ?: 0, f.security)
        val imap = f.imapHost.trim().takeIf { it.isNotEmpty() }?.let { MailServer(it, f.imapPort.toIntOrNull() ?: IMAP_PORT, MailSecurity.SslTls) }
        account(MailKind.Smtp, f.address, ws, f.username.ifBlank { f.address }, smtp, imap, null, ref)
    }

    fun connectSendly(onDone: () -> Unit) = connect(onDone) { f, ref, ws ->
        account(MailKind.Sendly, f.sendlyFrom, ws, f.sendlyFrom, null, null, SendlyConfig(SendlyTransport.DEFAULT_BASE), ref)
    }

    fun recheck() {
        val acc = account.value ?: return
        viewModelScope.launch {
            val secret = vault.secrets.get(acc.secretRef).orEmpty()
            when (val result = transport.check(acc, secret)) {
                is Outcome.Done -> {
                    _check.value = result.value
                    mail.markChecked(acc.id)
                }
                is Outcome.Failed -> _form.update { it.copy(failure = result.reason) }
            }
        }
    }

    fun sendTest(subject: String, body: String, onDone: (Failure?) -> Unit) {
        val acc = account.value ?: return
        viewModelScope.launch {
            val secret = vault.secrets.get(acc.secretRef).orEmpty()
            val mailOut = OutgoingMail(acc.address, acc.displayName, listOf(acc.address), subject, body)
            onDone((transport.send(acc, secret, mailOut) as? Outcome.Failed)?.reason)
        }
    }

    fun disconnect(onDone: () -> Unit) {
        val acc = account.value ?: return
        viewModelScope.launch {
            mail.delete(acc.id)
            vault.secrets.remove(acc.secretRef)
            onDone()
        }
    }

    private fun account(
        kind: MailKind,
        address: String,
        name: String,
        username: String,
        smtp: MailServer?,
        imap: MailServer?,
        sendly: SendlyConfig?,
        ref: String,
    ) = MailAccount(ids.next(), kind, address.trim(), name, username.trim(), smtp, imap, sendly, ref, clock.now(), clock.now())

    private fun connect(onDone: () -> Unit, gmail: Boolean = false, build: (ConnectForm, String, String) -> MailAccount) {
        val f = _form.value
        if (f.testing) return
        _form.update { it.copy(testing = true, failure = null) }
        viewModelScope.launch {
            val ws = workspaces.current() ?: return@launch
            // Gmail shows app passwords in groups of four; the spaces are not part of it. Other passwords stay as typed.
            val secret = if (gmail) f.secret.replace(" ", "") else f.secret
            val ref = "mail.${ids.next()}"
            val acc = build(f, ref, ws.name)
            when (val result = transport.check(acc, secret)) {
                is Outcome.Failed -> _form.update { it.copy(testing = false, failure = result.reason) }
                is Outcome.Done -> {
                    vault.secrets.put(ref, secret)
                    account.value?.let { old -> vault.secrets.remove(old.secretRef) }
                    mail.replace(ws.id, acc)
                    _check.value = result.value
                    _form.value = ConnectForm()
                    onDone()
                }
            }
        }
    }

    private companion object {
        const val STOP_AFTER = 5_000L
        const val IMAP_PORT = 993
    }
}
