package app.kiddo.core.security

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

// File layout: magic, version, salt, nonce, sealed data. The magic lets Kiddo say "not a Kiddo backup" plainly.
actual object PassphraseBox {
    actual fun seal(plain: ByteArray, passphrase: String): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT).also(random::nextBytes)
        val nonce = ByteArray(NONCE).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, key(passphrase, salt, VERSION), GCMParameterSpec(TAG_BITS, nonce))
        val sealed = cipher.doFinal(plain)
        return ByteBuffer.allocate(MAGIC.size + 1 + SALT + NONCE + sealed.size)
            .put(MAGIC).put(VERSION).put(salt).put(nonce).put(sealed).array()
    }

    actual fun open(sealed: ByteArray, passphrase: String): ByteArray? = runCatching {
        val buffer = ByteBuffer.wrap(sealed)
        val magic = ByteArray(MAGIC.size).also { buffer.get(it) }
        val version = buffer.get()
        if (!magic.contentEquals(MAGIC) || version !in OLD_VERSION..VERSION) return null
        val salt = ByteArray(SALT).also { buffer.get(it) }
        val nonce = ByteArray(NONCE).also { buffer.get(it) }
        val data = ByteArray(buffer.remaining()).also { buffer.get(it) }
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, key(passphrase, salt, version), GCMParameterSpec(TAG_BITS, nonce))
        cipher.doFinal(data)
    }.getOrNull()

    private fun key(passphrase: String, salt: ByteArray, version: Byte): SecretKeySpec {
        if (version == VERSION) return SecretKeySpec(Argon2.key(passphrase, salt), "AES")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, ROUNDS, KEY_BITS)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(bytes, "AES")
    }

    private val MAGIC = "KIDDO".toByteArray()
    private const val VERSION: Byte = 2
    private const val OLD_VERSION: Byte = 1
    private const val SALT = Argon2.SALT
    private const val NONCE = 12
    private const val TAG_BITS = 128
    private const val KEY_BITS = 256
    private const val ROUNDS = 310_000
    private const val TRANSFORM = "AES/GCM/NoPadding"
}
