package app.kiddo.core.database

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.cash.sqldelight.db.SqlDriver
import java.io.File

fun androidDriver(context: Context): SqlDriver = AndroidSqliteDriver(KiddoDatabase.Schema, context, DATABASE)

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
