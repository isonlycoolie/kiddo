package app.kiddo.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.kiddo.core.ai.AiClient
import app.kiddo.core.ai.AiEvent
import app.kiddo.core.ai.AiTarget
import app.kiddo.core.ai.AiTurn
import app.kiddo.core.database.ChatStore
import app.kiddo.core.database.CvStore
import app.kiddo.core.database.MailStore
import app.kiddo.core.database.NoticeStore
import app.kiddo.core.database.ProviderStore
import app.kiddo.core.database.SettingsStore
import app.kiddo.core.database.WorkspaceStore
import app.kiddo.core.model.Chat
import app.kiddo.core.model.ChatMessage
import app.kiddo.core.model.ChatRole
import app.kiddo.core.model.Failure
import app.kiddo.core.model.ProviderAccount
import app.kiddo.core.security.Vault
import app.kiddo.feature.chat.agent.CvBuilder
import app.kiddo.feature.chat.agent.CvBuiltCard
import app.kiddo.feature.chat.agent.JobAgent
import app.kiddo.feature.chat.agent.Outbox
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class Setup(val model: Boolean, val mail: Boolean, val cv: Boolean) {
    val done: Boolean get() = model && mail && cv
}

// What's live right now: the answer being streamed, and whether a run is going.
data class Live(val chatId: String? = null, val streamingId: String? = null, val streamed: String = "", val running: Boolean = false)

data class ChatState(
    val name: String = "",
    val mail: app.kiddo.core.model.MailAccount? = null,
    val followUpDays: Int = 7,
    val unread: Int = 0,
    val setup: Setup = Setup(model = false, mail = false, cv = false),
    val accounts: List<ProviderAccount> = emptyList(),
    val active: ProviderAccount? = null,
    val messages: List<ChatMessage> = emptyList(),
    val recent: List<Chat> = emptyList(),
    val live: Live = Live(),
    val loaded: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
    private val workspaces: WorkspaceStore,
    private val providers: ProviderStore,
    private val settings: SettingsStore,
    private val chats: ChatStore,
    private val cvs: CvStore,
    mail: MailStore,
    private val vault: Vault,
    private val ai: AiClient,
    private val prompt: PromptBuilder,
    private val agent: JobAgent,
    private val builder: CvBuilder,
    val outbox: Outbox,
    private val notices: NoticeStore,
) : ViewModel() {
    private val live = MutableStateFlow(Live())
    private var run: Job? = null

    val state: StateFlow<ChatState> = workspaces.workspace.filterNotNull().flatMapLatest { ws ->
        val base = combine(
            providers.accounts(ws.id),
            settings.settings(ws.id),
            mail.accounts(ws.id),
            cvs.cvs(ws.id),
            chats.chats(ws.id),
        ) { accounts, s, mails, cvList, chatList ->
            ChatState(
                name = ws.name.substringBefore(' '),
                setup = Setup(accounts.isNotEmpty(), mails.isNotEmpty(), cvList.isNotEmpty()),
                mail = mails.firstOrNull(),
                followUpDays = s.followUpDays,
                accounts = accounts,
                active = accounts.firstOrNull { it.id == s.chatModel } ?: accounts.firstOrNull(),
                recent = chatList.take(RECENT),
                loaded = true,
            )
        }
        combine(
            base,
            live.flatMapLatest { l -> l.chatId?.let { chats.messages(it) } ?: flowOf(emptyList()) },
            live,
            notices.notices(ws.id),
        ) { b, m, l, n -> b.copy(messages = m, live = l, unread = n.count { !it.read }) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER), ChatState())

    fun open(chatId: String?) {
        if (live.value.chatId == chatId) return
        stop()
        live.value = Live(chatId = chatId)
    }

    fun pickModel(accountId: String) {
        viewModelScope.launch { workspaces.current()?.let { ws -> settings.update(ws.id) { it.copy(chatModel = accountId) } } }
    }

    fun send(text: String) {
        val message = text.trim()
        val account = state.value.active ?: return
        if (message.isEmpty() || live.value.running) return
        run = viewModelScope.launch {
            val ws = workspaces.current() ?: return@launch
            val chatId = live.value.chatId ?: chats.start(ws.id, message.lineSequence().first(), account.id)
            chats.add(chatId, ChatRole.User, message, account.id)
            if (agent.isJob(message)) {
                working(chatId) { agent.fromMessage(chatId, account, message) }
                return@launch
            }
            val answerId = chats.add(chatId, ChatRole.Assistant, "", account.id)
            live.value = Live(chatId, answerId, "", running = true)
            answer(account, chatId, answerId)
        }
    }

    // What the user saw in the Kiddo browser, handed back for the chat that asked for it.
    fun fromBrowser(url: String, pageText: String) {
        val account = state.value.active ?: return
        val chatId = live.value.chatId ?: return
        if (pageText.isBlank() || live.value.running) return
        run = viewModelScope.launch { working(chatId) { agent.fromBrowser(chatId, account, url, pageText) } }
    }

    // "Improve with Kiddo" from a CV, or the CV starter: a new chat that rebuilds the CV.
    fun improveCv(cvId: String?, title: String) {
        val account = state.value.active ?: return
        if (live.value.running) return
        run = viewModelScope.launch {
            val ws = workspaces.current() ?: return@launch
            val chatId = chats.start(ws.id, title, account.id)
            chats.add(chatId, ChatRole.User, title, account.id)
            live.value = Live(chatId = chatId)
            working(chatId) { builder.improve(chatId, account, cvId) }
        }
    }

    fun saveCv(messageId: String, onSaved: (String?) -> Unit) {
        viewModelScope.launch { onSaved(builder.save(messageId)) }
    }

