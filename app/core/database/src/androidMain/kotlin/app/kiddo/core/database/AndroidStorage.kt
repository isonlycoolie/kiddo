package app.kiddo.core.database

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.cash.sqldelight.db.SqlDriver
import app.kiddo.core.security.SecretStore
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File
import java.security.SecureRandom

// SQLCipher: the whole file is encrypted with a random key that lives sealed in the vault. Single values inside stay
// sealed by the hardware key as well, so a leaked key alone still opens nothing personal.
fun androidDriver(context: Context, secrets: SecretStore): SqlDriver {
    System.loadLibrary("sqlcipher")
    val key = databaseKey(secrets)
    encryptOldFile(context, key)
    return AndroidSqliteDriver(KiddoDatabase.Schema, context, DATABASE, SupportOpenHelperFactory(key.toByteArray()))
}

// 32 random bytes as hex, so the same text works for the open helper and for ATTACH ... KEY.
private fun databaseKey(secrets: SecretStore): String =
    secrets.get(KEY_REF) ?: ByteArray(KEY_BYTES).also(SecureRandom()::nextBytes).hex().also { secrets.put(KEY_REF, it) }

// Files made before SQLCipher are plain SQLite: copy them into an encrypted file once, then swap it in.
private fun encryptOldFile(context: Context, key: String) {
    val file = context.getDatabasePath(DATABASE)
    if (!file.exists() || file.readHeader() != PLAIN_HEADER) return
    val sealed = File(file.parentFile, "$DATABASE.sealing").apply { delete() }
    SQLiteDatabase.openOrCreateDatabase(file, ByteArray(0), null, null, null).use { db ->
        db.rawExecSQL("ATTACH DATABASE '${sealed.path}' AS sealed KEY '$key'")
        db.rawExecSQL("SELECT sqlcipher_export('sealed')")
        db.rawExecSQL("PRAGMA sealed.user_version = ${db.version}")
        db.rawExecSQL("DETACH DATABASE sealed")
    }
    context.deleteDatabase(DATABASE)
    sealed.renameTo(file)
}

private fun File.readHeader(): String {
    val head = ByteArray(PLAIN_HEADER.length)
    val read = inputStream().use { it.read(head) }
    return String(head, 0, read.coerceAtLeast(0), Charsets.US_ASCII)
}

private fun ByteArray.hex() = joinToString("") { "%02x".format(it) }

// Wipes the database file itself, used by Erase everything after the hardware keys are gone.
fun deleteAndroidDatabase(context: Context) {
    context.deleteDatabase(DATABASE)
}

// App-private files (no other app can read them; backups are off). Contents arrive already sealed.
class AndroidFileVault(context: Context) : FileVault {
    private val dir = File(context.filesDir, "vault").apply { mkdirs() }

    override fun write(name: String, bytes: ByteArray) {
        File(dir, name).writeBytes(bytes)
    }

    override fun read(name: String): ByteArray? = File(dir, name).takeIf { it.exists() }?.readBytes()

    override fun delete(name: String) {
        File(dir, name).delete()
    }

    override fun deleteAll() {
        dir.listFiles()?.forEach { it.delete() }
    }
}

private const val DATABASE = "kiddo.db"
private const val KEY_REF = "database.key"
private const val KEY_BYTES = 32
private const val PLAIN_HEADER = "SQLite format 3"
