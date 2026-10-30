package app.kiddo.core.security

import com.ionspin.kotlin.crypto.LibsodiumInitializer
import com.ionspin.kotlin.crypto.pwhash.PasswordHash
import com.ionspin.kotlin.crypto.pwhash.crypto_pwhash_argon2id_ALG_ARGON2ID13

// Backup keys from a passphrase: Argon2id through libsodium, the same on Android and iPhone so a backup opens on
// either. Slow and memory-hungry on purpose, which is what makes guessing passphrases expensive.
internal object Argon2 {
    const val SALT = 16

    @OptIn(ExperimentalUnsignedTypes::class)
    fun key(passphrase: String, salt: ByteArray): ByteArray {
        if (!LibsodiumInitializer.isInitialized()) LibsodiumInitializer.initializeWithCallback {}
        return PasswordHash.pwhash(
            KEY_BYTES,
            passphrase,
            salt.toUByteArray(),
            OPS.toULong(),
            MEMORY,
            crypto_pwhash_argon2id_ALG_ARGON2ID13,
        ).toByteArray()
    }

    private const val KEY_BYTES = 32
    private const val OPS = 3
    private const val MEMORY = 64 * 1024 * 1024
}
