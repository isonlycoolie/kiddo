package app.kiddo.feature.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import app.kiddo.core.database.ChatStore
import app.kiddo.core.designsystem.component.BackPress
import app.kiddo.core.designsystem.component.LocalOverlay
import app.kiddo.core.designsystem.component.Overlay
import app.kiddo.core.designsystem.component.accountSheet
import app.kiddo.core.designsystem.component.message
import app.kiddo.core.model.MailAccount
import app.kiddo.core.navigation.Route
import app.kiddo.core.platform.rememberNotificationAsk
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.agent_no_mail
import app.kiddo.core.resources.blocked_paste_text
import app.kiddo.core.resources.chat_models
import app.kiddo.core.resources.cvcard_saved
import app.kiddo.core.resources.send_title
import app.kiddo.core.resources.starter_cv
import app.kiddo.core.web.PageReader
import app.kiddo.feature.chat.agent.CvBuilder
import app.kiddo.feature.chat.agent.DraftCard
import app.kiddo.feature.chat.agent.JobAgent
import app.kiddo.feature.chat.agent.Outbox
import app.kiddo.feature.chat.agent.parseModelJson
import app.kiddo.feature.chat.ui.BrowserScreen
import app.kiddo.feature.chat.ui.CardActions
import app.kiddo.feature.chat.ui.DraftScreen
import app.kiddo.feature.chat.ui.HomeActions
import app.kiddo.feature.chat.ui.HomeScreen
import app.kiddo.feature.chat.ui.MenuActions
import app.kiddo.feature.chat.ui.SendSheet
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val chatModule = module {
    factoryOf(::PromptBuilder)
    factoryOf(::JobAgent)
    factoryOf(::CvBuilder)
    factoryOf(::Outbox)
    single { PageReader(get()) }
    viewModelOf(::ChatViewModel)
}

private const val BROWSER_URL = "browser_url"
private const val BROWSER_TEXT = "browser_text"

fun NavGraphBuilder.chatScreens(nav: NavController) {
    composable<Route.Home> { entry -> HomeRoute(nav, entry) }
    composable<Route.Draft> { entry ->
        val id = entry.toRoute<Route.Draft>().messageId
        val chats: ChatStore = koinInject()
        val outbox: Outbox = koinInject()
        val scope = rememberCoroutineScope()
        val draft by produceState<DraftCard?>(null, id) { value = chats.message(id)?.payload?.let { parseModelJson<DraftCard>(it) } }
        draft?.let { d ->
            DraftScreen(d, { nav.popBackStack() }, { to, subject, body ->
                scope.launch {
                    outbox.saveDraft(id, to, subject, body)
                    nav.popBackStack()
                }
            })
        }
    }
    composable<Route.Browser> { entry ->
        val url = entry.toRoute<Route.Browser>().url
        BrowserScreen(url, { nav.popBackStack() }, { text ->
            nav.previousBackStackEntry?.savedStateHandle?.apply {
                set(BROWSER_URL, url)
                set(BROWSER_TEXT, text)
            }
            nav.popBackStack()
        })
    }
}

@Composable
private fun HomeRoute(nav: NavController, entry: NavBackStackEntry) {
    val route = entry.toRoute<Route.Home>()
    val viewModel: ChatViewModel = koinViewModel()
    val cvTitle = stringResource(Res.string.starter_cv)
    LaunchedEffect(route.chatId) { viewModel.open(route.chatId) }
    LaunchedEffect(route.improveCv) { route.improveCv?.let { viewModel.improveCv(it, cvTitle) } }
    // The Kiddo browser hands back what the user saw; read it once.
    val pageText by entry.savedStateHandle.getStateFlow<String?>(BROWSER_TEXT, null).collectAsStateWithLifecycle()
    LaunchedEffect(pageText) {
        val text = pageText ?: return@LaunchedEffect
        viewModel.fromBrowser(entry.savedStateHandle.get<String>(BROWSER_URL).orEmpty(), text)
        entry.savedStateHandle.remove<String>(BROWSER_TEXT)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var draft by rememberSaveable { mutableStateOf(route.draft.orEmpty()) }
    var menu by rememberSaveable { mutableStateOf(false) }
    BackPress(enabled = menu) { menu = false }
    val overlay = LocalOverlay.current
    val uri = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val askNotifications = rememberNotificationAsk()
    val modelTitle = stringResource(Res.string.chat_models)
    val sendTitle = stringResource(Res.string.send_title)
    val pasteText = stringResource(Res.string.blocked_paste_text)
    val toast: (StringResource) -> Unit = { res -> scope.launch { overlay.toast(getString(res)) } }
    HomeScreen(
        state,
        draft,
        menu,
        HomeActions(
            onMenu = { menu = it },
            onModel = { overlay.accountSheet(modelTitle, state.accounts, state.active?.id, viewModel::pickModel) },
            onProfile = { nav.navigate(Route.Profile) },
            onText = { draft = it },
            onSend = {
                viewModel.send(draft)
                draft = ""
            },
            onStop = viewModel::stop,
            onRetry = viewModel::retry,
            onSetupAi = { nav.navigate(Route.AiAdd()) },
            onSetupMail = { nav.navigate(Route.Email) },
            onSetupCv = { nav.navigate(Route.Cvs) },
            onImproveCv = { viewModel.improveCv(null, cvTitle) },
            menu = MenuActions(
                onNewChat = { viewModel.open(null) },
                onApplications = { nav.navigate(Route.Applications) },
                onCvs = { nav.navigate(Route.Cvs) },
                onUpdates = { nav.navigate(Route.Updates) },
                onHistory = { nav.navigate(Route.History) },
                onChat = viewModel::open,
                onProfile = { nav.navigate(Route.Profile) },
                onSettings = { nav.navigate(Route.Settings) },
            ),
            cards = CardActions(
                onEditDraft = { nav.navigate(Route.Draft(it)) },
                onReviewDraft = { id, card ->
                    val mail = state.mail
                    if (mail == null) {
                        toast(Res.string.agent_no_mail)
                    } else {
                        overlay.sendSheet(sendTitle, card, mail, state.followUpDays) { done ->
                            viewModel.sendDraft(id) { failure ->
                                done()
                                overlay.close()
                                if (failure != null) toast(failure.message()) else askNotifications()
                            }
                        }
                    }
                },
                onMarkApplied = viewModel::markApplied,
                onOpenSite = { uri.openUri(it) },
                onViewApplication = { nav.navigate(Route.Application(it)) },
                onOpenBrowser = { nav.navigate(Route.Browser(it)) },
                onPaste = { draft = pasteText },
                onSaveCv = { id -> viewModel.saveCv(id) { saved -> if (saved != null) toast(Res.string.cvcard_saved) } },
                onOpenCv = { nav.navigate(Route.Cv(it)) },
                renderCv = viewModel::renderCv,
            ),
        ),
    )
}

private fun Overlay.sendSheet(title: String, draft: DraftCard, mail: MailAccount, days: Int, onSend: (onDone: () -> Unit) -> Unit) =
    sheet(title) { SendSheet(draft, mail, days, onSend) }
