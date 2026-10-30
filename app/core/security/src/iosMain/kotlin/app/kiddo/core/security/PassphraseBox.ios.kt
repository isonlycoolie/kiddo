package app.kiddo.core.security

import app.kiddo.core.model.toByteArray
import app.kiddo.core.model.toNSData
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecSuccess
import platform.Security.kSecRandomDefault

// The same file as on Android (magic, version, salt, nonce, sealed data), so a backup opens on either phone.
// Version 2 keys come from Argon2id; version 1 (PBKDF2, older builds) still opens.
actual object PassphraseBox {
    actual fun seal(plain: ByteArray, passphrase: String): ByteArray {
        val salt = random(SALT)
        val nonce = random(NONCE)
        val crypto = AppleSecurity.crypto
        val key = Argon2.key(passphrase, salt).toNSData()
        val sealed = crypto.sealGcm(key, nonce.toNSData(), plain.toNSData())?.toByteArray() ?: error("Sealing failed")
        return MAGIC + byteArrayOf(VERSION) + salt + nonce + sealed
    }

    actual fun open(sealed: ByteArray, passphrase: String): ByteArray? {
        val head = MAGIC.size + 1
        if (sealed.size < head + SALT + NONCE + TAG) return null
        val version = sealed[MAGIC.size]
        if (!sealed.copyOfRange(0, MAGIC.size).contentEquals(MAGIC) || version !in OLD_VERSION..VERSION) return null
        val salt = sealed.copyOfRange(head, head + SALT)
        val nonce = sealed.copyOfRange(head + SALT, head + SALT + NONCE)
        val data = sealed.copyOfRange(head + SALT + NONCE, sealed.size)
        val crypto = AppleSecurity.crypto
        val key = if (version == VERSION) {
            Argon2.key(passphrase, salt).toNSData()
        } else {
            crypto.deriveKey(passphrase, salt.toNSData(), ROUNDS) ?: return null
        }
        return crypto.openGcm(key, nonce.toNSData(), data.toNSData())?.toByteArray()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun random(size: Int): ByteArray = ByteArray(size).also { out ->
        val status = out.usePinned { SecRandomCopyBytes(kSecRandomDefault, size.convert(), it.addressOf(0)) }
        check(status == errSecSuccess) { "No secure random" }
    }

    private val MAGIC = "KIDDO".encodeToByteArray()
    private const val VERSION: Byte = 2
    private const val OLD_VERSION: Byte = 1
    private const val SALT = Argon2.SALT
    private const val NONCE = 12
    private const val TAG = 16
    private const val ROUNDS = 310_000
}
