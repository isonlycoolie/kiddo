package app.kiddo.core.mail

import app.kiddo.core.model.Failure
import app.kiddo.core.model.FoundReply
import app.kiddo.core.model.MailAccount
import app.kiddo.core.model.Outcome
import app.kiddo.core.model.OutgoingMail
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// Sendly's developer API. The key is checked against /balance (it sends nothing); mail goes to /messages on the email
// channel from the sender the user verified in Sendly.
class SendlyTransport(private val http: HttpClient) : MailTransport {
    override suspend fun check(account: MailAccount, secret: String): Outcome<MailCheck> =
        request { http.get("${base(account)}/balance") { auth(secret) } }.let { result ->
            when (result) {
                is Outcome.Done -> Outcome.Done(MailCheck(sending = true, reading = null))
                is Outcome.Failed -> result
            }
        }

    override suspend fun replies(account: MailAccount, secret: String, since: Long): Outcome<List<FoundReply>> =
        Outcome.Done(emptyList())

    override suspend fun send(account: MailAccount, secret: String, mail: OutgoingMail): Outcome<String> = request {
        http.post("${base(account)}/messages") {
            auth(secret)
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("channel", "email")
                    put("to", mail.to.first())
                    put("from", mail.from)
                    put("subject", mail.subject)
                    put("text", mail.body)
                }.toString(),
            )
        }
    }.let { result -> if (result is Outcome.Done) Outcome.Done("") else result as Outcome.Failed }

    private fun base(account: MailAccount) = (account.sendly?.baseUrl ?: DEFAULT_BASE).trimEnd('/')

    private fun io.ktor.client.request.HttpRequestBuilder.auth(secret: String) {
        header("Authorization", "Bearer $secret")
    }

    private suspend fun request(block: suspend () -> HttpResponse): Outcome<Unit> = try {
        val status = block().status.value
        when {
            status in 200..299 -> Outcome.Done(Unit)
            status == 401 || status == 403 -> Outcome.Failed(Failure.WrongKey)
            status == 402 -> Outcome.Failed(Failure.NoCredit)
            status == 429 -> Outcome.Failed(Failure.RateLimited)
            status in 400..499 -> Outcome.Failed(Failure.BadInput)
            else -> Outcome.Failed(Failure.CantReach)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        Outcome.Failed(Failure.CantReach)
    }

    companion object {
        const val DEFAULT_BASE = "https://api.usesendly.cc/v1"
    }
}
