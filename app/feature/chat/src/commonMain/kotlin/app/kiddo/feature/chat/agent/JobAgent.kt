package app.kiddo.feature.chat.agent

import app.kiddo.core.ai.AiClient
import app.kiddo.core.ai.AiTarget
import app.kiddo.core.ai.complete
import app.kiddo.core.database.ChatStore
import app.kiddo.core.database.CvStore
import app.kiddo.core.database.WorkspaceStore
import app.kiddo.core.model.ChatRole
import app.kiddo.core.model.Cv
import app.kiddo.core.model.MessageKind
import app.kiddo.core.model.Outcome
import app.kiddo.core.model.ProviderAccount
import app.kiddo.core.model.Signature
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.agent_blocked
import app.kiddo.core.resources.agent_draft_intro
import app.kiddo.core.resources.agent_failed_read
import app.kiddo.core.resources.agent_found
import app.kiddo.core.resources.agent_found_role
import app.kiddo.core.resources.agent_missing
import app.kiddo.core.resources.agent_site
import app.kiddo.core.resources.step_browser
import app.kiddo.core.resources.step_cant_read
import app.kiddo.core.resources.step_cv
import app.kiddo.core.resources.step_no_cv
import app.kiddo.core.resources.step_read
import app.kiddo.core.resources.step_reading
import app.kiddo.core.resources.step_writing
import app.kiddo.core.security.Vault
import app.kiddo.core.web.PageReader
import app.kiddo.core.web.firstUrl
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import org.jetbrains.compose.resources.getString

// Turns "I found this job" into a draft: read the post (link or pasted text), pull out the job, pick the CV, write
// the email. Every step shows in the chat as it happens. It never sends; sending is the user's tap on the send sheet.
class JobAgent(
    private val chats: ChatStore,
    private val cvs: CvStore,
    private val workspaces: WorkspaceStore,
    private val pages: PageReader,
    private val ai: AiClient,
    private val vault: Vault,
) {
    // A message is a job when it carries a link or is long enough to be a pasted post.
    fun isJob(text: String): Boolean = firstUrl(text) != null || text.length >= PASTED_POST

    suspend fun fromMessage(chatId: String, account: ProviderAccount, text: String) {
        val url = firstUrl(text)
        if (url == null) {
            fromText(chatId, account, text, userText = text)
            return
        }
        val host = url.substringAfter("://").substringBefore('/')
        val step = chats.add(chatId, ChatRole.Assistant, "", account.id, MessageKind.Step, step(getString(Res.string.step_reading, host), StepState.Running))
        val page = pages.read(url)
        val read = (page as? Outcome.Done)?.value?.takeUnless { it.needsBrowser }
        if (read == null) {
            chats.setPayload(step, step(getString(Res.string.step_cant_read, host), StepState.Failed))
            chats.add(chatId, ChatRole.Assistant, getString(Res.string.agent_blocked), account.id)
            chats.add(chatId, ChatRole.Assistant, "", account.id, MessageKind.Blocked, cardJson.encodeToString(BlockedCard(url)))
            return
        }
        chats.setPayload(step, step(getString(Res.string.step_read, host), StepState.Done))
        fromText(chatId, account, "${read.title}\n\n${read.text}\n\nEmails on the page: ${read.emails.joinToString()}", userText = text)
    }

    // The Kiddo browser handed back what the user saw on a page Kiddo couldn't fetch.
    suspend fun fromBrowser(chatId: String, account: ProviderAccount, url: String, pageText: String) {
        val host = url.substringAfter("://").substringBefore('/')
        chats.add(chatId, ChatRole.Assistant, "", account.id, MessageKind.Step, step(getString(Res.string.step_browser, host), StepState.Done))
        fromText(chatId, account, pageText, userText = url)
    }

