package app.kiddo.feature.onboarding.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.kiddo.core.database.WorkspaceStore
import app.kiddo.core.designsystem.component.Field
import app.kiddo.core.designsystem.component.FieldKind
import app.kiddo.core.designsystem.component.KiddoScreen
import app.kiddo.core.designsystem.component.Primary
import app.kiddo.core.designsystem.component.Title
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.workspace_create
import app.kiddo.core.resources.workspace_email
import app.kiddo.core.resources.workspace_email_bad
import app.kiddo.core.resources.workspace_name
import app.kiddo.core.resources.workspace_sub
import app.kiddo.core.resources.workspace_title
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

data class WorkspaceState(val name: String = "", val email: String = "", val saving: Boolean = false) {
    val emailLooksRight: Boolean get() = EMAIL.matches(email.trim())
    val canCreate: Boolean get() = name.isNotBlank() && emailLooksRight && !saving
}

private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

class WorkspaceViewModel(private val workspaces: WorkspaceStore) : ViewModel() {
    private val _state = MutableStateFlow(WorkspaceState())
    val state: StateFlow<WorkspaceState> = _state

    fun name(value: String) = _state.update { it.copy(name = value) }

    fun email(value: String) = _state.update { it.copy(email = value) }

    fun create(onDone: () -> Unit) {
        val now = _state.value
        if (!now.canCreate) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            workspaces.create(now.name, now.email)
            onDone()
        }
    }
}

@Composable
fun WorkspaceRoute(viewModel: WorkspaceViewModel, onCreated: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    WorkspaceScreen(state, viewModel::name, viewModel::email, { viewModel.create(onCreated) }, modifier)
}

@Composable
fun WorkspaceScreen(
    state: WorkspaceState,
    onName: (String) -> Unit,
    onEmail: (String) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KiddoScreen(
        modifier,
        footer = { Primary(stringResource(Res.string.workspace_create), onCreate, enabled = state.canCreate, busy = state.saving) },
    ) {
        Title(stringResource(Res.string.workspace_title), sub = stringResource(Res.string.workspace_sub))
        Field(state.name, onName, label = stringResource(Res.string.workspace_name))
        Field(
            state.email,
            onEmail,
            label = stringResource(Res.string.workspace_email),
            kind = FieldKind.Email,
            error = stringResource(Res.string.workspace_email_bad).takeIf { state.email.isNotBlank() && !state.emailLooksRight },
        )
    }
}
