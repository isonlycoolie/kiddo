package app.kiddo.core.model

// The result type every adapter and repository returns: a value, or a reason the UI can explain in plain words.
sealed interface Outcome<out T> {
    data class Done<T>(val value: T) : Outcome<T>

    data class Failed(val reason: Failure) : Outcome<Nothing>
}

enum class Failure {
    WrongKey,
    NoCredit,
    RateLimited,
    CantReach,
    NotFound,
    BadInput,
    AppPasswordNeeded,
    WrongPassword,
    ImapOff,
    TlsRequired,
    TooLarge,
    Unreadable,
    Unknown,
}

inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Done -> Outcome.Done(transform(value))
    is Outcome.Failed -> this
}

fun interface Clock {
    fun now(): Long
}

fun interface Ids {
    fun next(): String
}
