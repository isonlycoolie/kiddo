package app.kiddo.feature.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.kiddo.core.database.CvStore
import app.kiddo.core.database.WorkspaceStore
import app.kiddo.core.designsystem.Kiddo
import app.kiddo.core.designsystem.component.Avatar
import app.kiddo.core.designsystem.component.Field
import app.kiddo.core.designsystem.component.FieldKind
import app.kiddo.core.designsystem.component.Group
import app.kiddo.core.designsystem.component.Hairline
import app.kiddo.core.designsystem.component.Info
import app.kiddo.core.designsystem.component.KiddoIcon
import app.kiddo.core.designsystem.component.KiddoScreen
import app.kiddo.core.designsystem.component.ListRow
import app.kiddo.core.designsystem.component.LocalOverlay
import app.kiddo.core.designsystem.component.NavBar
import app.kiddo.core.designsystem.component.SheetGroup
import app.kiddo.core.designsystem.component.SheetRow
import app.kiddo.core.designsystem.component.pressable
import app.kiddo.core.model.Cv
import app.kiddo.core.model.Profile
import app.kiddo.core.model.Signature
import app.kiddo.core.model.Tone
import app.kiddo.core.model.Workspace
import app.kiddo.core.navigation.Route
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.action_edit
import app.kiddo.core.resources.action_save
import app.kiddo.core.resources.generated.KiddoGlyph
import app.kiddo.core.resources.profile_default_cv
import app.kiddo.core.resources.profile_email
import app.kiddo.core.resources.profile_headline
import app.kiddo.core.resources.profile_in_every
import app.kiddo.core.resources.profile_linkedin
import app.kiddo.core.resources.profile_name
import app.kiddo.core.resources.profile_phone
import app.kiddo.core.resources.profile_portfolio
import app.kiddo.core.resources.profile_signature
import app.kiddo.core.resources.profile_stored
import app.kiddo.core.resources.profile_title
import app.kiddo.core.resources.profile_tone
import app.kiddo.core.resources.profile_writing
import app.kiddo.core.resources.profile_writing_note
import app.kiddo.core.resources.settings_none
import app.kiddo.core.resources.signature_name
import app.kiddo.core.resources.signature_name_phone
import app.kiddo.core.resources.tone_brief
import app.kiddo.core.resources.tone_formal
import app.kiddo.core.resources.tone_friendly
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

data class ProfileState(val workspace: Workspace, val profile: Profile, val defaultCv: Cv?)

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(private val workspaces: WorkspaceStore, cvs: CvStore) : ViewModel() {
    val state: StateFlow<ProfileState?> = workspaces.workspace.filterNotNull().flatMapLatest { ws ->
        combine(workspaces.profile, cvs.cvs(ws.id)) { p, list -> ProfileState(ws, p, list.firstOrNull { it.isDefault }) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER), null)

    fun save(workspace: Workspace, profile: Profile) {
        viewModelScope.launch { workspaces.save(workspace, profile) }
    }
}

val profileModule = module { viewModelOf(::ProfileViewModel) }

