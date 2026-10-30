package app.kiddo.feature.chat.agent

import app.kiddo.core.ai.AiClient
import app.kiddo.core.ai.AiTarget
import app.kiddo.core.ai.complete
import app.kiddo.core.database.ChatStore
import app.kiddo.core.database.CvStore
import app.kiddo.core.database.WorkspaceStore
import app.kiddo.core.model.ChatRole
import app.kiddo.core.model.CvData
import app.kiddo.core.model.CvFormat
import app.kiddo.core.model.MessageKind
import app.kiddo.core.model.Outcome
import app.kiddo.core.model.ProviderAccount
import app.kiddo.core.platform.CvReader
import app.kiddo.core.platform.CvRenderer
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.agent_cv_failed
import app.kiddo.core.resources.agent_cv_intro
import app.kiddo.core.resources.agent_cv_no_text
import app.kiddo.core.resources.agent_cv_none
import app.kiddo.core.resources.cv_clean_name
import app.kiddo.core.resources.step_read
import app.kiddo.core.resources.step_reading
import app.kiddo.core.security.Vault
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import org.jetbrains.compose.resources.getString

// "Improve with Kiddo": the model restructures the CV's own facts, the phone draws a clean PDF. The original file is
// never changed; saving adds a new CV.
class CvBuilder(
    private val chats: ChatStore,
    private val cvs: CvStore,
    private val workspaces: WorkspaceStore,
    private val ai: AiClient,
    private val vault: Vault,
    private val renderer: CvRenderer,
    private val reader: CvReader,
) {
    suspend fun improve(chatId: String, account: ProviderAccount, cvId: String?) {
        val ws = workspaces.current() ?: return
        val all = cvs.cvs(ws.id).first()
        val cv = all.firstOrNull { it.id == cvId } ?: all.firstOrNull { it.isDefault } ?: all.firstOrNull()
        if (cv == null) {
            chats.add(chatId, ChatRole.Assistant, getString(Res.string.agent_cv_none), account.id)
            return
        }
        val step = chats.add(chatId, ChatRole.Assistant, "", account.id, MessageKind.Step, step(getString(Res.string.step_reading, cv.name), StepState.Running))
        val text = cv.text
        if (text.isNullOrBlank()) {
            chats.setPayload(step, step(getString(Res.string.step_reading, cv.name), StepState.Failed))
            chats.add(chatId, ChatRole.Assistant, getString(Res.string.agent_cv_no_text), account.id)
            return
        }
        val target = AiTarget(account.provider, account.baseUrl, account.model, vault.secrets.get(account.keyRef).orEmpty())
        val built = (ai.complete(target, AgentPrompts.CV, AgentPrompts.cvPrompt(text)) as? Outcome.Done)?.value?.let { parseModelJson<CvData>(it) }
        if (built == null || built.sections.isEmpty()) {
            chats.setPayload(step, step(getString(Res.string.step_reading, cv.name), StepState.Failed))
            chats.add(chatId, ChatRole.Assistant, getString(Res.string.agent_cv_failed), account.id)
            return
        }
        chats.setPayload(step, step(getString(Res.string.step_read, cv.name), StepState.Done))
        chats.add(chatId, ChatRole.Assistant, getString(Res.string.agent_cv_intro), account.id)
        chats.add(chatId, ChatRole.Assistant, "", account.id, MessageKind.CvBuilt, cardJson.encodeToString(CvBuiltCard(built, cv.name)))
    }

    suspend fun render(cv: CvData): ByteArray = renderer.render(cv)

    // Draws the PDF again (the same input gives the same pages) and stores it as a new CV made by Kiddo.
    suspend fun save(messageId: String): String? {
        val ws = workspaces.current() ?: return null
        val message = chats.message(messageId) ?: return null
        val card = message.payload?.let { parseModelJson<CvBuiltCard>(it) } ?: return null
        card.savedCvId?.let { return it }
        val bytes = renderer.render(card.cv)
        val pages = reader.read(bytes, CvFormat.Pdf)?.pages ?: 1
        val text = reader.read(bytes, CvFormat.Pdf)?.text ?: plainText(card.cv)
        val id = cvs.add(ws.id, getString(Res.string.cv_clean_name, card.sourceName), CvFormat.Pdf, pages, text, bytes, byKiddo = true)
        chats.setPayload(messageId, cardJson.encodeToString(card.copy(savedCvId = id)))
        return id
    }

    // The CV's text for the model when the phone can't read the PDF it just drew.
    private fun plainText(cv: CvData) = buildString {
        appendLine(cv.name)
        appendLine(cv.headline)
        appendLine(cv.contact.joinToString(" · "))
        appendLine(cv.summary)
        cv.sections.forEach { s ->
            appendLine(s.title)
            s.items.forEach { i ->
                appendLine("${i.title} ${i.subtitle} ${i.dates}")
                i.bullets.forEach { appendLine("- $it") }
            }
        }
    }

    private fun step(text: String, state: StepState) = cardJson.encodeToString(StepCard(text, state))
}
