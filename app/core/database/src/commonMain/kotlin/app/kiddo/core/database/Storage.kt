package app.kiddo.core.database

import app.cash.sqldelight.db.SqlDriver
import app.kiddo.core.model.Clock
import app.kiddo.core.model.Ids
import app.kiddo.core.security.FieldCipher
import kotlin.coroutines.CoroutineContext

// Files the app keeps privately (CVs), already sealed by the caller.
interface FileVault {
    fun write(name: String, bytes: ByteArray)

    fun read(name: String): ByteArray?

    fun delete(name: String)

    fun deleteAll()
}

// Everything a store needs; built once by the app and shared.
class Storage(
    driver: SqlDriver,
    val cipher: FieldCipher,
    val files: FileVault,
    val clock: Clock,
    val ids: Ids,
    val io: CoroutineContext,
) {
    val db = KiddoDatabase(driver)
    internal val q get() = db.kiddoQueries
}
