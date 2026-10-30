package app.kiddo.core.model

enum class MailKind { GmailAppPassword, Smtp, Sendly }

enum class MailSecurity { SslTls, StartTls }

data class MailServer(val host: String, val port: Int, val security: MailSecurity)

// The account Kiddo sends from. Its password or API key lives in the secret store under `secretRef`.
data class MailAccount(
    val id: String,
    val kind: MailKind,
    val address: String,
    val displayName: String,
    val username: String,
    val smtp: MailServer?,
    val imap: MailServer?,
    val sendly: SendlyConfig?,
    val secretRef: String,
    val lastCheckedAt: Long?,
    val createdAt: Long,
) {
    val readsReplies: Boolean get() = imap != null
}

// Sendly sends through its developer API (POST /v1/messages, Bearer key) from a verified sender on the user's domain.
// That API takes text only, so a CV can't be attached when sending through Sendly.
data class SendlyConfig(val baseUrl: String)

object GmailServers {
    val Smtp = MailServer("smtp.gmail.com", 465, MailSecurity.SslTls)
    val Imap = MailServer("imap.gmail.com", 993, MailSecurity.SslTls)
}

// A plain email ready to leave: text body, optional PDF attachment.
data class OutgoingMail(
    val from: String,
    val fromName: String,
    val to: List<String>,
    val subject: String,
    val body: String,
    val attachment: MailAttachment? = null,
    // The Message-ID this answers, so a follow-up sits in the same thread as the application.
    val inReplyTo: String? = null,
)

class MailAttachment(val fileName: String, val mimeType: String, val bytes: ByteArray)
