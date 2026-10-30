package app.kiddo.core.model

// The three wire formats every provider speaks one of.
enum class ProviderApi { OpenAiCompatible, Anthropic, Gemini }

// The providers Kiddo knows by name. `Other` is any OpenAI-compatible URL the user types, local models included.
enum class AiProvider(
    val label: String,
    val api: ProviderApi,
    val baseUrl: String,
    val keyPage: String,
    val needsKey: Boolean = true,
) {
    Anthropic("Anthropic", ProviderApi.Anthropic, "https://api.anthropic.com", "console.anthropic.com"),
    OpenAi("OpenAI", ProviderApi.OpenAiCompatible, "https://api.openai.com/v1", "platform.openai.com/api-keys"),
    Gemini("Google Gemini", ProviderApi.Gemini, "https://generativelanguage.googleapis.com", "aistudio.google.com/apikey"),
    DeepSeek("DeepSeek", ProviderApi.OpenAiCompatible, "https://api.deepseek.com", "platform.deepseek.com/api_keys"),
    Mistral("Mistral", ProviderApi.OpenAiCompatible, "https://api.mistral.ai/v1", "console.mistral.ai/api-keys"),
    XAi("xAI", ProviderApi.OpenAiCompatible, "https://api.x.ai/v1", "console.x.ai"),
    Groq("Groq", ProviderApi.OpenAiCompatible, "https://api.groq.com/openai/v1", "console.groq.com/keys"),
    OpenRouter("OpenRouter", ProviderApi.OpenAiCompatible, "https://openrouter.ai/api/v1", "openrouter.ai/keys"),
    Ollama("Ollama", ProviderApi.OpenAiCompatible, "http://192.168.1.10:11434/v1", "ollama.com", needsKey = false),
    Other("Other", ProviderApi.OpenAiCompatible, "", "", needsKey = false),
}

// One connected provider. Its key lives in the secret store under `keyRef`, never here.
data class ProviderAccount(
    val id: String,
    val provider: AiProvider,
    val baseUrl: String,
    val model: String,
    val keyRef: String,
    val tokensUsed: Long,
    val createdAt: Long,
)

enum class ChatRole { User, Assistant }

// What a chat line is: plain text, or a card the app drew (a step Kiddo took, a job it read, a draft, a CV it made).
// Cards carry their data as JSON in `payload`; only the chat feature reads it.
enum class MessageKind { Text, Step, Job, Draft, Blocked, CvBuilt }

data class ChatMessage(
    val id: String,
    val chatId: String,
    val role: ChatRole,
    val text: String,
    val createdAt: Long,
    val failed: Boolean = false,
    val kind: MessageKind = MessageKind.Text,
    val payload: String? = null,
)

data class Chat(val id: String, val title: String, val providerAccountId: String?, val updatedAt: Long)

data class TokenUsage(val input: Long, val output: Long) {
    val total: Long get() = input + output
}
