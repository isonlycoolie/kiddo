package app.kiddo.feature.applications

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import app.kiddo.core.designsystem.component.LocalOverlay
import app.kiddo.core.designsystem.component.SheetGroup
import app.kiddo.core.designsystem.component.SheetRow
import app.kiddo.core.designsystem.component.message
import app.kiddo.core.model.AppStatus
import app.kiddo.core.model.NoticeKind
import app.kiddo.core.navigation.Route
import app.kiddo.core.platform.shortDate
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.app_delete
import app.kiddo.core.resources.follow_body
import app.kiddo.core.resources.follow_sent
import app.kiddo.core.resources.follow_subject
import app.kiddo.core.resources.followups_after
import app.kiddo.core.resources.followups_first
import app.kiddo.core.resources.followups_max
import app.kiddo.core.resources.generated.KiddoGlyph
import app.kiddo.core.resources.status_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val applicationsModule = module {
    singleOf(::InboxWatcher)
    factoryOf(::FollowUpSender)
    viewModelOf(::ApplicationsViewModel)
}

fun NavGraphBuilder.applicationsScreens(nav: NavController) {
    composable<Route.Applications> {
        val vm: ApplicationsViewModel = koinViewModel()
        val list by vm.list.collectAsStateWithLifecycle()
        ApplicationsScreen(list, { nav.popBackStack() }, { nav.navigate(Route.Application(it)) }, { nav.popBackStack() })
    }
    composable<Route.Application> { entry ->
        val id = entry.toRoute<Route.Application>().id
        val vm: ApplicationsViewModel = koinViewModel()
        val list by vm.list.collectAsStateWithLifecycle()
        val app = list?.firstOrNull { it.id == id }
        val overlay = LocalOverlay.current
        val statusTitle = stringResource(Res.string.status_title)
        ApplicationScreen(
            app,
            ApplicationActions(
                onBack = { nav.popBackStack() },
                onMore = {
                    overlay.sheet(statusTitle) {
                        SheetGroup {
                            AppStatus.entries.forEach { s ->
                                SheetRow(stringResource(statusLabel(s)), { vm.setStatus(id, s).also { overlay.close() } }, picked = s == app?.status)
                            }
                        }
                        SheetGroup {
                            SheetRow(stringResource(Res.string.app_delete), {
                                overlay.close()
                                vm.delete(id)
                                nav.popBackStack()
                            }, glyph = KiddoGlyph.Trash, danger = true)
                        }
                    }
                },
                onOpenChat = { chat -> nav.navigate(Route.Home(chatId = chat)) { popUpTo<Route.Home> { inclusive = true } } },
                onFollowUp = { nav.navigate(Route.FollowUp(id)) },
            ),
        )
    }
    composable<Route.Updates> {
        val vm: ApplicationsViewModel = koinViewModel()
        val notes by vm.notes.collectAsStateWithLifecycle()
        val checking by vm.checking.collectAsStateWithLifecycle()
        // Opening Updates counts as seeing them.
        LaunchedEffect(notes?.count { !it.read }) { if (notes?.any { !it.read } == true) vm.readAll() }
        UpdatesScreen(notes, checking, { nav.popBackStack() }, vm::checkNow, { n ->
            when {
                n.kind == NoticeKind.FollowUpDue && n.applicationId != null -> nav.navigate(Route.FollowUp(n.applicationId!!))
                n.kind == NoticeKind.CheckFailed -> nav.navigate(Route.Email)
                n.applicationId != null -> nav.navigate(Route.Application(n.applicationId!!))
            }
        })
    }
    composable<Route.FollowUp> { entry ->
        val id = entry.toRoute<Route.FollowUp>().applicationId
        val vm: ApplicationsViewModel = koinViewModel()
        val sender: FollowUpSender = koinInject()
        val list by vm.list.collectAsStateWithLifecycle()
        val prefs by vm.prefs.collectAsStateWithLifecycle()
        val app = list?.firstOrNull { it.id == id } ?: return@composable
        val overlay = LocalOverlay.current
        val scope = rememberCoroutineScope()
        val subject = stringResource(Res.string.follow_subject, app.role)
        var body by remember(app.id) { mutableStateOf("") }
        var sending by remember { mutableStateOf(false) }
        LaunchedEffect(app.id) {
            if (body.isEmpty()) body = getString(Res.string.follow_body, app.company.ifBlank { app.to }, app.role, shortDate(app.sentAt), sender.signOff())
        }
        FollowUpScreen(app.to, subject, body, { body = it }, app.followUps + 1, prefs.followUpMax, sending, { nav.popBackStack() }, {
            sending = true
            scope.launch {
                val failure = sender.send(app, subject, body)
                sending = false
                overlay.toast(getString(failure?.message() ?: Res.string.follow_sent))
                if (failure == null) nav.popBackStack()
            }
        })
    }
    composable<Route.FollowUps> {
        val vm: ApplicationsViewModel = koinViewModel()
        val prefs by vm.prefs.collectAsStateWithLifecycle()
        val overlay = LocalOverlay.current
        val daysTitle = stringResource(Res.string.followups_first)
        val maxTitle = stringResource(Res.string.followups_max)
        FollowUpsScreen(
            prefs,
            FollowUpsActions(
                onBack = { nav.popBackStack() },
                onChange = vm::updateSettings,
                onDays = {
                    overlay.sheet(daysTitle) {
                        SheetGroup {
                            listOf(3, 5, 7, 10, 14).forEach { d ->
                                SheetRow(stringResource(Res.string.followups_after, d), { vm.updateSettings { it.copy(followUpDays = d) }.also { overlay.close() } }, picked = d == prefs.followUpDays)
                            }
                        }
                    }
                },
                onMax = {
                    overlay.sheet(maxTitle) {
                        SheetGroup {
