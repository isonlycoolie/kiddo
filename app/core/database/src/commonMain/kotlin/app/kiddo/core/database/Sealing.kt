package app.kiddo.core.database

import app.kiddo.core.model.MailSecurity
import app.kiddo.core.model.MailServer
import app.kiddo.core.model.Profile
import app.kiddo.core.model.SendlyConfig
import app.kiddo.core.model.Signature
import app.kiddo.core.model.Tone
import app.kiddo.core.security.FieldCipher

// Small records stored as one sealed value: parts joined by the ASCII unit separator, which no typed text contains.
internal object Codec {
    private const val SEP = "\u001F"

    fun profile(p: Profile): String = listOf(p.headline, p.phone, p.portfolio, p.linkedin, p.tone.name, p.signature.name)
        .joinToString(SEP)

    fun profile(raw: String?): Profile {
        val parts = raw?.split(SEP) ?: return Profile()
        if (parts.size < PROFILE_PARTS) return Profile()
        return Profile(
            headline = parts[0],
            phone = parts[1],
            portfolio = parts[2],
            linkedin = parts[3],
            tone = Tone.entries.firstOrNull { it.name == parts[4] } ?: Tone.Friendly,
            signature = Signature.entries.firstOrNull { it.name == parts[5] } ?: Signature.NameAndPhone,
        )
    }

    fun server(s: MailServer): String = listOf(s.host, s.port.toString(), s.security.name).joinToString(SEP)

    fun server(raw: String?): MailServer? {
        val parts = raw?.split(SEP) ?: return null
        val port = parts.getOrNull(1)?.toIntOrNull() ?: return null
        val security = MailSecurity.entries.firstOrNull { it.name == parts.getOrNull(2) } ?: return null
        return MailServer(parts[0], port, security)
    }

    fun sendly(c: SendlyConfig): String = c.baseUrl

    fun sendly(raw: String?): SendlyConfig? = raw?.takeIf { it.isNotBlank() }?.let(::SendlyConfig)

    private const val PROFILE_PARTS = 6
}

internal fun FieldCipher.openOrEmpty(sealed: String): String = open(sealed).orEmpty()

internal fun FieldCipher.sealOrNull(plain: String?): String? = plain?.let(::seal)

internal fun FieldCipher.openOrNull(sealed: String?): String? = sealed?.let(::open)
