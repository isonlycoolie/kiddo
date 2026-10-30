package app.kiddo.feature.chat.agent

import app.kiddo.core.database.ApplicationStore
import app.kiddo.core.database.ChatStore
import app.kiddo.core.database.CvStore
import app.kiddo.core.database.MailStore
import app.kiddo.core.database.NoticeStore
import app.kiddo.core.database.SettingsStore
import app.kiddo.core.database.WorkspaceStore
import app.kiddo.core.mail.MailTransport
import app.kiddo.core.model.AppStatus
import app.kiddo.core.model.ChatRole
import app.kiddo.core.model.Failure
import app.kiddo.core.model.MailAccount
import app.kiddo.core.model.MailAttachment
import app.kiddo.core.model.MailKind
import app.kiddo.core.model.NoticeKind
import app.kiddo.core.model.Outcome
import app.kiddo.core.model.OutgoingMail
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.agent_sent
import app.kiddo.core.resources.agent_sent_blind
import app.kiddo.core.resources.notice_sent
import app.kiddo.core.security.Vault
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import org.jetbrains.compose.resources.getString

// Sends an approved draft, then records it: the application, an update, and a line in the chat. Only the send sheet
// calls this, so nothing ever leaves without the user's tap.
class Outbox(
    private val chats: ChatStore,
    private val cvs: CvStore,
    private val mail: MailStore,
    private val applications: ApplicationStore,
    private val notices: NoticeStore,
    private val settings: SettingsStore,
    private val workspaces: WorkspaceStore,
    private val transport: MailTransport,
    private val vault: Vault,
) {
    suspend fun account(): MailAccount? = workspaces.current()?.let { mail.accounts(it.id).first().firstOrNull() }

    suspend fun send(messageId: String): Failure? {
        val message = chats.message(messageId) ?: return Failure.Unknown
        val draft = message.payload?.let { parseModelJson<DraftCard>(it) } ?: return Failure.Unknown
        if (draft.sent || draft.to.isBlank()) return null
        val ws = workspaces.current() ?: return Failure.Unknown
        val account = account() ?: return Failure.BadInput
        val attachment = if (account.kind == MailKind.Sendly) null else draft.cvId?.let { attachment(it, draft.cvName) }
        val outgoing = OutgoingMail(account.address, ws.name, listOf(draft.to.trim()), draft.subject, draft.body, attachment)
        val sent = transport.send(account, vault.secrets.get(account.secretRef).orEmpty(), outgoing)
        if (sent is Outcome.Failed) return sent.reason
        val headerId = (sent as Outcome.Done).value.ifEmpty { null }
        val appId = applications.add(ws.id, draft.role, draft.company, draft.to, message.chatId, headerId, draft.cvName)
        chats.setPayload(messageId, cardJson.encodeToString(draft.copy(sent = true, applicationId = appId)))
        notices.add(ws.id, NoticeKind.Sent, appId, getString(Res.string.notice_sent, draft.company.ifBlank { draft.to }, draft.role))
        val days = settings.settings(ws.id).first().followUpDays
        val line = if (account.readsReplies) getString(Res.string.agent_sent, days) else getString(Res.string.agent_sent_blind, days)
        chats.add(message.chatId, ChatRole.Assistant, line, null)
        return null
    }

    // The job is applied for on a website: record it so it shows in Applications, nothing is sent.
    suspend fun markAppliedOnSite(messageId: String) {
        val message = chats.message(messageId) ?: return
        val draft = message.payload?.let { parseModelJson<DraftCard>(it) } ?: return
        val ws = workspaces.current() ?: return
        val appId = applications.add(ws.id, draft.role, draft.company, draft.applyUrl, message.chatId, null, draft.cvName, AppStatus.AppliedOnSite)
        chats.setPayload(messageId, cardJson.encodeToString(draft.copy(sent = true, applicationId = appId)))
    }

    suspend fun saveDraft(messageId: String, to: String, subject: String, body: String) {
        val message = chats.message(messageId) ?: return
        val draft = message.payload?.let { parseModelJson<DraftCard>(it) } ?: return
        if (draft.sent) return
        // An address the user typed themselves is no longer "from the page".
        val fromPage = draft.fromPage && to.trim().equals(draft.to.trim(), ignoreCase = true)
        chats.setPayload(messageId, cardJson.encodeToString(draft.copy(to = to.trim(), subject = subject, body = body, fromPage = fromPage)))
    }

    private suspend fun attachment(cvId: String, name: String?): MailAttachment? {
        val ws = workspaces.current() ?: return null
        val cv = cvs.cvs(ws.id).first().firstOrNull { it.id == cvId } ?: return null
        val bytes = cvs.bytes(cvId) ?: return null
        val ext = if (cv.format == app.kiddo.core.model.CvFormat.Pdf) "pdf" else "docx"
        return MailAttachment("${(name ?: cv.name).replace(Regex("[^A-Za-z0-9 ._-]"), "")}.$ext", cv.format.mimeType, bytes)
    }
}
