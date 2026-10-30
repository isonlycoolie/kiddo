package app.kiddo.app

import app.kiddo.core.ai.AiClient
import app.kiddo.core.ai.HttpAiClient
import app.kiddo.core.database.ApplicationStore
import app.kiddo.core.database.ChatStore
import app.kiddo.core.database.CvStore
import app.kiddo.core.database.FileVault
import app.kiddo.core.database.MailStore
import app.kiddo.core.database.NoticeStore
import app.kiddo.core.database.ProviderStore
import app.kiddo.core.database.SettingsStore
import app.kiddo.core.database.Storage
import app.kiddo.core.database.WorkspaceStore
import app.kiddo.core.mail.MailTransport
import app.kiddo.core.mail.Mailer
import app.kiddo.core.mail.SendlyTransport
import app.kiddo.core.mail.SocketTransport
import app.kiddo.core.model.AppInfo
import app.kiddo.core.model.Clock
import app.kiddo.core.model.Ids
import app.kiddo.core.security.Vault
import app.kiddo.feature.applications.applicationsModule
import app.kiddo.feature.backup.backupModule
import app.kiddo.feature.chat.chatModule
import app.kiddo.feature.cvs.cvsModule
import app.kiddo.feature.email.emailModule
import app.kiddo.feature.history.historyModule
import app.kiddo.feature.models.modelsModule
import app.kiddo.feature.onboarding.onboardingModule
import app.kiddo.feature.profile.profileModule
import app.kiddo.feature.settings.settingsModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// Starts Kiddo once per process. `platform` provides what only the phone can: the Vault, the SQLite driver, the file
// vault, the HTTP engine, the pool for blocking work (CoroutineContext), CV reading and drawing, notifications and
// erase-everything.
fun startKiddo(platform: Module, info: AppInfo, extra: KoinAppDeclaration = {}): KoinApplication = startKoin {
    extra()
    modules(platform, appModule(info), shellModule, *featureModules.toTypedArray())
}

@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
private fun appModule(info: AppInfo) = module {
    single<Clock> { Clock { kotlin.time.Clock.System.now().toEpochMilliseconds() } }
    single<Ids> { Ids { Uuid.random().toString() } }
    single { Storage(get(), get<Vault>().fields, get<FileVault>(), get(), get(), get<CoroutineContext>()) }
    single { WorkspaceStore(get()) }
    single { SettingsStore(get()) }
    single { ProviderStore(get()) }
    single { ChatStore(get()) }
    single { CvStore(get()) }
    single { MailStore(get()) }
    single { ApplicationStore(get()) }
    single { NoticeStore(get()) }
    single {
        HttpClient(get<HttpClientEngine>()) {
            expectSuccess = false
            install(HttpTimeout) {
                connectTimeoutMillis = CONNECT_TIMEOUT
                socketTimeoutMillis = SOCKET_TIMEOUT
            }
        }
    }
    single<AiClient> { HttpAiClient(get()) }
    single<MailTransport> {
        val clock = get<Clock>()
        Mailer(SocketTransport(get<CoroutineContext>()) { clock.now() }, SendlyTransport(get()))
    }
    single { info }
    single { InboxCheck(get(), get()) }
}

private val shellModule = module { viewModelOf(::ShellViewModel) }

private val featureModules = listOf(
    onboardingModule,
    chatModule,
    historyModule,
    cvsModule,
    profileModule,
    settingsModule,
    modelsModule,
    emailModule,
    applicationsModule,
    backupModule,
)

const val SOURCE_URL = "https://github.com/isonlycoolie/kiddo"
const val AUTHOR_URL = "https://github.com/isonlycoolie"
const val AUTHOR = "isonlycoolie"

private const val CONNECT_TIMEOUT = 10_000L
private const val SOCKET_TIMEOUT = 90_000L
