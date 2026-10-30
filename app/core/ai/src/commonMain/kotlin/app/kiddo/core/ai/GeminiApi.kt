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
import kotlinx.serialization.json.putJsonObject

// Google's Gemini API: streamGenerateContent with server-sent events.
internal class GeminiApi(private val http: HttpClient) : AiClient {
    override suspend fun models(target: AiTarget): Outcome<List<String>> =
        http.getJson("${target.baseUrl.trimSlash()}/v1beta/models?pageSize=200") { auth(target) }.map { body ->
            body.obj()?.get("models").arr().orEmpty()
                .mapNotNull { it.obj() }
                .filter { m -> m["supportedGenerationMethods"].arr().orEmpty().any { it.str() == "generateContent" } }
                .mapNotNull { it["name"].str()?.removePrefix("models/") }
        }

    override fun stream(target: AiTarget, system: String, turns: List<AiTurn>): Flow<AiEvent> {
        val body = buildJsonObject {
            putJsonObject("systemInstruction") {
                putJsonArray("parts") { addJsonObject { put("text", system) } }
            }
            putJsonArray("contents") {
                turns.forEach { turn ->
                    addJsonObject {
                        put("role", if (turn.role == ChatRole.User) "user" else "model")
                        putJsonArray("parts") { addJsonObject { put("text", turn.text) } }
                    }
                }
            }
        }
        val url = "${target.baseUrl.trimSlash()}/v1beta/models/${target.model}:streamGenerateContent?alt=sse"
        return http.streamSse(url, body, { auth(target) }) { event ->
            val root = event.obj() ?: return@streamSse
            val candidate = root["candidates"].arr()?.firstOrNull()?.obj()
            candidate?.get("content").obj()?.get("parts").arr().orEmpty()
                .mapNotNull { it.obj()?.get("text").str() }
                .forEach { emit(AiEvent.Text(it)) }
            // Every chunk repeats the running total; the one that finishes the answer holds the final count.
            val meta = root["usageMetadata"].obj()
            if (meta != null && candidate?.get("finishReason") != null) {
                emit(AiEvent.Usage(meta["promptTokenCount"].long() ?: 0, meta["candidatesTokenCount"].long() ?: 0))
            }
        }
    }

    private fun HttpRequestBuilder.auth(target: AiTarget) {
        header("x-goog-api-key", target.key)
    }
}
