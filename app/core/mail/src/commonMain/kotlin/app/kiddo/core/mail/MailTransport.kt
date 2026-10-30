package app.kiddo.core.mail

import app.kiddo.core.model.FoundReply
import app.kiddo.core.model.MailAccount
import app.kiddo.core.model.MailKind
import app.kiddo.core.model.Outcome
import app.kiddo.core.model.OutgoingMail

// What a connection test found: sending works, and whether reading replies works (null when there is no inbox set).
data class MailCheck(val sending: Boolean, val reading: Boolean?)

interface MailTransport {
    suspend fun check(account: MailAccount, secret: String): Outcome<MailCheck>

    // Returns the Message-ID the mail went out with (empty when the service doesn't say).
    suspend fun send(account: MailAccount, secret: String, mail: OutgoingMail): Outcome<String>

    // Mail that arrived since a moment, headers and a short plain-text start only. Empty when the account can't read.
    suspend fun replies(account: MailAccount, secret: String, since: Long): Outcome<List<FoundReply>>
}

// SMTP and IMAP for Gmail and custom domains; Sendly's API for Sendly. Business code sees only MailTransport.
class Mailer(private val sockets: MailTransport, private val sendly: MailTransport) : MailTransport {
    override suspend fun check(account: MailAccount, secret: String): Outcome<MailCheck> =
        pick(account).check(account, secret)

    override suspend fun send(account: MailAccount, secret: String, mail: OutgoingMail): Outcome<String> =
        pick(account).send(account, secret, mail)

    override suspend fun replies(account: MailAccount, secret: String, since: Long): Outcome<List<FoundReply>> =
        if (account.imap == null) Outcome.Done(emptyList()) else sockets.replies(account, secret, since)

    private fun pick(account: MailAccount) = if (account.kind == MailKind.Sendly) sendly else sockets
}
