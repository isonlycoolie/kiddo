package app.kiddo.core.ai

import app.kiddo.core.model.Failure
import app.kiddo.core.model.Outcome
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

internal val json = Json { ignoreUnknownKeys = true }

// HTTP status to a reason the user can act on. 400 with a quota message is also "no credit" on some providers.
internal fun failureFor(status: Int, body: String): Failure = when {
    status == 401 || status == 403 -> Failure.WrongKey
    status == 402 -> Failure.NoCredit
    status == 429 && body.contains("quota", ignoreCase = true) -> Failure.NoCredit
    status == 429 -> Failure.RateLimited
    status == 404 -> Failure.NotFound
    status == 400 && body.contains("credit", ignoreCase = true) -> Failure.NoCredit
    status in 400..499 -> Failure.BadInput
    else -> Failure.CantReach
}

internal suspend fun HttpClient.getJson(url: String, headers: HttpRequestBuilder.() -> Unit): Outcome<JsonElement> =
    call {
        val response = get(url, headers)
        val body = response.bodyAsText()
        if (response.status.value !in 200..299) {
            Outcome.Failed(failureFor(response.status.value, body))
        } else {
            Outcome.Done(json.parseToJsonElement(body))
        }
    }

// Posts a JSON body and reads the server-sent events line by line, handing each `data:` payload to `onData`.
internal fun HttpClient.streamSse(
    url: String,
    body: JsonObject,
    headers: HttpRequestBuilder.() -> Unit,
    onData: suspend kotlinx.coroutines.flow.FlowCollector<AiEvent>.(JsonElement) -> Unit,
): Flow<AiEvent> = flow {
    val failure = runCatchingIo {
        preparePost(url) {
            headers()
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }.execute { response -> readEvents(response, onData) }
    }
    if (failure != null) emit(AiEvent.Failed(failure))
}

private suspend fun kotlinx.coroutines.flow.FlowCollector<AiEvent>.readEvents(
    response: HttpResponse,
    onData: suspend kotlinx.coroutines.flow.FlowCollector<AiEvent>.(JsonElement) -> Unit,
) {
    if (response.status.value !in 200..299) {
        emit(AiEvent.Failed(failureFor(response.status.value, response.bodyAsText())))
        return
    }
    val channel = response.bodyAsChannel()
    while (true) {
        val line = channel.readUTF8Line() ?: break
        if (!line.startsWith("data:")) continue
        val data = line.removePrefix("data:").trim()
        if (data.isEmpty() || data == "[DONE]") continue
        val element = runCatching { json.parseToJsonElement(data) }.getOrNull() ?: continue
        onData(element)
    }
}

private suspend fun <T> call(block: suspend () -> Outcome<T>): Outcome<T> {
    var result: Outcome<T> = Outcome.Failed(Failure.Unknown)
    val failure = runCatchingIo { result = block() }
    return if (failure != null) Outcome.Failed(failure) else result
}

// Network trouble becomes "can't reach"; cancellation keeps cancelling.
private suspend fun runCatchingIo(block: suspend () -> Unit): Failure? = try {
    block()
    null
} catch (e: CancellationException) {
    throw e
} catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
    if (e is kotlinx.serialization.SerializationException) Failure.Unknown else Failure.CantReach
}

internal fun JsonElement?.obj(): JsonObject? = this as? JsonObject

internal fun JsonElement?.arr(): JsonArray? = this as? JsonArray

internal fun JsonElement?.str(): String? = (this as? JsonPrimitive)?.contentOrNull

internal fun JsonElement?.long(): Long? = (this as? JsonPrimitive)?.longOrNull

internal fun String.trimSlash() = trimEnd('/')
