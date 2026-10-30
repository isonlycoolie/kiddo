package app.kiddo.core.designsystem.component

import app.kiddo.core.model.Failure
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.failure_app_password
import app.kiddo.core.resources.failure_bad_input
import app.kiddo.core.resources.failure_cant_reach
import app.kiddo.core.resources.failure_imap_off
import app.kiddo.core.resources.failure_no_credit
import app.kiddo.core.resources.failure_not_found
import app.kiddo.core.resources.failure_rate
import app.kiddo.core.resources.failure_tls
import app.kiddo.core.resources.failure_too_large
import app.kiddo.core.resources.failure_unknown
import app.kiddo.core.resources.failure_unreadable
import app.kiddo.core.resources.failure_wrong_key
import app.kiddo.core.resources.failure_wrong_password
import org.jetbrains.compose.resources.StringResource

// Every failure in plain words the user can act on.
fun Failure.message(): StringResource = when (this) {
    Failure.WrongKey -> Res.string.failure_wrong_key
    Failure.NoCredit -> Res.string.failure_no_credit
    Failure.RateLimited -> Res.string.failure_rate
    Failure.CantReach -> Res.string.failure_cant_reach
    Failure.NotFound -> Res.string.failure_not_found
    Failure.BadInput -> Res.string.failure_bad_input
    Failure.AppPasswordNeeded -> Res.string.failure_app_password
    Failure.WrongPassword -> Res.string.failure_wrong_password
    Failure.ImapOff -> Res.string.failure_imap_off
    Failure.TlsRequired -> Res.string.failure_tls
    Failure.TooLarge -> Res.string.failure_too_large
    Failure.Unreadable -> Res.string.failure_unreadable
    Failure.Unknown -> Res.string.failure_unknown
}
