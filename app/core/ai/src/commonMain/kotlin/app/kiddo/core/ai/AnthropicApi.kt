package app.kiddo.core.ai

import app.kiddo.core.model.ChatRole
import app.kiddo.core.model.Outcome
import app.kiddo.core.model.map
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

// Anthropic's Messages API with server-sent events.
internal class AnthropicApi(private val http: HttpClient) : AiClient {
    override suspend fun models(target: AiTarget): Outcome<List<String>> =
        http.getJson("${target.baseUrl.trimSlash()}/v1/models?limit=100") { auth(target) }.map { body ->
            body.obj()?.get("data").arr().orEmpty().mapNotNull { it.obj()?.get("id").str() }
        }

    override fun stream(target: AiTarget, system: String, turns: List<AiTurn>): Flow<AiEvent> {
        val body = buildJsonObject {
            put("model", target.model)
            put("max_tokens", MAX_TOKENS)
            put("stream", true)
            put("system", system)
            putJsonArray("messages") {
                turns.forEach { turn ->
                    addJsonObject {
                        put("role", if (turn.role == ChatRole.User) "user" else "assistant")
                        put("content", turn.text)
                    }
                }
            }
        }
        var input = 0L
        return http.streamSse("${target.baseUrl.trimSlash()}/v1/messages", body, { auth(target) }) { event ->
            val root = event.obj() ?: return@streamSse
            when (root["type"].str()) {
                "message_start" -> input = root["message"].obj()?.get("usage").obj()?.get("input_tokens").long() ?: 0
                "content_block_delta" -> root["delta"].obj()?.get("text").str()?.let { emit(AiEvent.Text(it)) }
                "message_delta" -> root["usage"].obj()?.get("output_tokens").long()
                    ?.let { emit(AiEvent.Usage(input, it)) }
                "error" -> emit(AiEvent.Failed(failureFor(OVERLOADED, root.toString())))
            }
        }
    }

    private fun HttpRequestBuilder.auth(target: AiTarget) {
        header("x-api-key", target.key)
        header("anthropic-version", VERSION)
    }

    private companion object {
        const val VERSION = "2023-06-01"
        const val MAX_TOKENS = 4096
        const val OVERLOADED = 529
    }
}
