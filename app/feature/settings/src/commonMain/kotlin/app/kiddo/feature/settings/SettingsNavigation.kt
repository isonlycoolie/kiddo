package app.kiddo.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.kiddo.core.designsystem.component.LocalOverlay
import app.kiddo.core.designsystem.component.Overlay
import app.kiddo.core.designsystem.component.Primary
import app.kiddo.core.designsystem.component.SheetGroup
import app.kiddo.core.designsystem.component.SheetRow
import app.kiddo.core.designsystem.component.SheetText
import app.kiddo.core.model.Appearance
import app.kiddo.core.model.AppInfo
import app.kiddo.core.model.LockAfter
import app.kiddo.core.navigation.Route
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.erase_body
import app.kiddo.core.resources.erase_title
import app.kiddo.core.resources.lock_prompt
import app.kiddo.core.resources.security_lock_after
import app.kiddo.core.resources.settings_appearance
import app.kiddo.core.resources.settings_erase
import app.kiddo.core.security.UnlockResult
import app.kiddo.core.security.rememberDeviceUnlock
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val settingsModule = module { viewModelOf(::SettingsViewModel) }

fun NavGraphBuilder.settingsScreens(nav: NavController) {
    composable<Route.Settings> {
        val viewModel: SettingsViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val overlay = LocalOverlay.current
        val appearanceTitle = stringResource(Res.string.settings_appearance)
        val eraseTitle = stringResource(Res.string.erase_title)
        SettingsScreen(
            state,
            SettingsActions(
                onBack = { nav.popBackStack() },
                onEmail = { nav.navigate(Route.Email) },
                onAi = { nav.navigate(Route.Ai) },
                onFollowUps = { nav.navigate(Route.FollowUps) },
                onSecurity = { nav.navigate(Route.Security) },
                onBackup = { nav.navigate(Route.Backup) },
                onAppearance = { overlay.appearanceSheet(appearanceTitle, state.settings.appearance) { a -> viewModel.update { it.copy(appearance = a) } } },
                onAbout = { nav.navigate(Route.About) },
                onErase = { overlay.eraseSheet(eraseTitle, viewModel::eraseEverything) },
            ),
        )
    }
    composable<Route.Security> {
        val viewModel: SettingsViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val overlay = LocalOverlay.current
        val afterTitle = stringResource(Res.string.security_lock_after)
        val eraseTitle = stringResource(Res.string.erase_title)
        // Turning the lock on or off asks the phone first, so nobody else can switch it off.
        var wanted by remember { mutableStateOf(false) }
        val confirm = rememberDeviceUnlock(stringResource(Res.string.lock_prompt)) { result ->
            if (result == UnlockResult.Unlocked) viewModel.update { it.copy(lockEnabled = wanted) }
        }
        SecurityScreen(
            state,
            SecurityActions(
                onBack = { nav.popBackStack() },
                onLock = { on ->
                    wanted = on
                    confirm()
                },
                onLockAfter = { overlay.lockAfterSheet(afterTitle, state.settings.lockAfter) { a -> viewModel.update { it.copy(lockAfter = a) } } },
                onHide = { on -> viewModel.update { it.copy(hideInSwitcher = on) } },
                onErase = { overlay.eraseSheet(eraseTitle, viewModel::eraseEverything) },
            ),
        )
    }
    composable<Route.About> {
        val info: AppInfo = koinInject()
        val uri = LocalUriHandler.current
        AboutScreen(info, onBack = { nav.popBackStack() }, onOpen = { uri.openUri(it) })
    }
}

private fun Overlay.appearanceSheet(title: String, current: Appearance, onPick: (Appearance) -> Unit) = sheet(title) {
    SheetGroup {
        Appearance.entries.forEach { a ->
            SheetRow(stringResource(appearanceLabel(a)), { onPick(a).also { close() } }, picked = a == current)
        }
    }
}

private fun Overlay.lockAfterSheet(title: String, current: LockAfter, onPick: (LockAfter) -> Unit) = sheet(title) {
    SheetGroup {
        LockAfter.entries.forEach { a ->
            SheetRow(stringResource(lockAfterLabel(a)), { onPick(a).also { close() } }, picked = a == current)
        }
    }
}

private fun Overlay.eraseSheet(title: String, onErase: () -> Unit) = sheet(title) {
    SheetText(stringResource(Res.string.erase_body))
    EraseButton(onErase)
}

@Composable
private fun EraseButton(onErase: () -> Unit) {
    Primary(stringResource(Res.string.settings_erase), onErase, danger = true)
}
