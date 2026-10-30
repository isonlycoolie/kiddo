package app.kiddo.core.security

import platform.Foundation.NSData

// The crypto only Swift can reach (CryptoKit), implemented by the iOS shell and handed over at start.
interface AppleCrypto {
    // AES-256-GCM with the device key called `name`: nonce, then ciphertext, then tag. The key is created on first use
    // and wrapped by a Secure Enclave key; null when the key is gone or the data was changed.
    fun sealWithDeviceKey(name: String, plain: NSData): NSData?

    fun openWithDeviceKey(name: String, sealed: NSData): NSData?

    // Deletes the Enclave keys and the wrapped device keys: every value they sealed is unreadable from then on.
    fun eraseDeviceKeys()

    fun hasSecureEnclave(): Boolean

    // PBKDF2-HMAC-SHA256 to a 32-byte key.
    fun deriveKey(passphrase: String, salt: NSData, rounds: Int): NSData?

    // AES-256-GCM with a given key and nonce; the sealed form is ciphertext then the 16-byte tag.
    fun sealGcm(key: NSData, nonce: NSData, plain: NSData): NSData?

    fun openGcm(key: NSData, nonce: NSData, sealed: NSData): NSData?
}

// Set once by the app before anything opens a vault or a backup.
object AppleSecurity {
    lateinit var crypto: AppleCrypto
        private set

    fun install(crypto: AppleCrypto) {
        this.crypto = crypto
    }
}
