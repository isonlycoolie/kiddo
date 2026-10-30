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

    private suspend fun fromText(chatId: String, account: ProviderAccount, post: String, userText: String) {
        val target = target(account)
        val applicant = applicant()
        val extracted = ai.complete(target, AgentPrompts.EXTRACT, AgentPrompts.extractPrompt(post, applicant.summary))
        val job = (extracted as? Outcome.Done)?.value?.let { parseModelJson<JobCard>(it) }
        if (job == null || (job.role.isBlank() && job.company.isBlank())) {
            chats.add(chatId, ChatRole.Assistant, getString(Res.string.agent_failed_read), account.id, kind = MessageKind.Text)
            return
        }
        val found = if (job.company.isNotBlank()) getString(Res.string.agent_found, job.company, job.role) else getString(Res.string.agent_found_role, job.role)
        chats.add(chatId, ChatRole.Assistant, found, account.id)
        chats.add(chatId, ChatRole.Assistant, "", account.id, MessageKind.Job, cardJson.encodeToString(job))
        if (job.missing.isNotEmpty()) {
            chats.add(chatId, ChatRole.Assistant, getString(Res.string.agent_missing, job.missing.joinToString(", ")), account.id)
        }
        val cv = applicant.cv
        val cvLine = cv?.let { getString(Res.string.step_cv, it.name) } ?: getString(Res.string.step_no_cv)
        chats.add(chatId, ChatRole.Assistant, "", account.id, MessageKind.Step, step(cvLine, StepState.Done))
        val writing = chats.add(chatId, ChatRole.Assistant, "", account.id, MessageKind.Step, step(getString(Res.string.step_writing), StepState.Running))
        val drafted = ai.complete(target, AgentPrompts.DRAFT, AgentPrompts.draftPrompt(cardJson.encodeToString(job), applicant.full, applicant.tone, applicant.signature))
        val draft = (drafted as? Outcome.Done)?.value?.let { parseModelJson<DraftText>(it) }
        if (draft == null) {
            chats.setPayload(writing, step(getString(Res.string.step_writing), StepState.Failed))
            chats.add(chatId, ChatRole.Assistant, getString(Res.string.agent_failed_read), account.id)
            return
        }
        chats.setPayload(writing, step(getString(Res.string.step_writing), StepState.Done))
        val to = job.applyEmail.trim()
        val intro = if (to.isEmpty()) getString(Res.string.agent_site) else getString(Res.string.agent_draft_intro)
        chats.add(chatId, ChatRole.Assistant, intro, account.id)
        val card = DraftCard(
            to = to,
            subject = draft.subject,
            body = draft.body,
            role = job.role,
            company = job.company,
            cvId = cv?.id,
            cvName = cv?.name,
            fromPage = to.isNotEmpty() && !userText.contains(to, ignoreCase = true),
            applyUrl = job.applyUrl,
        )
        chats.add(chatId, ChatRole.Assistant, "", account.id, MessageKind.Draft, cardJson.encodeToString(card))
    }

    private suspend fun target(account: ProviderAccount) =
        AiTarget(account.provider, account.baseUrl, account.model, vault.secrets.get(account.keyRef).orEmpty())

    private class Applicant(val summary: String, val full: String, val tone: String, val signature: String, val cv: Cv?)

    private suspend fun applicant(): Applicant {
        val ws = workspaces.current() ?: return Applicant("", "", "", "", null)
        val profile = workspaces.profile.first()
        val all = cvs.cvs(ws.id).first()
        val cv = all.firstOrNull { it.isDefault } ?: all.firstOrNull()
        val details = listOf(
            "Name: ${ws.name}",
            "Email: ${ws.email}",
            "Headline: ${profile.headline}",
            "Phone: ${profile.phone}",
            "Portfolio: ${profile.portfolio}",
            "LinkedIn: ${profile.linkedin}",
        ).filterNot { it.endsWith(": ") }.joinToString("\n")
        val cvText = cv?.text?.take(CV_LIMIT).orEmpty()
        val sign = if (profile.signature == Signature.NameAndPhone && profile.phone.isNotBlank()) "${ws.name}\n${profile.phone}" else ws.name
        return Applicant(details, "$details\n\nCV:\n$cvText", profile.tone.name, sign, cv)
    }

    private fun step(text: String, state: StepState) = cardJson.encodeToString(StepCard(text, state))

    @kotlinx.serialization.Serializable
    private data class DraftText(val subject: String = "", val body: String = "")

    private companion object {
        const val PASTED_POST = 350
        const val CV_LIMIT = 8_000
    }
}
