package app.kiddo.core.security

import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// Android Keystore: AES-256-GCM keys that never leave the secure hardware (StrongBox when the phone has one, the TEE
// otherwise). Sealed secrets live in app-private preferences; backups are off for the whole app.
class KeystoreVault(private val context: Context) : Vault {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val secretKey = KeyHandle(SECRET_ALIAS)
    private val fieldKey = KeyHandle(FIELD_ALIAS)

    override val secrets: SecretStore = object : SecretStore {
        override fun get(ref: String): String? = prefs.getString(ref, null)?.let { secretKey.open(it) }

        override fun put(ref: String, value: String) {
            prefs.edit().putString(ref, secretKey.seal(value)).apply()
        }

        override fun remove(ref: String) {
            prefs.edit().remove(ref).apply()
        }
    }

    override val fields: FieldCipher = object : FieldCipher {
        override fun seal(plain: String): String = fieldKey.seal(plain)

        override fun open(sealed: String): String? = fieldKey.open(sealed)

        override fun sealBytes(plain: ByteArray): ByteArray = fieldKey.sealBytes(plain)

        override fun openBytes(sealed: ByteArray): ByteArray? = fieldKey.openBytes(sealed)
    }

    override fun hardware(): HardwareInfo {
        val strongBox = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        return HardwareInfo(strongBox = strongBox, deviceLock = keyguard?.isDeviceSecure == true)
    }

    override fun erase() {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        listOf(SECRET_ALIAS, FIELD_ALIAS).forEach { if (store.containsAlias(it)) store.deleteEntry(it) }
        prefs.edit().clear().commit()
    }

    private inner class KeyHandle(private val alias: String) {
        fun seal(plain: String): String =
            Base64.encodeToString(sealBytes(plain.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)

        fun open(sealed: String): String? = runCatching { Base64.decode(sealed, Base64.NO_WRAP) }.getOrNull()
            ?.let(::openBytes)
            ?.toString(Charsets.UTF_8)

        fun sealBytes(plain: ByteArray): ByteArray {
            val cipher = Cipher.getInstance(TRANSFORM)
            cipher.init(Cipher.ENCRYPT_MODE, key())
            return cipher.iv + cipher.doFinal(plain)
        }

        fun openBytes(raw: ByteArray): ByteArray? = runCatching {
            val cipher = Cipher.getInstance(TRANSFORM)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, raw, 0, IV_BYTES))
            cipher.doFinal(raw, IV_BYTES, raw.size - IV_BYTES)
        }.getOrNull()

        private fun key(): SecretKey {
            val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
            (store.getKey(alias, null) as? SecretKey)?.let { return it }
            return runCatching { generate(strongBox = true) }.getOrElse { generate(strongBox = false) }
        }

        private fun generate(strongBox: Boolean): SecretKey {
            val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_BITS)
            if (strongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) spec.setIsStrongBoxBacked(true)
            // Without StrongBox this throws, and key() falls back to the TEE.
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
            generator.init(spec.build())
            return generator.generateKey()
        }
    }

