package app.kiddo.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import app.kiddo.core.database.SettingsStore
import app.kiddo.core.database.WorkspaceStore
import app.kiddo.core.designsystem.KiddoTheme
import app.kiddo.core.designsystem.component.OverlayHost
import app.kiddo.core.model.AppSettings
import app.kiddo.core.model.Appearance
import app.kiddo.core.model.Clock
import app.kiddo.core.navigation.Route
import app.kiddo.core.navigation.ScreenTransitions
import app.kiddo.core.platform.HideFromSwitcher
import app.kiddo.core.platform.platformFeel
import app.kiddo.core.platform.rememberReducedMotion
import app.kiddo.feature.applications.applicationsScreens
import app.kiddo.feature.backup.backupScreens
import app.kiddo.feature.chat.chatScreens
import app.kiddo.feature.cvs.cvsScreens
import app.kiddo.feature.email.emailScreens
import app.kiddo.feature.history.historyScreens
import app.kiddo.feature.lock.LockScreen
import app.kiddo.feature.models.modelsScreens
import app.kiddo.feature.onboarding.onboardingScreens
import app.kiddo.feature.profile.profileScreens
import app.kiddo.feature.settings.settingsScreens
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

// App-wide state: the settings that shape the shell (theme, lock, app switcher) and whether Kiddo is locked now.
@OptIn(ExperimentalCoroutinesApi::class)
class ShellViewModel(
    workspaces: WorkspaceStore,
    settings: SettingsStore,
    private val clock: Clock,
    private val inbox: InboxCheck,
) : ViewModel() {
    // Kiddo looks for replies each time it comes to the front; on iOS this is the main way, on Android a bonus.
    fun checkInbox() {
        viewModelScope.launch { inbox.run(background = false) }
    }

    val settings: StateFlow<AppSettings?> = workspaces.workspace
        .flatMapLatest { ws -> ws?.let { settings.settings(it.id) } ?: flowOf(AppSettings()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _locked = MutableStateFlow<Boolean?>(null)
    val locked: StateFlow<Boolean?> = _locked
    private var leftAt: Long? = null

    // Locked on every cold start when the lock is on.
    fun settle(s: AppSettings) {
        if (_locked.value == null) _locked.value = s.lockEnabled
        if (!s.lockEnabled) _locked.value = false
    }

    fun left() {
        leftAt = clock.now()
    }

    fun returned(s: AppSettings) {
        val away = leftAt?.let { clock.now() - it } ?: return
        leftAt = null
        if (s.lockEnabled && away >= s.lockAfter.millis) _locked.value = true
    }

    fun unlocked() {
        _locked.value = false
    }
}

