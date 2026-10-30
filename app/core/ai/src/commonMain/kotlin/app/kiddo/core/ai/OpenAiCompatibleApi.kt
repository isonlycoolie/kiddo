package app.kiddo.core.ai

import app.kiddo.core.model.AiProvider
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
import kotlinx.serialization.json.putJsonObject

// The `/chat/completions` format: OpenAI, DeepSeek, Mistral, xAI, Groq, OpenRouter, Ollama and any compatible URL.
internal class OpenAiCompatibleApi(private val http: HttpClient) : AiClient {
    override suspend fun models(target: AiTarget): Outcome<List<String>> =
        http.getJson("${target.baseUrl.trimSlash()}/models") { auth(target) }.map { body ->
            body.obj()?.get("data").arr().orEmpty().mapNotNull { it.obj()?.get("id").str() }.sorted()
        }

    override fun stream(target: AiTarget, system: String, turns: List<AiTurn>): Flow<AiEvent> {
        val body = buildJsonObject {
            put("model", target.model)
            put("stream", true)
            // Only OpenAI is known to accept this; other servers may reject unknown fields.
            if (target.provider == AiProvider.OpenAi) putJsonObject("stream_options") { put("include_usage", true) }
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "system")
                    put("content", system)
                }
                turns.forEach { turn ->
                    addJsonObject {
                        put("role", if (turn.role == ChatRole.User) "user" else "assistant")
                        put("content", turn.text)
                    }
                }
            }
        }
        return http.streamSse("${target.baseUrl.trimSlash()}/chat/completions", body, { auth(target) }) { event ->
            val root = event.obj() ?: return@streamSse
            root["choices"].arr()?.firstOrNull()?.obj()?.get("delta")?.obj()?.get("content").str()
                ?.takeIf { it.isNotEmpty() }
                ?.let { emit(AiEvent.Text(it)) }
            root["usage"].obj()?.let { usage ->
                emit(AiEvent.Usage(usage["prompt_tokens"].long() ?: 0, usage["completion_tokens"].long() ?: 0))
            }
        }
    }

    private fun HttpRequestBuilder.auth(target: AiTarget) {
        if (target.key.isNotBlank()) header("Authorization", "Bearer ${target.key}")
        // OpenRouter asks apps to name themselves; harmless elsewhere.
        if (target.provider == AiProvider.OpenRouter) header("X-Title", "Kiddo")
    }
}
