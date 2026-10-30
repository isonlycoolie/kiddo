package app.kiddo.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.kiddo.core.model.toByteArray
import app.kiddo.core.model.toNSData
import co.touchlab.sqliter.DatabaseConfiguration
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSData
import platform.Foundation.NSDataWritingAtomic
import platform.Foundation.NSDataWritingFileProtectionCompleteUntilFirstUserAuthentication
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile

// Kiddo's folders live in Application Support, are never copied to iCloud or a computer backup (nothing leaves the
// phone except through Kiddo's own encrypted backup), and stay readable after the first unlock so the background
// check can run.
@OptIn(ExperimentalForeignApi::class)
internal fun kiddoFolder(name: String): String {
    val files = NSFileManager.defaultManager
    val base = files.URLsForDirectory(NSApplicationSupportDirectory, NSUserDomainMask).firstOrNull() as? NSURL
    val url = base?.URLByAppendingPathComponent(name, isDirectory = true) ?: error("No Application Support folder")
    files.createDirectoryAtURL(url, withIntermediateDirectories = true, attributes = null, error = null)
    url.setResourceValue(true, forKey = NSURLIsExcludedFromBackupKey, error = null)
    return url.path ?: error("No folder path")
}

fun iosDriver(): SqlDriver = NativeSqliteDriver(KiddoDatabase.Schema, DATABASE, onConfiguration = { config ->
    config.copy(extendedConfig = DatabaseConfiguration.Extended(basePath = kiddoFolder(DATABASES)))
})

// Wipes the database files, used by Erase everything after the keys are gone and the driver is closed.
@OptIn(ExperimentalForeignApi::class)
fun deleteIosDatabase() {
    val dir = kiddoFolder(DATABASES)
    listOf("", "-wal", "-shm", "-journal").forEach { NSFileManager.defaultManager.removeItemAtPath("$dir/$DATABASE$it", error = null) }
}

// App-private files. Contents arrive already sealed.
@OptIn(ExperimentalForeignApi::class)
class IosFileVault : FileVault {
    private val dir = kiddoFolder("vault")

    override fun write(name: String, bytes: ByteArray) {
        bytes.toNSData().writeToFile("$dir/$name", options = NSDataWritingAtomic or NSDataWritingFileProtectionCompleteUntilFirstUserAuthentication, error = null)
    }

    override fun read(name: String): ByteArray? = NSData.dataWithContentsOfFile("$dir/$name")?.toByteArray()

    override fun delete(name: String) {
        NSFileManager.defaultManager.removeItemAtPath("$dir/$name", error = null)
    }

    override fun deleteAll() {
        val files = NSFileManager.defaultManager
        files.contentsOfDirectoryAtPath(dir, error = null)?.forEach { files.removeItemAtPath("$dir/$it", error = null) }
    }
}

private const val DATABASE = "kiddo.db"
private const val DATABASES = "databases"
