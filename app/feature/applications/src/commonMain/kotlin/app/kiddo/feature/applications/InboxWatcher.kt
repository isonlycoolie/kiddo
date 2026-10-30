package app.kiddo.feature.applications

import app.kiddo.core.database.ApplicationStore
import app.kiddo.core.database.MailStore
import app.kiddo.core.database.NoticeStore
import app.kiddo.core.database.SettingsStore
import app.kiddo.core.database.WorkspaceStore
import app.kiddo.core.mail.MailTransport
import app.kiddo.core.model.AppStatus
import app.kiddo.core.model.Application
import app.kiddo.core.model.Clock
import app.kiddo.core.model.FoundReply
import app.kiddo.core.model.NoticeKind
import app.kiddo.core.model.Outcome
import app.kiddo.core.platform.isWeekend
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.notice_failed
import app.kiddo.core.resources.notice_follow
import app.kiddo.core.resources.notice_follow_body
import app.kiddo.core.resources.notice_reply
import app.kiddo.core.security.Vault
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.getString

// Looks for replies to sent applications and says when a follow-up is due. Runs when Kiddo opens and in the
// background; running twice changes nothing twice. It never sends anything.
class InboxWatcher(
    private val workspaces: WorkspaceStore,
    private val settings: SettingsStore,
    private val mail: MailStore,
    private val applications: ApplicationStore,
    private val notices: NoticeStore,
    private val transport: MailTransport,
    private val vault: Vault,
    private val clock: Clock,
) {
    private val lock = Mutex()

    // Returns how many new updates it added.
    suspend fun check(): Int = lock.withLock {
        val ws = workspaces.current() ?: return 0
        val waiting = applications.all(ws.id).filter { it.status == AppStatus.Sent }
        if (waiting.isEmpty()) return 0
        val before = notices.all(ws.id).size
        val account = mail.accounts(ws.id).first().firstOrNull()
        if (account != null && account.readsReplies) {
            val since = maxOf(waiting.minOf { it.sentAt } - DAY, clock.now() - LOOK_BACK)
            when (val found = transport.replies(account, vault.secrets.get(account.secretRef).orEmpty(), since)) {
                is Outcome.Done -> {
                    mail.markChecked(account.id)
                    waiting.forEach { app -> match(app, found.value)?.let { reply -> recordReply(ws.id, app, reply) } }
                }
                is Outcome.Failed -> failedOnce(ws.id)
            }
        }
        followUps(ws.id)
        notices.all(ws.id).size - before
    }

    private fun match(app: Application, replies: List<FoundReply>): FoundReply? = replies
        .filter { it.at == 0L || it.at > app.sentAt }
        .firstOrNull { reply ->
            val threaded = app.messageId != null && reply.inReplyTo.contains(app.messageId!!, ignoreCase = true)
            val domain = app.to.substringAfter('@', "").lowercase()
            val sameDomain = domain.isNotEmpty() && domain !in PERSONAL && reply.from.lowercase().contains("@$domain")
            threaded || sameDomain
        }

    private suspend fun recordReply(workspaceId: String, app: Application, reply: FoundReply) {
        if (app.replyAt != null && reply.at <= app.replyAt!!) return
        applications.setReply(app.id, AppStatus.Replied, reply.snippet.ifBlank { reply.subject }, reply.at.takeIf { it > 0 } ?: clock.now())
        notices.clear(app.id, NoticeKind.FollowUpDue)
        notices.add(workspaceId, NoticeKind.Reply, app.id, "${getString(Res.string.notice_reply, app.company.ifBlank { app.to })}\n${reply.snippet.take(SNIPPET)}")
    }

    private suspend fun followUps(workspaceId: String) {
        val s = settings.settings(workspaceId).first()
        if (!s.followUps) return
        val now = clock.now()
        if (s.skipWeekends && isWeekend(now)) return
        val due = notices.all(workspaceId).filter { it.kind == NoticeKind.FollowUpDue }.mapNotNull { it.applicationId }.toSet()
        applications.all(workspaceId)
            .filter { it.status == AppStatus.Sent && it.followUps < s.followUpMax && it.id !in due }
            .filter { now >= it.sentAt + s.followUpDays * DAY * (it.followUps + 1) }
            .forEach { app ->
                val body = "${getString(Res.string.notice_follow, app.company.ifBlank { app.to })}\n${getString(Res.string.notice_follow_body, s.followUpDays)}"
                notices.add(workspaceId, NoticeKind.FollowUpDue, app.id, body)
            }
    }

