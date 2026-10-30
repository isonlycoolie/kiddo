package app.kiddo.core.security

import androidx.compose.runtime.Composable

// API keys, passwords and tokens. Values are sealed by a hardware-held key and never written anywhere in plain text.
interface SecretStore {
    fun get(ref: String): String?

    fun put(ref: String, value: String)

    fun remove(ref: String)
}

// Encrypts single database values (names, emails, chat text, CV text) with a second hardware-held key, so the
// database file holds nothing readable even outside Android's own storage encryption.
interface FieldCipher {
    fun seal(plain: String): String

    // Null when the value can't be opened (the hardware key was reset or erased).
    fun open(sealed: String): String?

    // The same for files (CVs): nonce first, then the sealed bytes.
    fun sealBytes(plain: ByteArray): ByteArray

    fun openBytes(sealed: ByteArray): ByteArray?
}

// What the phone's secure hardware offers, shown on the Security screen.
data class HardwareInfo(val strongBox: Boolean, val deviceLock: Boolean)

interface Vault {
    val secrets: SecretStore
    val fields: FieldCipher

    fun hardware(): HardwareInfo

    // Deletes the hardware keys first, which makes every sealed value unreadable at once, then the stored values.
    fun erase()
}

enum class UnlockResult { Unlocked, Cancelled, NoDeviceLock, Failed }

// Asks the phone to confirm it's the owner: fingerprint, face or the phone PIN. Returns the function that shows it.
@Composable
expect fun rememberDeviceUnlock(title: String, onResult: (UnlockResult) -> Unit): () -> Unit

// Erase everything: hardware keys first, then the database and files, then the app starts fresh. The app provides it.
fun interface AppReset {
    fun eraseAll()
}

// Seals a backup with a passphrase only the user knows: a key stretched from the passphrase (PBKDF2, a fresh salt each
// time) and AES-256-GCM. Open returns null for a wrong passphrase or a damaged file.
expect object PassphraseBox {
    fun seal(plain: ByteArray, passphrase: String): ByteArray

    fun open(sealed: ByteArray, passphrase: String): ByteArray?
}
