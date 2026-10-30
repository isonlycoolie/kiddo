package app.kiddo.core.mail

import app.kiddo.core.model.Failure
import app.kiddo.core.model.FoundReply
import app.kiddo.core.model.MailAccount
import app.kiddo.core.model.Outcome
import app.kiddo.core.model.OutgoingMail
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

// Gmail (app password) and custom domains: SMTP to send, IMAP to prove replies can be read. All on `io`, the
// platform's pool for blocking work; `now` dates each message.
class SocketTransport(private val io: CoroutineContext, private val now: () -> Long) : MailTransport {
    override suspend fun check(account: MailAccount, secret: String): Outcome<MailCheck> = guarded {
        val smtp = account.smtp ?: return@guarded Outcome.Failed(Failure.BadInput)
        Smtp(smtp).login(account.username, secret).close()
        val reading = account.imap?.let { Imap(it).check(account.username, secret) }
        if (reading != null && reading != Failure.ImapOff) return@guarded Outcome.Failed(reading)
        Outcome.Done(MailCheck(sending = true, reading = account.imap?.let { reading == null }))
    }

    override suspend fun send(account: MailAccount, secret: String, mail: OutgoingMail): Outcome<String> = guarded {
        val smtp = account.smtp ?: return@guarded Outcome.Failed(Failure.BadInput)
        if ((mail.attachment?.bytes?.size ?: 0) > MAX_ATTACHMENT) return@guarded Outcome.Failed(Failure.TooLarge)
        val (message, messageId) = Mime.build(mail, account.address.substringAfter('@', "kiddo.local"), now())
        Smtp(smtp).login(account.username, secret).use { it.send(mail.from, mail.to, message) }
        Outcome.Done(messageId)
    }

    override suspend fun replies(account: MailAccount, secret: String, since: Long): Outcome<List<FoundReply>> = guarded {
        val imap = account.imap ?: return@guarded Outcome.Done(emptyList())
        Outcome.Done(Imap(imap).recent(account.username, secret, since))
    }

    private suspend fun <T> guarded(block: suspend () -> Outcome<T>): Outcome<T> = withContext(io) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: SmtpRefused) {
            Outcome.Failed(e.failure)
        } catch (e: MailNetworkException) {
            Outcome.Failed(if (e.tls) Failure.TlsRequired else Failure.CantReach)
        } catch (e: MailProtocolException) {
            Outcome.Failed(Failure.Unknown)
        }
    }

    private companion object {
        const val MAX_ATTACHMENT = 20 * 1024 * 1024
    }
}
