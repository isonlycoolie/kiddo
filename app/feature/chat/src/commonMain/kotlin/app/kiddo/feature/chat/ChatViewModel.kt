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

