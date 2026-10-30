package app.kiddo.app

import app.cash.sqldelight.db.SqlDriver
import app.kiddo.core.database.FileVault
import app.kiddo.core.database.IosFileVault
import app.kiddo.core.database.deleteIosDatabase
import app.kiddo.core.database.iosDriver
import app.kiddo.core.model.AppInfo
import app.kiddo.core.platform.CvReader
import app.kiddo.core.platform.CvRenderer
import app.kiddo.core.platform.IosCvReader
import app.kiddo.core.platform.IosCvRenderer
import app.kiddo.core.platform.IosNotifier
import app.kiddo.core.platform.Notifier
import app.kiddo.core.security.AppReset
import app.kiddo.core.security.AppleCrypto
import app.kiddo.core.security.AppleSecurity
import app.kiddo.core.security.IosVault
import app.kiddo.core.security.Vault
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import platform.Foundation.NSBundle
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.CoroutineContext

// What the Swift shell gives Kotlin besides the crypto: a way to start over with a fresh screen.
interface ShellHost {
    // Erase everything finished: drop the current screen and show the app from its start.
    fun restart()
}

private lateinit var shellHost: ShellHost

// Called once by the Swift shell before the first frame.
fun startKiddoIos(crypto: AppleCrypto, host: ShellHost) {
    AppleSecurity.install(crypto)
    shellHost = host
    start()
}

private fun start() {
    val version = NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String ?: ""
    startKiddo(iosPlatform(), AppInfo(version, SOURCE_URL, AUTHOR_URL, AUTHOR))
}

// What only iOS provides to the shared app.
private fun iosPlatform() = module {
    single<Vault> { IosVault(AppleSecurity.crypto) }
    single<SqlDriver> { iosDriver() }
    single<FileVault> { IosFileVault() }
    single<CoroutineContext> { Dispatchers.IO }
    single<HttpClientEngine> { Darwin.create() }
    single<CvReader> { IosCvReader(get()) }
    single<CvRenderer> { IosCvRenderer() }
    single<Notifier> { IosNotifier() }
    single<AppReset> { IosAppReset(get(), get(), get()) }
}

// Erase everything: the keys go first (every sealed value becomes unreadable at once), then the files and the
// database. iOS apps never quit themselves, so Kiddo starts again in place: a new graph and a new first screen.
private class IosAppReset(private val vault: Vault, private val files: FileVault, private val driver: SqlDriver) : AppReset {
    override fun eraseAll() {
        vault.erase()
        files.deleteAll()
        driver.close()
        deleteIosDatabase()
        dispatch_async(dispatch_get_main_queue()) {
            stopKoin()
            start()
            shellHost.restart()
        }
    }
}
