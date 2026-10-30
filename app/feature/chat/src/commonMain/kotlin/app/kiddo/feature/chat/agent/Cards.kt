package app.kiddo.feature.chat.agent

import app.kiddo.core.model.CvData
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// The data each chat card carries, stored sealed as JSON in the message row.

@Serializable
enum class StepState { Running, Done, Failed }

@Serializable
data class StepCard(val text: String, val state: StepState)

@Serializable
data class JobCard(
    val role: String = "",
    val company: String = "",
    val location: String = "",
    val applyEmail: String = "",
    val applyUrl: String = "",
    val deadline: String = "",
    val requirements: List<String> = emptyList(),
    val missing: List<String> = emptyList(),
)

@Serializable
data class DraftCard(
    val to: String,
    val subject: String,
    val body: String,
    val role: String,
    val company: String,
    val cvId: String? = null,
    val cvName: String? = null,
    // The address came from a page or a model, not from what the user typed: the send sheet says so.
    val fromPage: Boolean = false,
    val applyUrl: String = "",
    val sent: Boolean = false,
    val applicationId: String? = null,
)

@Serializable
data class BlockedCard(val url: String)

@Serializable
data class CvBuiltCard(val cv: CvData, val sourceName: String, val savedCvId: String? = null)

internal val cardJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    encodeDefaults = true
}

// The model is asked for JSON; this takes the outermost object even when it adds words or fences around it.
internal inline fun <reified T> parseModelJson(text: String): T? {
    val start = text.indexOf('{')
    val end = text.lastIndexOf('}')
    if (start < 0 || end <= start) return null
    return runCatching { cardJson.decodeFromString<T>(text.substring(start, end + 1)) }.getOrNull()
}
