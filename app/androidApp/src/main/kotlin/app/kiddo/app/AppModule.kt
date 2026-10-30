package app.kiddo.app

import android.content.Context
import android.content.Intent
import app.cash.sqldelight.db.SqlDriver
import app.kiddo.core.database.AndroidFileVault
import app.kiddo.core.database.FileVault
import app.kiddo.core.database.androidDriver
import app.kiddo.core.database.deleteAndroidDatabase
import app.kiddo.core.platform.AndroidCvReader
import app.kiddo.core.platform.AndroidCvRenderer
import app.kiddo.core.platform.AndroidNotifier
import app.kiddo.core.platform.CvReader
import app.kiddo.core.platform.CvRenderer
import app.kiddo.core.platform.Notifier
import app.kiddo.core.security.AppReset
import app.kiddo.core.security.KeystoreVault
import app.kiddo.core.security.Vault
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext
import kotlin.system.exitProcess

// What only Android provides to the shared app: the Keystore vault, storage, the HTTP engine, CVs, notifications.
val androidPlatform = module {
    single<Vault> { KeystoreVault(androidContext()) }
    single<SqlDriver> { androidDriver(androidContext(), get<Vault>().secrets) }
    single<FileVault> { AndroidFileVault(androidContext()) }
    single<CoroutineContext> { Dispatchers.IO }
    single<HttpClientEngine> { OkHttp.create() }
    single<CvReader> { AndroidCvReader(androidContext()) }
    single<CvRenderer> { AndroidCvRenderer() }
    single<Notifier> { AndroidNotifier(androidContext()) }
    single<AppReset> { AndroidAppReset(androidContext(), get()) }
}

// Erase everything: the hardware keys go first (every sealed value becomes unreadable at once), then the files and
// the database, then the app restarts clean.
private class AndroidAppReset(private val context: Context, private val vault: Vault) : AppReset {
    override fun eraseAll() {
        vault.erase()
        AndroidFileVault(context).deleteAll()
        deleteAndroidDatabase(context)
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { launch ->
            context.startActivity(Intent.makeRestartActivityTask(launch.component))
        }
        exitProcess(0)
    }
}
