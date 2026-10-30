package app.kiddo.core.database

import app.kiddo.core.security.PassphraseBox
import app.kiddo.core.security.SecretStore
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

// Everything Kiddo holds, as one file sealed with the user's passphrase. Inside it, values are plain (the phone's own
// keys can't travel to another phone); the passphrase is what protects them. Keys and passwords go in only when the
// user ticks "Include keys and passwords".
@OptIn(ExperimentalEncodingApi::class)
class BackupService(private val s: Storage, private val secrets: SecretStore) {
    suspend fun export(passphrase: String, includeKeys: Boolean): ByteArray = withContext(s.io) {
        val q = s.q
        val ws = q.workspaceFirst().executeAsOneOrNull() ?: error("No workspace")
        val open: (String?) -> String? = { it?.let(s.cipher::open) }
        val data = BackupData(
            workspace = Row(ws.id, mapOf("name" to open(ws.name_sealed), "email" to open(ws.email_sealed), "profile" to open(ws.profile_sealed)), ws.created_at),
            settings = q.allSettings().executeAsList().map { Row(it.key, mapOf("value" to it.value_)) },
            providers = q.allProviders().executeAsList().map {
                Row(it.id, mapOf("provider" to it.provider, "base" to it.base_url, "model" to it.model, "ref" to it.key_ref, "tokens" to it.tokens_used.toString(), "key" to secret(it.key_ref, includeKeys)), it.created_at)
            },
            chats = q.allChats().executeAsList().map { Row(it.id, mapOf("title" to open(it.title_sealed), "provider" to it.provider_account_id), it.updated_at) },
            messages = q.allMessages().executeAsList().map {
                Row(it.id, mapOf("chat" to it.chat_id, "role" to it.role, "text" to open(it.text_sealed), "failed" to it.failed.toString(), "kind" to it.kind, "payload" to open(it.payload_sealed)), it.created_at)
            },
            cvs = q.allCvs().executeAsList().map {
                val file = s.files.read("cv-${it.id}.bin")?.let(s.cipher::openBytes)?.let { bytes -> Base64.encode(bytes) }
                Row(it.id, mapOf("name" to open(it.name_sealed), "format" to it.format, "pages" to it.pages.toString(), "text" to open(it.text_sealed), "default" to it.is_default.toString(), "kiddo" to it.by_kiddo.toString(), "file" to file), it.added_at)
            },
            mail = q.allMail().executeAsList().map {
                Row(it.id, mapOf("kind" to it.kind, "address" to open(it.address_sealed), "name" to open(it.display_name_sealed), "user" to open(it.username_sealed), "smtp" to open(it.smtp_sealed), "imap" to open(it.imap_sealed), "sendly" to open(it.sendly_sealed), "ref" to it.secret_ref, "secret" to secret(it.secret_ref, includeKeys)), it.created_at)
            },
            applications = q.allApplications().executeAsList().map {
                Row(it.id, mapOf("role" to open(it.role_sealed), "company" to open(it.company_sealed), "to" to open(it.to_sealed), "status" to it.status, "chat" to it.chat_id, "mid" to open(it.message_id_sealed), "cv" to open(it.cv_name_sealed), "follow" to it.follow_ups.toString(), "reply" to open(it.reply_sealed), "replyAt" to it.reply_at?.toString(), "sent" to it.sent_at.toString()), it.updated_at)
            },
            notices = q.allNotices().executeAsList().map {
                Row(it.id, mapOf("kind" to it.kind, "app" to it.application_id, "body" to open(it.body_sealed), "read" to it.read.toString()), it.created_at)
            },
        )
        PassphraseBox.seal(json.encodeToString(BackupData.serializer(), data).encodeToByteArray(), passphrase)
    }

    // Null when the passphrase is wrong or the file isn't a Kiddo backup. On success, everything here is replaced.
    suspend fun restore(file: ByteArray, passphrase: String): Boolean = withContext(s.io) {
        val plain = PassphraseBox.open(file, passphrase) ?: return@withContext false
        val data = runCatching { json.decodeFromString(BackupData.serializer(), plain.decodeToString()) }.getOrNull() ?: return@withContext false
        val q = s.q
        val seal: (String?) -> String? = { it?.let(s.cipher::seal) }
        val ws = data.workspace
        q.transaction {
            listOf(q::wipeWorkspace, q::wipeSettings, q::wipeProviders, q::wipeChats, q::wipeMessages, q::wipeCvs, q::wipeMail, q::wipeApplications, q::wipeNotices).forEach { it() }
            q.insertWorkspace(ws.id, seal(ws.v["name"]).orEmpty(), seal(ws.v["email"]).orEmpty(), ws.at)
            q.updateWorkspace(seal(ws.v["name"]).orEmpty(), seal(ws.v["email"]).orEmpty(), seal(ws.v["profile"]), ws.id)
            data.settings.forEach { q.putSetting(ws.id, it.id, it.v["value"].orEmpty()) }
            data.providers.forEach {
                q.restoreProvider(it.id, ws.id, it.v["provider"].orEmpty(), it.v["base"].orEmpty(), it.v["model"].orEmpty(), it.v["ref"].orEmpty(), it.v["tokens"]?.toLongOrNull() ?: 0, it.at)
                it.v["key"]?.let { key -> secrets.put(it.v["ref"].orEmpty(), key) }
            }
            data.chats.forEach { q.insertChat(it.id, ws.id, seal(it.v["title"]).orEmpty(), it.v["provider"], it.at) }
            data.messages.forEach {
                q.insertMessage(it.id, it.v["chat"].orEmpty(), it.v["role"].orEmpty(), seal(it.v["text"]).orEmpty(), it.v["failed"]?.toLongOrNull() ?: 0, it.at, it.v["kind"] ?: "Text", seal(it.v["payload"]))
            }
            data.cvs.forEach {
                it.v["file"]?.let { file -> s.files.write("cv-${it.id}.bin", s.cipher.sealBytes(Base64.decode(file))) }
                q.insertCv(it.id, ws.id, seal(it.v["name"]).orEmpty(), it.v["format"].orEmpty(), it.v["pages"]?.toLongOrNull() ?: 1, seal(it.v["text"]), it.v["default"]?.toLongOrNull() ?: 0, it.v["kiddo"]?.toLongOrNull() ?: 0, it.at)
            }
            data.mail.forEach {
                q.insertMailAccount(it.id, ws.id, it.v["kind"].orEmpty(), seal(it.v["address"]).orEmpty(), seal(it.v["name"]).orEmpty(), seal(it.v["user"]).orEmpty(), seal(it.v["smtp"]), seal(it.v["imap"]), seal(it.v["sendly"]), it.v["ref"].orEmpty(), null, it.at)
                it.v["secret"]?.let { secret -> secrets.put(it.v["ref"].orEmpty(), secret) }
            }
            data.applications.forEach {
                q.restoreApplication(
                    it.id, ws.id, seal(it.v["role"]).orEmpty(), seal(it.v["company"]).orEmpty(), seal(it.v["to"]).orEmpty(), it.v["status"].orEmpty(),
                    it.v["chat"], seal(it.v["mid"]), seal(it.v["cv"]), it.v["follow"]?.toLongOrNull() ?: 0, seal(it.v["reply"]),
                    it.v["replyAt"]?.toLongOrNull(), it.v["sent"]?.toLongOrNull() ?: it.at, it.at,
                )
            }
            data.notices.forEach {
                q.restoreNotice(it.id, ws.id, it.v["kind"].orEmpty(), it.v["app"], seal(it.v["body"]).orEmpty(), it.v["read"]?.toLongOrNull() ?: 0, it.at)
            }
        }
        true
    }

    private fun secret(ref: String, include: Boolean): String? = if (include) secrets.get(ref) else null

    @Serializable
    private data class Row(val id: String, val v: Map<String, String?>, val at: Long = 0)

    @Serializable
    private data class BackupData(
        val version: Int = 1,
        val workspace: Row,
        val settings: List<Row>,
        val providers: List<Row>,
        val chats: List<Row>,
        val messages: List<Row>,
        val cvs: List<Row>,
        val mail: List<Row>,
        val applications: List<Row>,
        val notices: List<Row>,
    )

    private val json = Json { ignoreUnknownKeys = true }
}
