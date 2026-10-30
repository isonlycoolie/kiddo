package app.kiddo.core.security

import app.kiddo.core.model.toByteArray
import app.kiddo.core.model.toNSData
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSUserDefaults
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

// The iOS Vault: values sealed with device keys that a Secure Enclave key wraps (see AppleCrypto); secrets are then
// kept in the Keychain. The Keychain outlives deleting the app, so a fresh install forgets everything first, as
// Android does.
@OptIn(ExperimentalEncodingApi::class)
class IosVault(private val crypto: AppleCrypto) : Vault {
    private val keychain = Keychain(SERVICE)

    init {
        val defaults = NSUserDefaults.standardUserDefaults
        if (!defaults.boolForKey(INSTALLED)) {
            crypto.eraseDeviceKeys()
            keychain.removeAll()
            defaults.setBool(true, forKey = INSTALLED)
        }
    }

    override val secrets: SecretStore = object : SecretStore {
        override fun get(ref: String): String? =
            keychain.get(ref)?.let { crypto.openWithDeviceKey(SECRET_KEY, it) }?.toByteArray()?.decodeToString()

        override fun put(ref: String, value: String) {
            crypto.sealWithDeviceKey(SECRET_KEY, value.encodeToByteArray().toNSData())?.let { keychain.put(ref, it) }
        }

        override fun remove(ref: String) {
            keychain.remove(ref)
        }
    }

    override val fields: FieldCipher = object : FieldCipher {
        override fun seal(plain: String): String = Base64.Default.encode(sealBytes(plain.encodeToByteArray()))

        override fun open(sealed: String): String? =
            runCatching { Base64.Default.decode(sealed) }.getOrNull()?.let(::openBytes)?.decodeToString()

        override fun sealBytes(plain: ByteArray): ByteArray =
            crypto.sealWithDeviceKey(FIELD_KEY, plain.toNSData())?.toByteArray() ?: error("Device key unavailable")

        override fun openBytes(sealed: ByteArray): ByteArray? = crypto.openWithDeviceKey(FIELD_KEY, sealed.toNSData())?.toByteArray()
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun hardware(): HardwareInfo = HardwareInfo(
        strongBox = crypto.hasSecureEnclave(),
        deviceLock = LAContext().canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, error = null),
    )

    override fun erase() {
        crypto.eraseDeviceKeys()
        keychain.removeAll()
    }

    private companion object {
        const val SERVICE = "app.kiddo.secrets"
        const val INSTALLED = "app.kiddo.installed"
        const val SECRET_KEY = "secret"
        const val FIELD_KEY = "field"
    }
}
