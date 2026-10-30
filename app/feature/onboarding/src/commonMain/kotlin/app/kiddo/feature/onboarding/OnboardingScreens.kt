package app.kiddo.feature.onboarding

import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.kiddo.core.database.SettingsStore
import app.kiddo.core.database.WorkspaceStore
import app.kiddo.core.designsystem.KiddoMotion
import app.kiddo.core.designsystem.component.LocalOverlay
import app.kiddo.core.navigation.Route
import app.kiddo.core.navigation.startOver
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.lock_needs_passcode
import app.kiddo.core.resources.lock_needs_screen_lock
import app.kiddo.core.platform.platformFeel
import app.kiddo.core.resources.lock_prompt
import app.kiddo.core.security.UnlockResult
import app.kiddo.core.security.rememberDeviceUnlock
import app.kiddo.feature.onboarding.ui.LockSetupScreen
import app.kiddo.feature.onboarding.ui.SplashScreen
import app.kiddo.feature.onboarding.ui.WelcomeScreen
import app.kiddo.feature.onboarding.ui.WorkspaceRoute
import app.kiddo.feature.onboarding.ui.WorkspaceViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

class StartViewModel(private val workspaces: WorkspaceStore, private val settings: SettingsStore) : ViewModel() {
    // The first open goes to Welcome; every open after goes straight to the chat.
    suspend fun next(): Route = if (workspaces.current() == null) Route.Welcome else Route.Home()

    fun turnOnLock(onDone: () -> Unit) {
        viewModelScope.launch {
            workspaces.current()?.let { ws -> settings.update(ws.id) { it.copy(lockEnabled = true) } }
            onDone()
        }
    }
}

val onboardingModule = module {
    viewModelOf(::StartViewModel)
    viewModelOf(::WorkspaceViewModel)
}

fun NavGraphBuilder.onboardingScreens(nav: NavController) {
    composable<Route.Splash> {
        val viewModel: StartViewModel = koinViewModel()
        LaunchedEffect(Unit) {
            val next = viewModel.next()
            delay(KiddoMotion.SPLASH_HOLD.toLong())
            nav.startOver(next)
        }
        SplashScreen()
    }
    composable<Route.Welcome> { WelcomeScreen(onStart = { nav.navigate(Route.Workspace) }) }
    composable<Route.Workspace> {
        WorkspaceRoute(koinViewModel(), onCreated = { nav.startOver(Route.LockSetup) })
    }
    composable<Route.LockSetup> {
        val viewModel: StartViewModel = koinViewModel()
        val overlay = LocalOverlay.current
        val home = { nav.startOver(Route.Home()) }
        val unlock = rememberDeviceUnlock(stringResource(Res.string.lock_prompt)) { result ->
            when (result) {
                UnlockResult.Unlocked -> viewModel.turnOnLock(home)
                UnlockResult.NoDeviceLock -> viewModel.viewModelScope.launch {
                    overlay.toast(getString(if (platformFeel.appleNames) Res.string.lock_needs_passcode else Res.string.lock_needs_screen_lock))
                }
                UnlockResult.Cancelled, UnlockResult.Failed -> Unit
            }
        }
        LockSetupScreen(onTurnOn = unlock, onSkip = home)
    }
}
