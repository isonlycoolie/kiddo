package app.kiddo.core.ai

import app.kiddo.core.model.AiProvider
import app.kiddo.core.model.ChatRole
import app.kiddo.core.model.Outcome
import app.kiddo.core.model.ProviderApi
import io.ktor.client.HttpClient
import app.kiddo.core.model.Failure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// What a streaming answer sends back, piece by piece.
sealed interface AiEvent {
    data class Text(val delta: String) : AiEvent

    data class Usage(val input: Long, val output: Long) : AiEvent

    data class Failed(val reason: app.kiddo.core.model.Failure) : AiEvent
}

data class AiTurn(val role: ChatRole, val text: String)

// Where and how to call: the provider's wire format, its address, the model and the key (empty for local models).
data class AiTarget(val provider: AiProvider, val baseUrl: String, val model: String, val key: String)

interface AiClient {
    // Lists the models the key can use. Doubles as the key test: it costs no tokens.
    suspend fun models(target: AiTarget): Outcome<List<String>>

    fun stream(target: AiTarget, system: String, turns: List<AiTurn>): Flow<AiEvent>
}

// One client for every provider; each wire format is its own small adapter. No vendor SDKs.
class HttpAiClient(http: HttpClient) : AiClient {
    private val openAi = OpenAiCompatibleApi(http)
    private val anthropic = AnthropicApi(http)
    private val gemini = GeminiApi(http)

    override suspend fun models(target: AiTarget): Outcome<List<String>> =
        if (allowed(target.baseUrl)) api(target).models(target) else Outcome.Failed(Failure.TlsRequired)

    override fun stream(target: AiTarget, system: String, turns: List<AiTurn>): Flow<AiEvent> =
        if (allowed(target.baseUrl)) api(target).stream(target, system, turns) else flowOf(AiEvent.Failed(Failure.TlsRequired))

    private fun api(target: AiTarget): AiClient = when (target.provider.api) {
        ProviderApi.OpenAiCompatible -> openAi
        ProviderApi.Anthropic -> anthropic
        ProviderApi.Gemini -> gemini
    }
}

// HTTPS everywhere; plain http only to a model on this phone or the local network (Ollama, LM Studio).
internal fun allowed(baseUrl: String): Boolean {
    val url = baseUrl.trim().lowercase()
    if (url.startsWith("https://")) return true
    if (!url.startsWith("http://")) return false
    val host = url.removePrefix("http://").substringBefore('/').substringBefore(':')
    val parts = host.split('.').mapNotNull { it.toIntOrNull() }
    return host == "localhost" ||
        (parts.size == 4 && (parts[0] == 10 || parts[0] == 127 || (parts[0] == 192 && parts[1] == 168) || (parts[0] == 172 && parts[1] in 16..31)))
}

// One whole answer instead of a stream: for the steps that read a job or write a draft as JSON.
suspend fun AiClient.complete(target: AiTarget, system: String, prompt: String): Outcome<String> {
    val text = StringBuilder()
    var failure: Failure? = null
    stream(target, system, listOf(AiTurn(ChatRole.User, prompt))).collect { event ->
        when (event) {
            is AiEvent.Text -> text.append(event.delta)
            is AiEvent.Failed -> failure = event.reason
            is AiEvent.Usage -> Unit
        }
    }
    return failure?.let { Outcome.Failed(it) } ?: Outcome.Done(text.toString())
}
